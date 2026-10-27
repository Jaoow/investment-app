package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Portfolio p where p.id = :id")
    Optional<Portfolio> findByIdForPositionUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<Portfolio> findByUser(User user);

    Optional<Portfolio> findByIdAndUser(Long id, User user);

    Optional<Portfolio> findByIdAndUserEmail(Long id, String email);

    Page<Portfolio> findAllByUserEmail(Pageable pageable, String email);

}
