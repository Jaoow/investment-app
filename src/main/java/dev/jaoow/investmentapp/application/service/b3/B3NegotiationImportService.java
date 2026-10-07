package dev.jaoow.investmentapp.application.service.b3;

import dev.jaoow.investmentapp.application.dto.b3.B3NegotiationItemDto;
import dev.jaoow.investmentapp.application.dto.b3.B3NegotiationSnapshotDto;
import dev.jaoow.investmentapp.application.dto.response.B3ImportResultResponse;
import dev.jaoow.investmentapp.application.exception.B3ImportException;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.B3ImportHistory;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.model.B3ImportStatus;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.B3ImportHistoryRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class B3NegotiationImportService {

    private final B3NegotiationFileParser fileParser;
    private final B3ImportHistoryRepository importHistoryRepository;
    private final AssetMovementRepository assetMovementRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final dev.jaoow.investmentapp.infrastructure.client.BrapiClient brapiClient;

    public B3NegotiationImportService(
            B3NegotiationFileParser fileParser,
            B3ImportHistoryRepository importHistoryRepository,
            AssetMovementRepository assetMovementRepository,
            PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            dev.jaoow.investmentapp.infrastructure.client.BrapiClient brapiClient) {
        this.fileParser = fileParser;
        this.importHistoryRepository = importHistoryRepository;
        this.assetMovementRepository = assetMovementRepository;
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.brapiClient = brapiClient;
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public B3ImportResultResponse importNegotiations(Long portfolioId, MultipartFile file, Principal principal) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        // 1. Parse File
        B3NegotiationSnapshotDto snapshot = fileParser.parse(file);

        // Resolve old tickers
        resolveTickers(snapshot.getItems());

        // Idempotency: check if exact file was imported
        importHistoryRepository.findByPortfolioIdAndFileHash(portfolioId, snapshot.getFileHash())
                .ifPresent(existing -> {
                    if (existing.getStatus() == B3ImportStatus.SUCCESS) {
                        throw new B3ImportException("This negotiation spreadsheet has already been imported.");
                    }
                });

        B3ImportHistory history = new B3ImportHistory();
        history.setPortfolio(portfolio);
        history.setImportedBy(user);
        history.setFileName(snapshot.getFileName());
        history.setFileHash(snapshot.getFileHash());
        history.setTotalNegotiationsFound(snapshot.getItems().size());
        
        try {
            // 2. Deduplication using hashes
            List<String> hashes = snapshot.getItems().stream()
                    .map(B3NegotiationItemDto::getHash)
                    .toList();

            Set<String> existingHashes = assetMovementRepository
                    .findAllByPortfolioAndExternalReferenceIn(portfolio, hashes)
                    .stream()
                    .map(AssetMovement::getExternalReference)
                    .collect(Collectors.toSet());

            List<AssetMovement> newMovements = new ArrayList<>();
            int alreadyImported = 0;

            for (B3NegotiationItemDto item : snapshot.getItems()) {
                if (existingHashes.contains(item.getHash())) {
                    alreadyImported++;
                } else {
                    AssetMovement movement = new AssetMovement();
                    movement.setPortfolio(portfolio);
                    movement.setTickerSymbol(item.getTickerSymbol());
                    movement.setType(item.getType());
                    movement.setQuantity(item.getQuantity());
                    movement.setPrice(item.getPrice());
                    movement.setDate(item.getDate());
                    movement.setExternalReference(item.getHash());
                    newMovements.add(movement);
                }
            }

            // 3. Persist new movements
            if (!newMovements.isEmpty()) {
                assetMovementRepository.saveAll(newMovements);
            }

            history.setNewNegotiations(newMovements.size());
            history.setAlreadyImported(alreadyImported);
            history.setStatus(B3ImportStatus.SUCCESS);
            
        } catch (Exception e) {
            history.setStatus(B3ImportStatus.FAILED);
            history.setErrorMessage(e.getMessage());
            importHistoryRepository.save(history);
            throw new B3ImportException("Failed to process B3 negotiations: " + e.getMessage(), e);
        }

        history = importHistoryRepository.save(history);
        return toResultResponse(history);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public PagedModel<B3ImportResultResponse> getImportHistory(Long portfolioId, Pageable pageable) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        Page<B3ImportHistory> page = importHistoryRepository.findAllByPortfolio(portfolio, pageable);
        return new PagedModel<>(page.map(this::toResultResponse));
    }

    private B3ImportResultResponse toResultResponse(B3ImportHistory history) {
        return B3ImportResultResponse.builder()
                .id(history.getId())
                .portfolioId(history.getPortfolio().getId())
                .fileName(history.getFileName())
                .importedAt(history.getImportedAt())
                .status(history.getStatus())
                .totalNegotiationsFound(history.getTotalNegotiationsFound())
                .newNegotiations(history.getNewNegotiations())
                .alreadyImported(history.getAlreadyImported())
                .inconsistencies(history.getInconsistencies())
                .errorMessage(history.getErrorMessage())
                .build();
    }

    private void resolveTickers(List<B3NegotiationItemDto> items) {
        List<String> distinctTickers = items.stream()
                .map(B3NegotiationItemDto::getTickerSymbol)
                .distinct()
                .toList();

        Map<String, String> resolvedMap = new java.util.HashMap<>();
        int batchSize = 20;

        for (int i = 0; i < distinctTickers.size(); i += batchSize) {
            List<String> batch = distinctTickers.subList(i, Math.min(i + batchSize, distinctTickers.size()));
            try {
                List<dev.jaoow.investmentapp.application.dto.response.BrapiTickerResolveResultDto> results = brapiClient.resolveTickers(batch);
                for (dev.jaoow.investmentapp.application.dto.response.BrapiTickerResolveResultDto result : results) {
                    if (result.isChanged() && result.getSymbol() != null) {
                        resolvedMap.put(result.getRequestedSymbol(), result.getSymbol());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to resolve tickers batch: {}", e.getMessage());
            }
        }

        if (!resolvedMap.isEmpty()) {
            for (B3NegotiationItemDto item : items) {
                if (resolvedMap.containsKey(item.getTickerSymbol())) {
                    String newTicker = resolvedMap.get(item.getTickerSymbol());
                    log.info("Resolving old ticker {} to {}", item.getTickerSymbol(), newTicker);
                    item.setTickerSymbol(newTicker);
                }
            }
        }
    }
}
