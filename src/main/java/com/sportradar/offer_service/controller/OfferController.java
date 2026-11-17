
package com.sportradar.offer_service.controller;

import com.sportradar.offer_service.dto.OfferResponse;
import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.model.Market;
import com.sportradar.offer_service.service.OfferService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/v1/offer")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping
    public OfferResponse getOffer() {
        return offerService.getOffer();
    }

    @GetMapping("/events")
    public List<Event> getUpcomingEvents(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {

        return offerService.getUpcomingEvents(startDate);
    }

    @GetMapping("/markets")
    public Collection<Market> getAllMarkets() {
        return offerService.getAllMarkets();
    }
}
