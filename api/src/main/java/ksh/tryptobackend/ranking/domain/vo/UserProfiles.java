package ksh.tryptobackend.ranking.domain.vo;

import java.util.Map;
import java.util.Objects;

public class UserProfiles {

    private static final String UNKNOWN_NICKNAME = "";

    private final Map<Long, UserProfile> profileByUserId;

    public UserProfiles(Map<Long, UserProfile> profileByUserId) {
        this.profileByUserId = Map.copyOf(profileByUserId);
    }

    public String nicknameOf(Long userId) {
        UserProfile profile = profileByUserId.get(userId);
        return profile != null ? profile.nickname() : UNKNOWN_NICKNAME;
    }

    public boolean isPortfolioPublicOf(Long userId) {
        UserProfile profile = profileByUserId.get(userId);
        return profile != null && profile.portfolioPublic();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfiles that = (UserProfiles) o;
        return Objects.equals(profileByUserId, that.profileByUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileByUserId);
    }
}
