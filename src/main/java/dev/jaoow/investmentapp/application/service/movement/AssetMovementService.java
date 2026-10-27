package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.application.dto.response.AssetMovementResponse;
import dev.jaoow.investmentapp.application.exception.AssetMovementNotFoundException;
import dev.jaoow.investmentapp.application.exception.InvalidPortfolioPositionException;
import dev.jaoow.investmentapp.application.exception.InvalidTickerException;
import dev.jaoow.investmentapp.application.exception.PortfolioMismatchException;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.util.TickerSymbol;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
public class AssetMovementService {

    private final AssetMovementRepository assetMovementRepository;
    private final PortfolioRepository portfolioRepository;
    private final TickerRepository tickerRepository;
    private final ModelMapper modelMapper;
    private final PositionLedger ledger;

    public AssetMovementService(AssetMovementRepository assetMovementRepository,
                                PortfolioRepository portfolioRepository,
                                TickerRepository tickerRepository,
                                ModelMapper modelMapper, PositionLedger ledger) {
        this.assetMovementRepository = assetMovementRepository;
        this.portfolioRepository = portfolioRepository;
        this.tickerRepository = tickerRepository;
        this.modelMapper = modelMapper;
        this.ledger = ledger;
    }

    @Transactional(readOnly = true)
    public PagedModel<AssetMovementResponse> getAllByPortfolio(Long portfolioId, Principal principal, Pageable pageable) {

        String userEmail = principal.getName();
        Portfolio portfolio = portfolioRepository.findByIdAndUserEmail(portfolioId, userEmail)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        return new PagedModel<>(assetMovementRepository.findAllByPortfolio(portfolio, pageable)
                .map(movement -> modelMapper.map(movement, AssetMovementResponse.class)));
    }

    @Transactional
    public List<AssetMovementResponse> createAssetMovementsBulk(Long portfolioId, List<AssetMovementRequest> assetMovementRequests) {
        Portfolio portfolio = ledger.lock(portfolioId);

        List<AssetMovement> assetMovements = assetMovementRequests.stream()
                .map(request -> {
                    if (request.getType() != MovementType.BUY && request.getType() != MovementType.SELL) {
                        throw new InvalidPortfolioPositionException("Use o endpoint de ajuste para corrigir a posição.");
                    }
                    AssetMovement assetMovement = modelMapper.map(request, AssetMovement.class);
                    assetMovement.setTickerSymbol(TickerSymbol.normalize(request.getTickerSymbol()));
                    assetMovement.setPortfolio(portfolio);
                    return assetMovement;
                })
                .toList();

        ledger.validateAppend(ledger.history(portfolioId), assetMovements);
        assetMovements = assetMovementRepository.saveAll(assetMovements);
        ledger.advance(portfolio);

        return assetMovements.stream()
                .map(movement -> modelMapper.map(movement, AssetMovementResponse.class))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public AssetMovementResponse createAssetMovement(Long portfolioId, AssetMovementRequest assetMovementRequest) {
        String symbol = TickerSymbol.normalize(assetMovementRequest.getTickerSymbol());
        if (!tickerRepository.existsById(symbol)) {
            throw new InvalidTickerException("O ativo " + symbol + " não está cadastrado. Selecione-o pela busca de ativos.");
        }
        assetMovementRequest.setTickerSymbol(symbol);
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
        Portfolio portfolio = ledger.lock(portfolioId);
        ledger.requireMutableHistory(ledger.history(portfolioId));
        if (assetMovementRequest.getType() != MovementType.BUY && assetMovementRequest.getType() != MovementType.SELL) {
            throw new InvalidPortfolioPositionException("Use o endpoint de ajuste para corrigir a posição.");
        }

        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new AssetMovementNotFoundException(movementId));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new PortfolioMismatchException(portfolioId, movementId);
        }

        modelMapper.map(assetMovementRequest, assetMovement);
        assetMovement.setTickerSymbol(TickerSymbol.normalize(assetMovementRequest.getTickerSymbol()));
        ledger.consolidate(ledger.history(portfolioId));
        assetMovement = assetMovementRepository.save(assetMovement);
        ledger.advance(portfolio);

        return modelMapper.map(assetMovement, AssetMovementResponse.class);
    }

    @PreAuthorize("hasRole('USER') and @assetMovementSecurity.isOwner(#movementId, authentication)")
    @Transactional
    public void deleteAssetMovement(Long portfolioId, Long movementId) {
        Portfolio portfolio = ledger.lock(portfolioId);
        List<AssetMovement> history = ledger.history(portfolioId);
        ledger.requireMutableHistory(history);
        AssetMovement assetMovement = assetMovementRepository.findById(movementId)
                .orElseThrow(() -> new AssetMovementNotFoundException(movementId));

        if (!assetMovement.getPortfolio().getId().equals(portfolioId)) {
            throw new PortfolioMismatchException(portfolioId, movementId);
        }

        ledger.consolidate(history.stream().filter(m -> !m.getId().equals(movementId)).toList());
        assetMovementRepository.delete(assetMovement);
        ledger.advance(portfolio);
    }
}
