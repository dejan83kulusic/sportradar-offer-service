
package com.sportradar.offer_service.model;

import java.time.LocalDateTime;
import java.util.List;

public class Event {

    private String id;
    private String name;
    private LocalDateTime startsAt;
    private int status;
    private List<EventMarket> markets;

    public Event() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<EventMarket> getMarkets() {
        return markets;
    }

    public void setMarkets(List<EventMarket> markets) {
        this.markets = markets;
    }
}
