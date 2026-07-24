package com.coffeecommits.brakket.twitch.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class HelixTwitchGateway implements TwitchGateway {
    private final HelixClient helixClient;

    @Override
    public ChannelInfo findChannel(String login) {
        JsonNode data = helixClient.get("/users?login=" + login).path("data");
        if (!data.isArray() || data.isEmpty()) return null;
        JsonNode user = data.get(0);
        return new ChannelInfo(user.path("id").asText(), user.path("login").asText(),
                user.path("display_name").asText());
    }

    @Override
    public StreamInfo findLiveStream(String login) {
        JsonNode data = helixClient.get("/streams?user_login=" + login).path("data");
        if (!data.isArray() || data.isEmpty()) return null;
        JsonNode stream = data.get(0);
        // started_at viene en UTC: convertir a hora local antes de descartar la zona.
        return new StreamInfo(stream.path("id").asText(), stream.path("viewer_count").asInt(),
                OffsetDateTime.parse(stream.path("started_at").asText())
                        .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
    }
}
