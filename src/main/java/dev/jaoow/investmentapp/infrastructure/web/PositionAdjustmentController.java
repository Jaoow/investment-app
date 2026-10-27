package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.PositionAdjustmentRequest;
import dev.jaoow.investmentapp.application.dto.response.PositionResponse;
import dev.jaoow.investmentapp.application.service.movement.PositionAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/positions/{ticker}")
public class PositionAdjustmentController {
    private final PositionAdjustmentService service;

    public PositionAdjustmentController(PositionAdjustmentService service) {
        this.service = service;
    }

    @GetMapping
    public PositionResponse get(@PathVariable Long portfolioId, @PathVariable String ticker) {
        return service.get(portfolioId, ticker);
    }

    @PutMapping
    public PositionResponse adjust(@PathVariable Long portfolioId, @PathVariable String ticker,
                                   @Valid @RequestBody PositionAdjustmentRequest request, Principal principal) {
        return service.adjust(portfolioId, ticker, request, principal);
    }
}
