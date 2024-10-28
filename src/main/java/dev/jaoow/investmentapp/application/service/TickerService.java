package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.TickerFilterRequest;
import dev.jaoow.investmentapp.application.dto.request.TickerRequest;
import dev.jaoow.investmentapp.application.dto.response.SectorResponse;
import dev.jaoow.investmentapp.application.dto.response.TickerResponse;
import dev.jaoow.investmentapp.application.exception.TickerNotFoundException;
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

    public TickerService(TickerRepository tickerRepository, ModelMapper modelMapper) {
        this.tickerRepository = tickerRepository;
        this.modelMapper = modelMapper;
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
