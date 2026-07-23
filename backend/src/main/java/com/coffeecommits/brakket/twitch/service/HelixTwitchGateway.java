package com.coffeecommits.brakket.twitch.service;

import com.coffeecommits.brakket.config.TwitchProperties;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class HelixTwitchGateway implements TwitchGateway {
    private final TwitchProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public ChannelInfo findChannel(String login) {
        JsonNode data = get("/users?login=" + login).path("data");
        if (!data.isArray() || data.isEmpty()) return null;
        JsonNode user = data.get(0);
        return new ChannelInfo(user.path("id").asText(), user.path("login").asText(),
                user.path("display_name").asText());
    }

    @Override
    public StreamInfo findLiveStream(String login) {
        JsonNode data = get("/streams?user_login=" + login).path("data");
        if (!data.isArray() || data.isEmpty()) return null;
        JsonNode stream = data.get(0);
        return new StreamInfo(stream.path("id").asText(), stream.path("viewer_count").asInt(),
                OffsetDateTime.parse(stream.path("started_at").asText()).toLocalDateTime());
    }

    private JsonNode get(String path) {
        if (!properties.isConfigured()) {
            throw new BusinessException("Configure TWITCH_CLIENT_ID y TWITCH_CLIENT_SECRET antes de validar el canal.");
        }
        try {
            RestClient auth = restClientBuilder.baseUrl(properties.getAuthBaseUrl()).build();
            JsonNode tokenResponse = auth.post()
                    .uri(uri -> uri.path("/token")
                            .queryParam("client_id", properties.getClientId())
                            .queryParam("client_secret", properties.getClientSecret())
                            .queryParam("grant_type", "client_credentials").build())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .retrieve().body(JsonNode.class);
            String token = tokenResponse == null ? "" : tokenResponse.path("access_token").asText();
            return restClientBuilder.baseUrl(properties.getApiBaseUrl()).build().get().uri(path)
                    .header("Client-Id", properties.getClientId())
                    .header("Authorization", "Bearer " + token)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new TwitchUnavailableException("Twitch rechazó la conexión o no está disponible.", ex);
        }
    }
}

