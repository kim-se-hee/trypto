package ksh.tryptobackend.user.adapter.out.oauth;

public record OAuthCredentials(String clientId, String clientSecret, String redirectUri) {

    public boolean hasClientSecret() {
        return isPresent(clientSecret);
    }

    public boolean isConfigured() {
        return isPresent(clientId) && isPresent(redirectUri);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
