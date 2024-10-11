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

    List<Portfolio> findByUser(User user);

    Optional<Portfolio> findByIdAndUser(Long id, User user);

    Optional<Portfolio> findByIdAndUserEmail(Long id, String email);

    Page<Portfolio> findAllByUserEmail(Pageable pageable, String email);

}
