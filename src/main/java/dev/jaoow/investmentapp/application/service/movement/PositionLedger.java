package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.exception.PositionConflictException;
import dev.jaoow.investmentapp.application.exception.InvalidPortfolioPositionException;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PositionLedger {
    private final PortfolioRepository portfolios;
    private final AssetMovementRepository movements;
    private final AssetConsolidationService consolidation;
    private final EntityManager entityManager;

    public PositionLedger(PortfolioRepository portfolios, AssetMovementRepository movements,
                          AssetConsolidationService consolidation, EntityManager entityManager) {
        this.portfolios = portfolios;
        this.movements = movements;
        this.consolidation = consolidation;
        this.entityManager = entityManager;
    }

    // Every financial writer holds this database lock until its transaction commits.
    public Portfolio lock(Long portfolioId) {
        Portfolio portfolio = portfolios.findByIdForPositionUpdate(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        // Authorization/import code may have loaded an older revision before waiting for this lock.
        entityManager.refresh(portfolio, LockModeType.PESSIMISTIC_WRITE);
        return portfolio;
    }

    public List<AssetMovement> history(Long portfolioId) {
        return movements.findAllByPortfolioId(portfolioId);
    }

    public List<AssetConsolidation> consolidate(List<AssetMovement> history) {
        Portfolio view = new Portfolio();
        view.setAssetMovements(history);
        return consolidation.processAssetMovements(view);
    }

    public void validateAppend(List<AssetMovement> history, List<AssetMovement> additions) {
        for (AssetMovement addition : additions) {
            if (addition.getType() != MovementType.POSITION_ADJUSTMENT
                    && (addition.getQuantity() == null || addition.getQuantity().signum() <= 0
                    || addition.getPrice() == null || addition.getPrice().signum() <= 0
                    || addition.getDate() == null
                    || (addition.getOperationCost() != null && addition.getOperationCost().signum() < 0))) {
                throw new InvalidPortfolioPositionException("Quantidade, preço e data da operação são inválidos.");
            }
            boolean beforeAdjustment = history.stream().anyMatch(existing ->
                    existing.getType() == MovementType.POSITION_ADJUSTMENT
                    && existing.getTickerSymbol().equals(addition.getTickerSymbol())
                    && addition.getDate().isBefore(existing.getDate()));
            if (beforeAdjustment) {
                throw new PositionConflictException(
                        "A operação é anterior a um ajuste de " + addition.getTickerSymbol()
                                + ". Reconcilie o histórico antes de importar ou registrar.");
            }
        }
        List<AssetMovement> combined = new ArrayList<>(history);
        combined.addAll(additions);
        consolidate(combined);
    }

    public void requireMutableHistory(List<AssetMovement> history) {
        if (history.stream().anyMatch(m -> m.getType() == MovementType.POSITION_ADJUSTMENT)) {
            throw new PositionConflictException(
                    "Esta carteira possui ajustes auditáveis. Não é permitido substituir, editar ou excluir seu histórico.");
        }
    }

    public void advance(Portfolio portfolio) {
        portfolio.setPositionRevision(Math.addExact(portfolio.getPositionRevision(), 1));
    }
}
