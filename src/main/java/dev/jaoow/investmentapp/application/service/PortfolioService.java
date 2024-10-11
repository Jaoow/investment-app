package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.PortfolioRequest;
import dev.jaoow.investmentapp.application.dto.response.PortfolioResponse;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.service.user.UserService;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;

    public PortfolioService(PortfolioRepository portfolioRepository, ModelMapper modelMapper, UserService userService) {
        this.portfolioRepository = portfolioRepository;
        this.modelMapper = modelMapper;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioByIdAndUserId(Long portfolioId, Principal principal) {
        String userEmail = principal.getName();
        Portfolio portfolio = portfolioRepository.findByIdAndUserEmail(portfolioId, userEmail).orElse(null);
        return modelMapper.map(portfolio, PortfolioResponse.class);
    }

    @Transactional(readOnly = true)
    public Page<PortfolioResponse> getAllPortfoliosByUser(Pageable pageable, Principal principal) {
        String userEmail = principal.getName();
        return portfolioRepository.findAllByUserEmail(pageable, userEmail)
                .map(portfolio -> modelMapper.map(portfolio, PortfolioResponse.class));
    }

    @Transactional
    public PortfolioResponse createPortfolio(PortfolioRequest portfolioRequest, Principal principal) {
        Portfolio portfolio = modelMapper.map(portfolioRequest, Portfolio.class);

        User userByEmail = userService.findUserByEmail(principal.getName());
        portfolio.setUser(userByEmail);

        portfolio = portfolioRepository.save(portfolio);
        return modelMapper.map(portfolio, PortfolioResponse.class);
    }

    @Transactional
    public PortfolioResponse updatePortfolio(Long id, PortfolioRequest portfolioRequest) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(id));

        portfolio.setName(portfolioRequest.getName());
        portfolio = portfolioRepository.save(portfolio);

        return modelMapper.map(portfolio, PortfolioResponse.class);
    }

    @Transactional
    public void deletePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(id));

        // Ensure all asset movements are removed when the portfolio is deleted
        portfolio.getAssetMovements().clear();
        portfolioRepository.delete(portfolio);
    }
}
