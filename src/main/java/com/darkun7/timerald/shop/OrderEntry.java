package com.darkun7.timerald.shop;

import java.util.UUID;

public class OrderEntry {
    private final UUID requesterId;
    private final String requesterName;
    private final int index;
    private final OrderItem orderItem;

    public OrderEntry(UUID requesterId, String requesterName, int index, OrderItem orderItem) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.index = index;
        this.orderItem = orderItem;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public int getIndex() {
        return index;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }
}
