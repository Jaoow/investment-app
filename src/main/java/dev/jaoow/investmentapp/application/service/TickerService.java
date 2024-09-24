package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.TickerFilterRequest;
import dev.jaoow.investmentapp.application.dto.request.TickerRequest;
import dev.jaoow.investmentapp.application.dto.response.SectorResponse;
import dev.jaoow.investmentapp.application.dto.response.TickerResponse;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
        Ticker ticker = tickerRepository.findById(symbol).orElseThrow(() -> new RuntimeException("Ticker not found"));
        return modelMapper.map(ticker, TickerResponse.class);
    }

    @Transactional(readOnly = true)
    public List<TickerResponse> getAllTickers() {
        return tickerRepository.findAll().stream()
                .map(ticker -> modelMapper.map(ticker, TickerResponse.class))
                .toList();
    }

    @Transactional
    public TickerResponse createTicker(TickerRequest tickerRequest) {
        Ticker ticker = modelMapper.map(tickerRequest, Ticker.class);
        ticker = tickerRepository.save(ticker);
        return modelMapper.map(ticker, TickerResponse.class);
    }

    @Transactional
    public TickerResponse updateTicker(String symbol, TickerRequest tickerRequest) {
        Ticker ticker = tickerRepository.findById(symbol)
                .orElseThrow(() -> new RuntimeException("Ticker not found"));

        ticker.setCategory(tickerRequest.getCategory());
        ticker.setSector(tickerRequest.getSector());
        ticker.setSubSector(tickerRequest.getSubSector());
        ticker.setPriceCeiling(tickerRequest.getPriceCeiling());

        ticker = tickerRepository.save(ticker);
        return modelMapper.map(ticker, TickerResponse.class);
    }

    @Transactional
    public void deleteTicker(String symbol) {
        Ticker ticker = tickerRepository.findById(symbol).orElseThrow(() -> new RuntimeException("Ticker not found"));
        tickerRepository.delete(ticker);
    }

    @Transactional(readOnly = true)
    public List<SectorResponse> getAllSectorsWithSubSectors() {
        List<String> sectors = tickerRepository.findAllSectors();
        return sectors.stream()
                .map(sector -> {
                    List<String> subSectors = tickerRepository.findAllSubSectorsBySector(sector);
                    return new SectorResponse(sector, subSectors);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TickerResponse> searchTickers(TickerFilterRequest filterRequest, Pageable pageable) {
        Page<Ticker> tickersPage = tickerRepository.search(
                filterRequest.getSymbol(),
                filterRequest.getCategory(),
                filterRequest.getSector(),
                filterRequest.getSubSector(),
                pageable
        );

        return new PageImpl<>(
                tickersPage.stream()
                        .map(ticker -> modelMapper.map(ticker, TickerResponse.class))
                        .collect(Collectors.toList()),
                pageable,
                tickersPage.getTotalElements()
        );
    }
}
