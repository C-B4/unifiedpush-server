package org.jboss.aerogear.unifiedpush.service.impl;

import java.util.UUID;

public class UserIdentifiers {

    private final UUID userGuid;
    private final UUID pushId;
    private final String client;

    public UserIdentifiers(UUID userGuid, UUID pushId, String client) {
        this.userGuid = userGuid;
        this.pushId = pushId;
        this.client = client;
    }

    public UUID getUserGuid() {
        return userGuid;
    }

    public UUID getPushId() {
        return pushId;
    }

    public String getClient() {
        return client;
    }
}
