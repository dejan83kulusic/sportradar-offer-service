package com.sportradar.offer_service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
        String id,
        String name,
        LocalDateTime startsAt,
        int status,
        List<EventMarketResponse> markets
) {
    public record EventMarketResponse(
            String id,
            String marketId,
            int status,
            List<EventMarketOutcomeResponse> outcomes
    ) {
    }

    public record EventMarketOutcomeResponse(
            String id,
            String outcomeId,
            int status,
            double odds
    ) {
    }
}