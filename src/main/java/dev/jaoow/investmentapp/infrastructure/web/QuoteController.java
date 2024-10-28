package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import dev.jaoow.investmentapp.application.service.QuoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public BrapiQuoteDto getQuote(
            @RequestParam String ticker,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String interval,
            @RequestParam(required = false) Boolean fundamental,
            @RequestParam(required = false) Boolean dividends) {
        return quoteService.getQuote(ticker, range, interval, fundamental, dividends);
    }
}
