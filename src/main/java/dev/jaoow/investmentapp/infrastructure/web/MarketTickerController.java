package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.model.MarketTicker;
import dev.jaoow.investmentapp.application.service.MarketTickerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/market/ticker")
public class MarketTickerController {

    private final MarketTickerService marketTickerService;

    public MarketTickerController(MarketTickerService marketTickerService) {
        this.marketTickerService = marketTickerService;
    }

    @GetMapping("/search")
    public List<MarketTicker> searchTickers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type) {
        return marketTickerService.searchTickers(query, type);
    }
}
