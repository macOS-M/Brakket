package com.coffeecommits.brakket.twitch.service;

import java.time.LocalDateTime;

public interface TwitchGateway {
    ChannelInfo findChannel(String login);
    StreamInfo findLiveStream(String login);

    record ChannelInfo(String id, String login, String displayName) {}
    record StreamInfo(String id, int viewers, LocalDateTime startedAt) {}
}

