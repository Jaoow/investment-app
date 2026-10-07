package dev.jaoow.investmentapp.domain.entity;

import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.model.B3ImportStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(name = "b3_import_history")
public class B3ImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User importedBy;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_hash", nullable = false)
    private String fileHash;

    @Column(name = "total_negotiations_found")
    private int totalNegotiationsFound;

    @Column(name = "new_negotiations")
    private int newNegotiations;

    @Column(name = "already_imported")
    private int alreadyImported;

    @Column(name = "inconsistencies")
    private int inconsistencies;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private B3ImportStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (importedAt == null) {
            importedAt = Instant.now();
        }
    }
}
