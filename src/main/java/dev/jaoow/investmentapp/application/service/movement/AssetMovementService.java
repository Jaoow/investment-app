package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.application.exception.AssetMovementNotFoundException;
import dev.jaoow.investmentapp.application.exception.PortfolioMismatchException;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
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
    public Page<AssetMovementResponse> getAllByPortfolio(Long portfolioId, Principal principal, Pageable pageable) {

        String userEmail = principal.getName();
        Portfolio portfolio = portfolioRepository.findByIdAndUserEmail(portfolioId, userEmail)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        return assetMovementRepository.findAllByPortfolio(portfolio, pageable)
                .map(movement -> modelMapper.map(movement, AssetMovementResponse.class));
    }

    @Transactional
    public List<AssetMovementResponse> createAssetMovementsBulk(Long portfolioId, List<AssetMovementRequest> assetMovementRequests) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        List<AssetMovement> assetMovements = assetMovementRequests.stream()
                .map(request -> {
                    AssetMovement assetMovement = modelMapper.map(request, AssetMovement.class);
                    assetMovement.setPortfolio(portfolio);
                    return assetMovement;
                })
                .toList();

        assetMovements = assetMovementRepository.saveAll(assetMovements);

        return assetMovements.stream()
                .map(movement -> modelMapper.map(movement, AssetMovementResponse.class))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and portfolioSecurity.isOwner(portfolioId, authentication)")
    public AssetMovementResponse createAssetMovement(Long portfolioId, AssetMovementRequest assetMovementRequest) {
        List<AssetMovementRequest> requests = List.of(assetMovementRequest);
        return createAssetMovementsBulk(portfolioId, requests).getFirst();
    }

    @PreAuthorize("hasRole('USER') and @assetMovementSecurity.isOwner(#movementId, authentication)")
    @Transactional(readOnly = true)
    public AssetMovementResponse getAssetMovement(Long portfolioId, Long movementId) {
        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new AssetMovementNotFoundException(movementId));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new PortfolioMismatchException(portfolioId, movementId);
        }

        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }


    @PreAuthorize("hasRole('USER') and @assetMovementSecurity.isOwner(#movementId, authentication)")
    @Transactional
    public AssetMovementResponse updateAssetMovement(Long portfolioId, Long movementId, AssetMovementRequest assetMovementRequest) {
        portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new AssetMovementNotFoundException(movementId));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new PortfolioMismatchException(portfolioId, movementId);
        }

        modelMapper.map(assetMovementRequest, assetMovement);
        assetMovement = assetMovementRepository.save(assetMovement);

        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }

    @PreAuthorize("hasRole('USER') and @assetMovementSecurity.isOwner(#movementId, authentication)")
    @Transactional
    public void deleteAssetMovement(Long portfolioId, Long movementId) {
        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new AssetMovementNotFoundException(movementId));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new PortfolioMismatchException(portfolioId, movementId);
        }

        assetMovementRepository.delete(assetMovement);
    }
}
