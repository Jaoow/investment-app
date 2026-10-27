package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.dto.request.PositionAdjustmentRequest;
import dev.jaoow.investmentapp.application.dto.response.PositionResponse;
import dev.jaoow.investmentapp.application.exception.InvalidPortfolioPositionException;
import dev.jaoow.investmentapp.application.exception.InvalidTickerException;
import dev.jaoow.investmentapp.application.exception.PositionConflictException;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.util.TickerSymbol;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class PositionAdjustmentService {
    private final PositionLedger ledger;
    private final AssetMovementRepository movements;
    private final TickerRepository tickers;

    public PositionAdjustmentService(PositionLedger ledger, AssetMovementRepository movements, TickerRepository tickers) {
        this.ledger = ledger;
        this.movements = movements;
        this.tickers = tickers;
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public PositionResponse get(Long portfolioId, String ticker) {
        var portfolio = ledger.lock(portfolioId);
        String symbol = TickerSymbol.normalize(ticker);
        var position = position(symbol, ledger.history(portfolioId));
        return new PositionResponse(symbol, position.getTotalQuantity(), position.getInvestedAmount(),
                portfolio.getPositionRevision());
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public PositionResponse adjust(Long portfolioId, String ticker, PositionAdjustmentRequest request, Principal principal) {
        var portfolio = ledger.lock(portfolioId);
        String symbol = TickerSymbol.normalize(ticker);
        var previousRequest = movements.findByPortfolioIdAndAdjustmentRequestId(portfolioId, request.requestId());
        if (previousRequest.isPresent()) {
            AssetMovement saved = previousRequest.get();
            if (!saved.getTickerSymbol().equals(symbol)
                    || saved.getAdjustmentQuantity().compareTo(request.quantity()) != 0
                    || saved.getAdjustmentTotalCost().compareTo(request.totalCost()) != 0
                    || !saved.getReason().equals(request.reason().trim())
                    || !saved.getAdjustmentExpectedRevision().equals(request.expectedRevision())) {
                throw new PositionConflictException("Identificador de ajuste reutilizado com dados diferentes.");
            }
            return new PositionResponse(symbol, saved.getAdjustmentQuantity(), saved.getAdjustmentTotalCost(),
                    saved.getAdjustmentResultRevision());
        }
        if (portfolio.getPositionRevision() != request.expectedRevision()) {
            throw new PositionConflictException("A carteira mudou. Recarregue a posição antes de confirmar o ajuste.");
        }
        if (!tickers.existsById(symbol)) throw new InvalidTickerException("Ativo não cadastrado: " + symbol);
        if (request.quantity().signum() == 0 && request.totalCost().signum() != 0) {
            throw new InvalidPortfolioPositionException("Uma posição zerada deve ter custo total zero.");
        }
        List<AssetMovement> history = ledger.history(portfolioId);
        LocalDate effectiveDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        if (history.stream().anyMatch(m -> m.getTickerSymbol().equals(symbol) && m.getDate().isAfter(effectiveDate))) {
            throw new PositionConflictException("O ativo possui operações futuras. Corrija essas datas antes de ajustar a posição.");
        }
        var previous = position(symbol, history);
        AssetMovement adjustment = new AssetMovement();
        adjustment.setPortfolio(portfolio);
        adjustment.setTickerSymbol(symbol);
        adjustment.setType(MovementType.POSITION_ADJUSTMENT);
        adjustment.setDate(effectiveDate);
        adjustment.setAdjustmentQuantity(request.quantity());
        adjustment.setAdjustmentTotalCost(request.totalCost());
        adjustment.setPreviousQuantity(previous.getTotalQuantity());
        adjustment.setPreviousTotalCost(previous.getInvestedAmount());
        adjustment.setReason(request.reason().trim());
        adjustment.setAdjustmentAuthor(principal.getName());
        adjustment.setAdjustmentRecordedAt(Instant.now());
        adjustment.setAdjustmentRequestId(request.requestId());
        adjustment.setAdjustmentExpectedRevision(request.expectedRevision());
        ledger.validateAppend(history, List.of(adjustment));
        ledger.advance(portfolio);
        adjustment.setAdjustmentResultRevision(portfolio.getPositionRevision());
        movements.saveAndFlush(adjustment);
        return new PositionResponse(symbol, request.quantity(), request.totalCost(), portfolio.getPositionRevision());
    }

    private AssetConsolidation position(String symbol, List<AssetMovement> history) {
        return ledger.consolidate(history).stream().filter(p -> p.getTickerSymbol().equals(symbol))
                .findFirst().orElseGet(() -> new AssetConsolidation(symbol));
    }
}
