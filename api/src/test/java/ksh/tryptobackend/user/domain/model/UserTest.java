package ksh.tryptobackend.user.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    @Nested
    @DisplayName("닉네임 변경")
    class ChangeNicknameTest {

        private User user;

        @BeforeEach
        void setUp() {
            user = createUser("기존닉네임");
        }

        @Test
        @DisplayName("정상 변경 — 닉네임이 새 값으로 바뀐다")
        void changeNickname_validNickname_changed() {
            // When
            user.changeNickname("새닉네임");

            // Then
            assertThat(user.getNickname().value()).isEqualTo("새닉네임");
        }

        @Test
        @DisplayName("경계값 — 2자(최소 길이) 닉네임 변경 성공")
        void changeNickname_minLength_success() {
            // When
            user.changeNickname("가나");

            // Then
            assertThat(user.getNickname().value()).isEqualTo("가나");
        }

        @Test
        @DisplayName("경계값 — 20자(최대 길이) 닉네임 변경 성공")
        void changeNickname_maxLength_success() {
            // Given
            String twentyChars = "가나다라마바사아자차카타파하가나다라마바";

            // When
            user.changeNickname(twentyChars);

            // Then
            assertThat(user.getNickname().value()).isEqualTo(twentyChars);
        }

        @Test
        @DisplayName("경계값 — 1자 닉네임은 길이 위반으로 실패한다")
        void changeNickname_belowMinLength_throwsException() {
            assertThatThrownBy(() -> user.changeNickname("가"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_NICKNAME_LENGTH);
        }

        @Test
        @DisplayName("경계값 — 21자 닉네임은 길이 위반으로 실패한다")
        void changeNickname_aboveMaxLength_throwsException() {
            // Given
            String twentyOneChars = "가나다라마바사아자차카타파하가나다라마바사";

            // Then
            assertThatThrownBy(() -> user.changeNickname(twentyOneChars))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_NICKNAME_LENGTH);
        }

        @Test
        @DisplayName("현재 닉네임과 동일한 값으로 변경 시 실패한다")
        void changeNickname_sameAsCurrent_throwsException() {
            assertThatThrownBy(() -> user.changeNickname("기존닉네임"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NICKNAME_SAME_AS_CURRENT);
        }
    }

    private static User createUser(String nickname) {
        return User.reconstitute(
                1L, 0L, "test@test.com", nickname, false, LocalDateTime.now(), LocalDateTime.now());
    }
}
