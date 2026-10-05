package dev.jaoow.investmentapp.application.dto.request;

import java.util.Map;

public record ContributionEligibilityRequest(Map<String, Boolean> assets) {
}