package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.TickerRequest;
import dev.jaoow.investmentapp.application.dto.response.TickerResponse;
import dev.jaoow.investmentapp.application.service.TickerService;
import jakarta.validation.Valid;
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
    public List<TickerResponse> getAllTickers() {
        return tickerService.getAllTickers();
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

    @PutMapping("/{symbol}")
    public TickerResponse updateTicker(@PathVariable String symbol, @Valid @RequestBody TickerRequest tickerRequest) {
        return tickerService.updateTicker(symbol, tickerRequest);
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTicker(@PathVariable String symbol) {
        tickerService.deleteTicker(symbol);
    }
}
