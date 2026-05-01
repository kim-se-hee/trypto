# 제공

패키지: `ksh.tryptobackend.user.application.port.in`

## FindUserPublicInfoUseCase
- `findByUserId(Long userId) → Optional<UserPublicInfoResult>`
- `findByUserIds(Set<Long> userIds) → List<UserPublicInfoResult>`
- Returns `UserPublicInfoResult { userId: Long, nickname: String, portfolioPublic: boolean }`

# 의존

다른 컨텍스트에 의존하지 않는다.
