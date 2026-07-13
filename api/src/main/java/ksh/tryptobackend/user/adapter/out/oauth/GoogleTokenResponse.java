package ksh.tryptobackend.user.adapter.out.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken) {}
