package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.domain.OwnerId;

@FunctionalInterface
public interface Notifier {

    void notifyOwner(OwnerId owner, String line);
}
