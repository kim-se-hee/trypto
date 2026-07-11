package ksh.tryptobackend.user.domain.vo;

public record SocialIdentity(Provider provider, String providerId) {

    public static SocialIdentity of(Provider provider, String providerId) {
        return new SocialIdentity(provider, providerId);
    }

    public String providerName() {
        return provider.name();
    }
}
