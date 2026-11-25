package com.moharajli.miniexchange.domain;


import com.moharajli.miniexchange.common.Validation;

public record Order(long id, long ownerId, Side side, long quantity, long price) {

    public Order {
        Validation.requirePositive(ownerId, "Owner id");
        Validation.requireNotNull(side, "Side");
        Validation.requirePositive(quantity, "Quantity");
        Validation.requirePositive(price, "Price");
    }

    public Order withQuantity(long newQuantity) {
        return new Order(this.id, this.ownerId, this.side, newQuantity, this.price);
    }
}
