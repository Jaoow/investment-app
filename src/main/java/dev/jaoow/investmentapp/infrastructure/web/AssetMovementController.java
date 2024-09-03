package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.application.service.AssetMovementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/movement")
public class AssetMovementController {
    private final AssetMovementService assetMovementService;

    public AssetMovementController(AssetMovementService assetMovementService) {
        this.assetMovementService = assetMovementService;
    }

    @GetMapping
    public List<AssetMovementResponse> getAllAssetMovements(@PathVariable Long portfolioId) {
        return assetMovementService.getAllAssetMovementsByPortfolio(portfolioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetMovementResponse createAssetMovement(@PathVariable Long portfolioId, @Valid @RequestBody AssetMovementRequest assetMovementRequest) {
        return assetMovementService.createAssetMovement(portfolioId, assetMovementRequest);
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AssetMovementResponse> importAssetMovements(@PathVariable Long portfolioId, @RequestParam("file") MultipartFile file) {
        return assetMovementService.importAssetMovements(portfolioId, file);
    }

    @GetMapping("/{movementId}")
    public AssetMovementResponse getAssetMovement(@PathVariable Long portfolioId, @PathVariable Long movementId) {
        return assetMovementService.getAssetMovement(portfolioId, movementId);
    }

    @PutMapping("/{movementId}")
    public AssetMovementResponse updateAssetMovement(@PathVariable Long portfolioId, @PathVariable Long movementId, @Valid @RequestBody AssetMovementRequest assetMovementRequest) {
        return assetMovementService.updateAssetMovement(portfolioId, movementId, assetMovementRequest);
    }

    @DeleteMapping("/{movementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssetMovement(@PathVariable Long portfolioId, @PathVariable Long movementId) {
        assetMovementService.deleteAssetMovement(portfolioId, movementId);
    }
}
