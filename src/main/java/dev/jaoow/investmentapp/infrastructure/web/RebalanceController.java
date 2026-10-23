package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.RebalanceSaleRequest;
import dev.jaoow.investmentapp.application.dto.response.RebalanceRecommendationResponse;
import dev.jaoow.investmentapp.application.dto.response.RebalanceSalePlanResponse;
import dev.jaoow.investmentapp.application.service.RebalanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/rebalance")
public class RebalanceController {

    private final RebalanceService rebalanceService;

    public RebalanceController(RebalanceService rebalanceService) {
        this.rebalanceService = rebalanceService;
    }

    @GetMapping
    public List<RebalanceRecommendationResponse> getRebalanceRecommendations(@PathVariable Long portfolioId,
                                                                             @RequestParam(required = false) String category) {
        return rebalanceService.generateRebalanceRecommendations(portfolioId, category);
    }

    @PostMapping("/sales")
    public RebalanceSalePlanResponse planSales(@PathVariable Long portfolioId,
                                               @Valid @RequestBody RebalanceSaleRequest request) {
        return rebalanceService.planSales(portfolioId, request);
    }
}
