package ksh.tryptobackend.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientTypeTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("클라이언트 유형이 없으면 웹으로 간주한다")
    void fromNullable_absentValue_returnsWeb(String absent) {
        assertThat(ClientType.fromNullable(absent)).isEqualTo(ClientType.WEB);
    }

    @ParameterizedTest
    @CsvSource({"WEB,WEB", "web,WEB", "ANDROID,ANDROID", "android,ANDROID"})
    @DisplayName("대소문자와 무관하게 클라이언트 유형을 해석한다")
    void fromNullable_supportedValue_returnsClientType(String name, ClientType expected) {
        assertThat(ClientType.fromNullable(name)).isEqualTo(expected);
    }

    @Test
    @DisplayName("지원하지 않는 클라이언트 유형은 예외를 던진다")
    void from_unsupportedValue_throwsInvalidClientType() {
        assertThatThrownBy(() -> ClientType.from("desktop"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CLIENT_TYPE);
    }
}
