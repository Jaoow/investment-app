package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.application.exception.AssetMovementImportException;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.util.AssetMovementFileProcessor;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class AssetMovementImportService {

    private final AssetMovementFileProcessor assetMovementFileProcessor;
    private final PortfolioRepository portfolioRepository;
    private final AssetMovementService assetMovementService;

    public AssetMovementImportService(AssetMovementFileProcessor assetMovementFileProcessor,
                                      PortfolioRepository portfolioRepository,
                                      AssetMovementService assetMovementService) {
        this.assetMovementFileProcessor = assetMovementFileProcessor;
        this.portfolioRepository = portfolioRepository;
        this.assetMovementService = assetMovementService;
    }

    @Transactional
    public List<AssetMovementResponse> importAssetMovements(Long portfolioId, MultipartFile file) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        try {
            List<AssetMovementRequest> assetMovements = assetMovementFileProcessor.processExcelFile(file);
            return assetMovementService.createAssetMovementsBulk(portfolio.getId(), assetMovements);
        } catch (Exception e) {
            throw new AssetMovementImportException();
        }
    }
}
