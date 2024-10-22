package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.PortfolioRequest;
import dev.jaoow.investmentapp.application.dto.response.PortfolioResponse;
import dev.jaoow.investmentapp.application.dto.response.summary.PortfolioSummaryResponse;
import dev.jaoow.investmentapp.application.service.PortfolioService;
import dev.jaoow.investmentapp.application.service.summary.PortfolioSummaryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioSummaryService portfolioSummaryService;

    public PortfolioController(PortfolioService portfolioService, PortfolioSummaryService portfolioSummaryService) {
        this.portfolioService = portfolioService;
        this.portfolioSummaryService = portfolioSummaryService;
    }

    @GetMapping
    public PagedModel<PortfolioResponse> getAllPortfolios(Pageable pageable, Principal principal) {
        return portfolioService.getAllPortfoliosByUser(pageable, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioResponse createPortfolio(@Valid @RequestBody PortfolioRequest portfolioRequest, Principal principal) {
        return portfolioService.createPortfolio(portfolioRequest, principal);
    }

    @GetMapping("/{portfolioId}")
    public PortfolioResponse getPortfolio(@PathVariable Long portfolioId, Principal principal) {
        return portfolioService.getPortfolioByIdAndUserId(portfolioId, principal);
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
        return portfolioSummaryService.getPortfolioSummary(portfolioId, category);
    }
}
