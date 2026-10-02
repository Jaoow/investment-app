package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.response.SharedPortfolioSnapshotResponse;
import dev.jaoow.investmentapp.application.service.PortfolioShareService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/portfolio-shares")
public class PublicPortfolioShareController {

    private final PortfolioShareService shareService;

    public PublicPortfolioShareController(PortfolioShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping("/{token}")
    public SharedPortfolioSnapshotResponse readSnapshot(@PathVariable String token) {
        return shareService.readPublicSnapshot(token);
    }
}
