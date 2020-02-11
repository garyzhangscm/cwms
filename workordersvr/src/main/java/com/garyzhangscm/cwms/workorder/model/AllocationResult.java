package com.garyzhangscm.cwms.workorder.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AllocationResult implements Serializable {

    List<Pick> picks = new ArrayList<>();

    List<ShortAllocation> shortAllocations = new ArrayList<>();

    @Override
    public String toString() {
        return new StringBuilder().append("AllocationResult: \n")
                .append("Pick List: ").append(picks)
                .append("Short Allocation List：").append(shortAllocations)
                .toString();
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
    }

    public List<ShortAllocation> getShortAllocations() {
        return shortAllocations;
    }

    public void setShortAllocations(List<ShortAllocation> shortAllocations) {
        this.shortAllocations = shortAllocations;
    }

    public void addPick(Pick pick) {
        picks.add(pick);
    }

    public void addShortAllocation(ShortAllocation shortAllocation) {
        shortAllocations.add(shortAllocation);
    }

    public void addPicks(List<Pick> picks) {
        this.picks.addAll(picks);
    }

    public void addShortAllocations(List<ShortAllocation> shortAllocations) {
        this.shortAllocations.addAll(shortAllocations);
    }
}
