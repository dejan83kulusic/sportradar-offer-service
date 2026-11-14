package com.sportradar.offer_service.dto;

import java.util.List;

public record OfferResponse(
        List<EventResponse> events,
        List<MarketResponse> markets,
        List<OutcomeResponse> outcomes
) {
}