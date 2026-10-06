package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.TickerFilterRequest;
import dev.jaoow.investmentapp.application.dto.request.TickerRequest;
import dev.jaoow.investmentapp.application.dto.request.RegisterTickerRequest;
import dev.jaoow.investmentapp.application.dto.response.SectorResponse;
import dev.jaoow.investmentapp.application.dto.response.TickerResponse;
import dev.jaoow.investmentapp.application.exception.TickerNotFoundException;
import dev.jaoow.investmentapp.application.exception.InvalidTickerException;
import dev.jaoow.investmentapp.application.util.TickerSymbol;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TickerService {

    private final TickerRepository tickerRepository;
    private final ModelMapper modelMapper;
    private final MarketTickerProvider marketTickerProvider;

    public TickerService(TickerRepository tickerRepository, ModelMapper modelMapper, MarketTickerProvider marketTickerProvider) {
        this.tickerRepository = tickerRepository;
        this.modelMapper = modelMapper;
        this.marketTickerProvider = marketTickerProvider;
    }

    @Transactional(readOnly = true)
    public TickerResponse getTicker(String symbol) {
        Ticker ticker = tickerRepository.findById(symbol)
                .orElseThrow(() -> new TickerNotFoundException(symbol));
        return modelMapper.map(ticker, TickerResponse.class);
    }

    @Transactional(readOnly = true)
    public PagedModel<TickerResponse> getAllTickers(Pageable pageable) {
        return new PagedModel<>(tickerRepository.findAll(pageable)
                .map(ticker -> modelMapper.map(ticker, TickerResponse.class)));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TickerResponse createTicker(TickerRequest tickerRequest) {
        Ticker ticker = modelMapper.map(tickerRequest, Ticker.class);
        ticker = tickerRepository.save(ticker);
        return modelMapper.map(ticker, TickerResponse.class);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public TickerResponse registerTicker(RegisterTickerRequest request) {
        String symbol = TickerSymbol.normalize(request.getSymbol());
        Ticker existing = tickerRepository.findById(symbol).orElse(null);
        if (existing != null) {
            if (existing.getCategory() != request.getCategory()) {
                throw new InvalidTickerException(
                        symbol + " já está cadastrado na categoria " + existing.getCategory() + ".");
            }
            return modelMapper.map(existing, TickerResponse.class);
        }
        Ticker ticker = new Ticker();
        ticker.setSymbol(symbol);
        ticker.setCategory(request.getCategory());

        try {
            List<dev.jaoow.investmentapp.application.model.MarketTicker> details = marketTickerProvider.searchTickers(symbol, null);
            if (details != null && !details.isEmpty()) {
                dev.jaoow.investmentapp.application.model.MarketTicker detail = details.stream()
                        .filter(t -> t.getSymbol().equalsIgnoreCase(symbol))
                        .findFirst()
                        .orElse(details.get(0));
                ticker.setName(detail.getName() != null ? detail.getName() : detail.getLongName());
                ticker.setLogoUrl(detail.getLogoUrl());
                ticker.setSector(detail.getSector());
                ticker.setSubSector(detail.getSubSector());
            }
        } catch (Exception ignored) {
            // Ignora falha silenciosamente caso não consiga buscar no provedor
        }

        return modelMapper.map(tickerRepository.save(ticker), TickerResponse.class);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TickerResponse updateTicker(String symbol, TickerRequest tickerRequest) {
        tickerRepository.findById(symbol)
                .orElseThrow(() -> new TickerNotFoundException(symbol));

        Ticker ticker = modelMapper.map(tickerRequest, Ticker.class);
        ticker = tickerRepository.save(ticker);
        return modelMapper.map(ticker, TickerResponse.class);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTicker(String symbol) {
        tickerRepository.deleteBySymbol(symbol);
    }

    @Transactional(readOnly = true)
    public List<SectorResponse> getAllSectorsWithSubSectors() {
        List<String> sectors = tickerRepository.findAllSectors();
        return sectors.stream()
                .map(sector -> {
                    List<String> subSectors = tickerRepository.findAllSubSectorsBySector(sector);
                    return new SectorResponse(sector, subSectors);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedModel<TickerResponse> searchTickers(TickerFilterRequest filterRequest, Pageable pageable) {
        Page<Ticker> search = tickerRepository.search(
                filterRequest.getSymbol(),
                filterRequest.getCategory(),
                filterRequest.getSector(),
                filterRequest.getSubSector(),
                pageable
        );
        return new PagedModel<>(search.map(ticker -> modelMapper.map(ticker, TickerResponse.class)));
    }
}
