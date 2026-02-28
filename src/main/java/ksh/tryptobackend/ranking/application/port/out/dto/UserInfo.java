package ksh.tryptobackend.ranking.application.port.out.dto;

public record UserInfo(
    Long userId,
    String nickname,
    boolean portfolioPublic
) {
}
