package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.CategoryAllocationRequest;
import dev.jaoow.investmentapp.application.dto.response.CategoryAllocationResponse;
import dev.jaoow.investmentapp.application.service.AllocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/allocations")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void setTargetAllocations(@PathVariable Long portfolioId,
                                     @RequestBody @Valid @NotEmpty List<@NotNull @Valid CategoryAllocationRequest> categoryAllocations) {
        allocationService.setCategoryAllocations(portfolioId, categoryAllocations);
    }

    @GetMapping
    public List<CategoryAllocationResponse> getAllocationsWithZeroValues(@PathVariable Long portfolioId) {
        return allocationService.getAllocationsWithZeroValues(portfolioId);
    }
}
