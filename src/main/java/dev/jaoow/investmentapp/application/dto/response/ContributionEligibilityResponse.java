package dev.jaoow.investmentapp.application.dto.response;

import java.util.Map;

public record ContributionEligibilityResponse(Map<String, Boolean> assets) {
}