package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.OwnerId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionRegistry implements Notifier {

    private final Map<OwnerId, ClientSession> sessions = new ConcurrentHashMap<>();

    public void register(OwnerId owner, ClientSession session) {
        Validation.requireNotNull(owner, "Owner");
        Validation.requireNotNull(session, "Session");
        sessions.put(owner, session);
    }

    public void unregister(OwnerId owner) {
        sessions.remove(owner);
    }

    @Override
    public void notifyOwner(OwnerId owner, String line) {
        ClientSession session = sessions.get(owner);
        if (session != null) {
            session.send(List.of(line));
        }
    }
}
