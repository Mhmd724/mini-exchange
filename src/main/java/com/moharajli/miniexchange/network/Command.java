package com.moharajli.miniexchange.network;

public sealed interface Command permits PlaceOrder, CancelOrder, ShowBook {
}
