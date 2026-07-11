package ksh.tryptobackend.user.adapter.out.acl;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {}
