package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssetMovementService {

    private final AssetMovementRepository assetMovementRepository;
    private final PortfolioRepository portfolioRepository;
    private final ModelMapper modelMapper;

    public AssetMovementService(AssetMovementRepository assetMovementRepository,
                                PortfolioRepository portfolioRepository,
                                ModelMapper modelMapper) {
        this.assetMovementRepository = assetMovementRepository;
        this.portfolioRepository = portfolioRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<AssetMovementResponse> getAllAssetMovementsByPortfolio(Long portfolioId) {
        return assetMovementRepository.findAll().stream()
                .filter(movement -> movement.getPortfolio().getId().equals(portfolioId))
                .map(movement -> modelMapper.map(movement, AssetMovementResponse.class))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetMovementResponse getAssetMovement(Long portfolioId, Long movementId) {
        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new RuntimeException("Asset Movement not found"));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new RuntimeException("Asset Movement does not belong to the specified portfolio");
        }

        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }

    @Transactional
    public AssetMovementResponse createAssetMovement(Long portfolioId, AssetMovementRequest assetMovementRequest) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        AssetMovement assetMovement = modelMapper.map(assetMovementRequest, AssetMovement.class);
        assetMovement.setPortfolio(portfolio);

        // Add the movement to the portfolio's list
        portfolio.getAssetMovements().add(assetMovement);

        assetMovement = assetMovementRepository.save(assetMovement);
        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }

    @Transactional
    public AssetMovementResponse updateAssetMovement(Long portfolioId, Long movementId, AssetMovementRequest assetMovementRequest) {
        portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new RuntimeException("Asset Movement not found"));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new RuntimeException("Asset Movement does not belong to the specified portfolio");
        }

        // Update the asset movement with the new values
        modelMapper.map(assetMovementRequest, assetMovement);
        assetMovement = assetMovementRepository.save(assetMovement);

        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }

    @Transactional
    public void deleteAssetMovement(Long portfolioId, Long movementId) {
        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new RuntimeException("Asset Movement not found"));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new RuntimeException("Asset Movement does not belong to the specified portfolio");
        }

        assetMovementRepository.delete(assetMovement);
    }
}
