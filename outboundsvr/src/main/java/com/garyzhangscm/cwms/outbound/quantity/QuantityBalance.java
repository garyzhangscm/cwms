package com.garyzhangscm.cwms.outbound.quantity;

/** Quantities are base-unit whole numbers, like the existing OrderLine/Pick models. */
public final class QuantityBalance {
    public final long demand, held, committed;
    public QuantityBalance(long demand, long held, long committed) {
        if (demand < 0 || held < 0 || committed < 0 || held > demand || committed > demand - held)
            throw new IllegalArgumentException("Invalid quantity allocation balance");
        this.demand = demand; this.held = held; this.committed = committed;
    }
    public QuantityBalance target(long requestedHeld, long availableWithoutThisHold) {
        if (requestedHeld < 0 || requestedHeld > demand - committed || availableWithoutThisHold < 0)
            throw new IllegalArgumentException("Requested reservation exceeds remaining demand");
        // Never reduce an existing reservation just because physical stock was adjusted elsewhere.
        long next = requestedHeld <= held ? requestedHeld :
            Math.max(held, Math.min(requestedHeld, availableWithoutThisHold));
        return new QuantityBalance(demand, next, committed);
    }
    public QuantityBalance convert(long quantity) {
        if (quantity <= 0 || quantity > held) throw new IllegalArgumentException("Allocate quantity before picking");
        return new QuantityBalance(demand, held - quantity, Math.addExact(committed, quantity));
    }
    public QuantityBalance cancelUnpicked(long quantity) {
        if (quantity <= 0 || quantity > committed) throw new IllegalArgumentException("Invalid cancelled pick quantity");
        return new QuantityBalance(demand, Math.addExact(held, quantity), committed - quantity);
    }
    public long shortage() { return demand - held - committed; }
}
