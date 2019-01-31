package org.jboss.aerogear.unifiedpush.service.impl;

import java.util.UUID;

public class UserIdentifiers {

    private final UUID guid;
    private final UUID pushId;
    private final String client;

    public UserIdentifiers(UUID guid, UUID pushId, String client) {
        this.guid = guid;
        this.pushId = pushId;
        this.client = client;
    }

    public UUID getGuid() {
        return guid;
    }

    public UUID getPushId() {
        return pushId;
    }

    public String getClient() {
        return client;
    }
}
