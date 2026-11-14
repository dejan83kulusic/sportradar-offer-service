package com.sportradar.offer_service.dto;

import java.util.List;

public record MarketResponse(
        String id,
        String name,
        int status,
        List<String> outcomeIds
) {
}