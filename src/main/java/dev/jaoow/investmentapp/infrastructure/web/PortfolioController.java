package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.PortfolioRequest;
import dev.jaoow.investmentapp.application.dto.response.PortfolioResponse;
import dev.jaoow.investmentapp.application.dto.response.summary.PortfolioSummaryResponse;
import dev.jaoow.investmentapp.application.service.PortfolioService;
import dev.jaoow.investmentapp.application.service.SummaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolio")
public class PortfolioController {
    private final PortfolioService portfolioService;
    private final SummaryService summaryService;

    public PortfolioController(PortfolioService portfolioService, SummaryService summaryService) {
        this.portfolioService = portfolioService;
        this.summaryService = summaryService;
    }

    @GetMapping
    public List<PortfolioResponse> getAllPortfolios() {
        return portfolioService.getAllPortfolios();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioResponse createPortfolio(@Valid @RequestBody PortfolioRequest portfolioRequest) {
        return portfolioService.createPortfolio(portfolioRequest);
    }

    @GetMapping("/{portfolioId}")
    public PortfolioResponse getPortfolio(@PathVariable Long portfolioId) {
        return portfolioService.getPortfolio(portfolioId);
    }

    @PutMapping("/{portfolioId}")
    public PortfolioResponse updatePortfolio(@PathVariable Long portfolioId, @Valid @RequestBody PortfolioRequest portfolioRequest) {
        return portfolioService.updatePortfolio(portfolioId, portfolioRequest);
    }

    @DeleteMapping("/{portfolioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePortfolio(@PathVariable Long portfolioId) {
        portfolioService.deletePortfolio(portfolioId);
    }

    @GetMapping("/{portfolioId}/summary")
    public PortfolioSummaryResponse getPortfolioSummary(@PathVariable Long portfolioId,
                                                        @RequestParam(required = false) String category) {
        return summaryService.getPortfolioSummary(portfolioId, category);
    }
}
