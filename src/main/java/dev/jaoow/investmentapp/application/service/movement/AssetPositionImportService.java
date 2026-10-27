package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.dto.request.AssetPositionRequest;
import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.application.exception.AssetMovementImportException;
import dev.jaoow.investmentapp.application.util.AssetPositionFileProcessor;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AssetPositionImportService {

    private final AssetPositionFileProcessor fileProcessor;
    private final PortfolioRepository portfolioRepository;
    private final AssetMovementRepository assetMovementRepository;
    private final ModelMapper modelMapper;
    private final PositionLedger ledger;

    public AssetPositionImportService(AssetPositionFileProcessor fileProcessor,
                                      PortfolioRepository portfolioRepository,
                                      AssetMovementRepository assetMovementRepository,
                                      ModelMapper modelMapper, PositionLedger ledger) {
        this.fileProcessor = fileProcessor;
        this.portfolioRepository = portfolioRepository;
        this.assetMovementRepository = assetMovementRepository;
        this.modelMapper = modelMapper;
        this.ledger = ledger;
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public List<AssetMovementResponse> importCurrentPositions(Long portfolioId, MultipartFile file) {
        try {
            List<AssetPositionRequest> positions = fileProcessor.processExcelFile(file);
            Portfolio portfolio = ledger.lock(portfolioId);
            ledger.requireMutableHistory(ledger.history(portfolioId));
            assetMovementRepository.deleteAllByPortfolio(portfolio);
            portfolio.getAssetMovements().clear();
            LocalDate importDate = LocalDate.now();
            Set<String> tickers = new HashSet<>();
            List<AssetMovement> movements = positions.stream().map(position -> {
                if (!tickers.add(position.getTickerSymbol())) {
                    throw new AssetMovementImportException("The position spreadsheet contains duplicate ticker " + position.getTickerSymbol() + ".", null);
                }
                AssetMovement movement = new AssetMovement();
                movement.setPortfolio(portfolio);
                movement.setTickerSymbol(position.getTickerSymbol());
                movement.setQuantity(position.getQuantity());
                movement.setPrice(position.getAveragePrice());
                movement.setDate(importDate);
                movement.setType(MovementType.BUY);
                return movement;
            }).toList();
            ledger.advance(portfolio);
            return assetMovementRepository.saveAll(movements).stream()
                    .map(movement -> modelMapper.map(movement, AssetMovementResponse.class))
                    .toList();
        } catch (IOException | IllegalArgumentException e) {
            throw new AssetMovementImportException("Could not read the current position spreadsheet.", e);
        }
    }
}
