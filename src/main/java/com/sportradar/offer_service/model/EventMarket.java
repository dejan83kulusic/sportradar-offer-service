
package com.sportradar.offer_service.model;

import java.util.List;

public class EventMarket {

    private String id;
    private String marketId;
    private int status;
    private List<EventMarketOutcome> outcomes;

    public EventMarket() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<EventMarketOutcome> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<EventMarketOutcome> outcomes) {
        this.outcomes = outcomes;
    }
}
