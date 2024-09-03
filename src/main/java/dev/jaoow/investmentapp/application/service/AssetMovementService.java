package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetMovementService {
    private final AssetMovementRepository assetMovementRepository;
    private final PortfolioRepository portfolioRepository;
    private final ModelMapper modelMapper;
    private final AssetMovementImportService assetMovementImportService;

    public AssetMovementService(AssetMovementRepository assetMovementRepository,
                                PortfolioRepository portfolioRepository,
                                ModelMapper modelMapper, AssetMovementImportService assetMovementImportService) {
        this.assetMovementRepository = assetMovementRepository;
        this.portfolioRepository = portfolioRepository;
        this.modelMapper = modelMapper;
        this.assetMovementImportService = assetMovementImportService;
    }

    // Get all asset movements by portfolio
    @Transactional(readOnly = true)
    public List<AssetMovementResponse> getAllAssetMovementsByPortfolio(Long portfolioId) {
        return assetMovementRepository.findAll().stream()
                .filter(movement -> movement.getPortfolio().getId().equals(portfolioId))
                .map(movement -> modelMapper.map(movement, AssetMovementResponse.class))
                .collect(Collectors.toList());
    }

    // Get a specific asset movement by ID, ensuring it belongs to the specified portfolio
    @Transactional(readOnly = true)
    public AssetMovementResponse getAssetMovement(Long portfolioId, Long movementId) {
        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new RuntimeException("Asset Movement not found"));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new RuntimeException("Asset Movement does not belong to the specified portfolio");
        }

        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }

    // Create a new asset movement in the specified portfolio
    @Transactional
    public AssetMovementResponse createAssetMovement(Long portfolioId, AssetMovementRequest assetMovementRequest) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        AssetMovement assetMovement = modelMapper.map(assetMovementRequest, AssetMovement.class);
        assetMovement.setPortfolio(portfolio); // Set the portfolio to ensure the relationship

        // Add the movement to the portfolio's list
        portfolio.getAssetMovements().add(assetMovement);

        assetMovement = assetMovementRepository.save(assetMovement);
        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }

    // Update an existing asset movement in the specified portfolio
    @Transactional
    public AssetMovementResponse updateAssetMovement(Long portfolioId, Long movementId, AssetMovementRequest assetMovementRequest) {
        portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new RuntimeException("Asset Movement not found"));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new RuntimeException("Asset Movement does not belong to the specified portfolio");
        }

        assetMovement.setTickerSymbol(assetMovementRequest.getTickerSymbol());
        assetMovement.setQuantity(assetMovementRequest.getQuantity());
        assetMovement.setPrice(assetMovementRequest.getPrice());
        assetMovement.setType(assetMovementRequest.getType());
        assetMovement.setDate(assetMovementRequest.getDate());

        assetMovement = assetMovementRepository.save(assetMovement);
        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }

    // Delete an asset movement by ID, ensuring it belongs to the specified portfolio
    @Transactional
    public void deleteAssetMovement(Long portfolioId, Long movementId) {
        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new RuntimeException("Asset Movement not found"));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new RuntimeException("Asset Movement does not belong to the specified portfolio");
        }

        assetMovementRepository.delete(assetMovement);
    }

    @Transactional
    public List<AssetMovementResponse> importAssetMovements(Long portfolioId, MultipartFile file) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        try {
            List<AssetMovement> assetMovements = assetMovementImportService.processExcelFile(file);

            for (AssetMovement assetMovement : assetMovements) {
                assetMovement.setPortfolio(portfolio);
                portfolio.getAssetMovements().add(assetMovement);
            }

            assetMovementRepository.saveAll(assetMovements);

            return assetMovements.stream()
                    .map(movement -> modelMapper.map(movement, AssetMovementResponse.class))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Error importing asset movements", e);
        }
    }
}
