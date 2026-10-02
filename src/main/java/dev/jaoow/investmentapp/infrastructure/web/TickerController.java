package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.TickerFilterRequest;
import dev.jaoow.investmentapp.application.dto.request.TickerRequest;
import dev.jaoow.investmentapp.application.dto.request.RegisterTickerRequest;
import dev.jaoow.investmentapp.application.dto.response.SectorResponse;
import dev.jaoow.investmentapp.application.dto.response.TickerResponse;
import dev.jaoow.investmentapp.application.service.TickerService;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/ticker")
public class TickerController {
    private final TickerService tickerService;

    public TickerController(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    @GetMapping
    public PagedModel<TickerResponse> getAllTickers(Pageable pageable) {
        return tickerService.getAllTickers(pageable);
    }

    @GetMapping("/{symbol}")
    public TickerResponse getTicker(@PathVariable String symbol) {
        return tickerService.getTicker(symbol);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TickerResponse createTicker(@Valid @RequestBody TickerRequest tickerRequest) {
        return tickerService.createTicker(tickerRequest);
    }

    @PostMapping("/register")
    public TickerResponse registerTicker(@Valid @RequestBody RegisterTickerRequest request) {
        return tickerService.registerTicker(request);
    }

    @PutMapping("/{symbol}")
    public TickerResponse updateTicker(@PathVariable String symbol, @Valid @RequestBody TickerRequest tickerRequest) {
        return tickerService.updateTicker(symbol, tickerRequest);
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTicker(@PathVariable String symbol) {
        tickerService.deleteTicker(symbol);
    }

    @GetMapping("/search")
    public PagedModel<TickerResponse> searchTickers(@RequestParam(required = false) String symbol,
                                                    @RequestParam(required = false) String sector,
                                                    @RequestParam(required = false) String subSector,
                                                    @RequestParam(required = false) AssetCategory category,
                                                    Pageable pageable) {
        TickerFilterRequest filterRequest = new TickerFilterRequest();
        filterRequest.setSector(sector);
        filterRequest.setSubSector(subSector);
        filterRequest.setCategory(category);
        filterRequest.setSymbol(symbol);

        return tickerService.searchTickers(filterRequest, pageable);
    }

    @GetMapping("/sectors")
    public List<SectorResponse> getSectorsWithSubSectors() {
        return tickerService.getAllSectorsWithSubSectors();
    }
}
