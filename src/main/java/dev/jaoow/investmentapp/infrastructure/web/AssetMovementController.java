package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.application.service.movement.AssetMovementImportService;
import dev.jaoow.investmentapp.application.service.movement.AssetMovementService;
import dev.jaoow.investmentapp.application.service.movement.AssetMovementTemplateService;
import dev.jaoow.investmentapp.application.service.movement.AssetPositionImportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/movement")
public class AssetMovementController {

    private final AssetMovementService assetMovementService;
    private final AssetMovementImportService assetMovementImportService;
    private final AssetPositionImportService assetPositionImportService;
    private final AssetMovementTemplateService templateService;

    public AssetMovementController(AssetMovementService assetMovementService,
                                   AssetMovementImportService assetMovementImportService,
                                   AssetPositionImportService assetPositionImportService,
                                   AssetMovementTemplateService templateService) {
        this.assetMovementService = assetMovementService;
        this.assetMovementImportService = assetMovementImportService;
        this.assetPositionImportService = assetPositionImportService;
        this.templateService = templateService;
    }

    @GetMapping
    public PagedModel<AssetMovementResponse> getAllAssetMovements(@PathVariable Long portfolioId, Principal principal, Pageable pageable) {
        return assetMovementService.getAllByPortfolio(portfolioId, principal, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetMovementResponse createAssetMovement(@PathVariable Long portfolioId, @Valid @RequestBody AssetMovementRequest assetMovementRequest) {
        return assetMovementService.createAssetMovement(portfolioId, assetMovementRequest);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<AssetMovementResponse> importAssetMovements(@PathVariable Long portfolioId, @RequestParam("file") MultipartFile file) {
        return assetMovementImportService.importAssetMovements(portfolioId, file);
    }

    @PostMapping(value = "/position-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<AssetMovementResponse> importCurrentPositions(@PathVariable Long portfolioId,
                                                               @RequestParam("file") MultipartFile file) {
        return assetPositionImportService.importCurrentPositions(portfolioId, file);
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long portfolioId,
                                                         @RequestParam(defaultValue = "movements") String type) {
        byte[] content = "positions".equalsIgnoreCase(type)
                ? templateService.createPositionsTemplate()
                : templateService.createMovementsTemplate();
        String filename = "positions".equalsIgnoreCase(type)
                ? "modelo-posicao-atual.xlsx"
                : "modelo-movimentacoes.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
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
