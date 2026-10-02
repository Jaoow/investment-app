package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.CreatePortfolioShareRequest;
import dev.jaoow.investmentapp.application.dto.response.PortfolioShareCreatedResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioShareResponse;
import dev.jaoow.investmentapp.application.service.PortfolioShareService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/shares")
public class PortfolioShareController {

    private final PortfolioShareService shareService;

    public PortfolioShareController(PortfolioShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioShareCreatedResponse create(
            @PathVariable Long portfolioId,
            @Valid @RequestBody CreatePortfolioShareRequest request) {
        return shareService.create(portfolioId, request);
    }

    @GetMapping
    public List<PortfolioShareResponse> list(@PathVariable Long portfolioId) {
        return shareService.list(portfolioId);
    }

    @DeleteMapping("/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long portfolioId, @PathVariable Long shareId) {
        shareService.revoke(portfolioId, shareId);
    }
}
