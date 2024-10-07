package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.PortfolioRequest;
import dev.jaoow.investmentapp.application.dto.response.PortfolioResponse;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final ModelMapper modelMapper;

    public PortfolioService(PortfolioRepository portfolioRepository, ModelMapper modelMapper) {
        this.portfolioRepository = portfolioRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(id));
        return modelMapper.map(portfolio, PortfolioResponse.class);
    }

    @Transactional(readOnly = true)
    public List<PortfolioResponse> getAllPortfolios() {
        return portfolioRepository.findAll().stream()
                .map(portfolio -> modelMapper.map(portfolio, PortfolioResponse.class))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PortfolioResponse> getAllPortfolios(Pageable pageable) {
        return portfolioRepository.findAll(pageable)
                .map(portfolio -> modelMapper.map(portfolio, PortfolioResponse.class));
    }

    @Transactional
    public PortfolioResponse createPortfolio(PortfolioRequest portfolioRequest) {
        Portfolio portfolio = modelMapper.map(portfolioRequest, Portfolio.class);
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
