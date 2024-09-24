package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.application.util.AssetMovementFileProcessor;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class AssetMovementImportService {

    private final AssetMovementFileProcessor assetMovementFileProcessor;
    private final PortfolioRepository portfolioRepository;
    private final AssetMovementRepository assetMovementRepository;
    private final ModelMapper modelMapper;

    public AssetMovementImportService(AssetMovementFileProcessor assetMovementFileProcessor,
                                      PortfolioRepository portfolioRepository,
                                      AssetMovementRepository assetMovementRepository,
                                      ModelMapper modelMapper) {
        this.assetMovementFileProcessor = assetMovementFileProcessor;
        this.portfolioRepository = portfolioRepository;
        this.assetMovementRepository = assetMovementRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public List<AssetMovementResponse> importAssetMovements(Long portfolioId, MultipartFile file) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        try {
            List<AssetMovement> assetMovements = assetMovementFileProcessor.processExcelFile(file);

            for (AssetMovement assetMovement : assetMovements) {
                assetMovement.setPortfolio(portfolio);
                portfolio.getAssetMovements().add(assetMovement);
            }

            assetMovementRepository.saveAll(assetMovements);

            return assetMovements.stream()
                    .map(movement -> modelMapper.map(movement, AssetMovementResponse.class))
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Error importing asset movements", e);
        }
    }
}
