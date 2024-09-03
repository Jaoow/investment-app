package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.CategoryAllocationRequest;
import dev.jaoow.investmentapp.application.dto.response.CategoryAllocationResponse;
import dev.jaoow.investmentapp.application.dto.response.RebalanceRecommendationResponse;
import dev.jaoow.investmentapp.application.service.RebalanceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolio")
public class RebalanceController {
    private final RebalanceService rebalanceService;

    public RebalanceController(RebalanceService rebalanceService) {
        this.rebalanceService = rebalanceService;
    }

    @PostMapping("/{portfolioId}/allocations")
    @ResponseStatus(HttpStatus.CREATED)
    public void setTargetAllocations(@PathVariable Long portfolioId,
                                     @RequestBody List<CategoryAllocationRequest> categoryAllocations) {
        rebalanceService.setCategoryAllocations(portfolioId, categoryAllocations);
    }

    @GetMapping("/{portfolioId}/rebalance")
    public List<RebalanceRecommendationResponse> getRebalanceRecommendations(@PathVariable Long portfolioId,
                                                                             @RequestParam(required = false) String category) {
        return rebalanceService.generateRebalancingRecommendations(portfolioId, category);
    }

    @GetMapping("/{portfolioId}/allocations")
    public List<CategoryAllocationResponse> getAllocationsWithZeroValues(@PathVariable Long portfolioId) {
        return rebalanceService.getAllocationsWithZeroValues(portfolioId);
    }
}
