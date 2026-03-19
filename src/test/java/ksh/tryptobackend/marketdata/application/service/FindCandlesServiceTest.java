package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;
import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;
import ksh.tryptobackend.marketdata.domain.model.CandleInterval;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FindCandlesServiceTest {

    private CandleQueryPort candleQueryPort;
    private ExchangeQueryPort exchangeQueryPort;
    private FindCandlesService findCandlesService;

    private static final ExchangeSummary UPBIT_SUMMARY = new ExchangeSummary(1L, "UPBIT", "KRW");
    private static final ExchangeSummary BINANCE_SUMMARY = new ExchangeSummary(3L, "BINANCE", "USDT");

    @BeforeEach
    void setUp() {
        candleQueryPort = mock(CandleQueryPort.class);
        exchangeQueryPort = mock(ExchangeQueryPort.class);
        findCandlesService = new FindCandlesService(candleQueryPort, exchangeQueryPort);

        when(exchangeQueryPort.findExchangeSummaryByName("UPBIT")).thenReturn(Optional.of(UPBIT_SUMMARY));
        when(exchangeQueryPort.findExchangeSummaryByName("BINANCE")).thenReturn(Optional.of(BINANCE_SUMMARY));
    }

    @Nested
    @DisplayName("exchange 식별자 검증")
    class ExchangeValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"UP BIT", "UP@BIT", "UPBIT!", "UP.BIT"})
        @DisplayName("유효하지 않은 exchange 식별자이면 INVALID_EXCHANGE_NAME 예외가 발생한다")
        void findCandles_invalidExchange_throwsException(String exchange) {
            // Given
            FindCandlesQuery query = new FindCandlesQuery(exchange, "BTC", "1d", 60, null);

            // When & Then
            assertThatThrownBy(() -> findCandlesService.findCandles(query))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_EXCHANGE_NAME);
        }

        @Test
        @DisplayName("존재하지 않는 거래소 이름이면 EXCHANGE_NOT_FOUND 예외가 발생한다")
        void findCandles_unknownExchange_throwsException() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UNKNOWN", "BTC", "1d", 60, null);
            when(exchangeQueryPort.findExchangeSummaryByName("UNKNOWN")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> findCandlesService.findCandles(query))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EXCHANGE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("coin 식별자 검증")
    class CoinValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"BT C", "BTC!", "BT.C"})
        @DisplayName("유효하지 않은 coin 식별자이면 INVALID_COIN_SYMBOL 예외가 발생한다")
        void findCandles_invalidCoin_throwsException(String coin) {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", coin, "1d", 60, null);

            // When & Then
            assertThatThrownBy(() -> findCandlesService.findCandles(query))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COIN_SYMBOL);
        }
    }

    @Nested
    @DisplayName("InfluxDB 코인 심볼 조합")
    class CoinSymbolResolution {

        @Test
        @DisplayName("국내 거래소(UPBIT)이면 coin/KRW 형태로 조합된다")
        void findCandles_domesticExchange_combinesCoinWithKRW() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "1d", 60, null);
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());
            ArgumentCaptor<CandleFilter> captor = ArgumentCaptor.forClass(CandleFilter.class);

            // When
            findCandlesService.findCandles(query);

            // Then
            verify(candleQueryPort).findByFilter(captor.capture());
            assertThat(captor.getValue().coin()).isEqualTo("BTC/KRW");
        }

        @Test
        @DisplayName("해외 거래소(BINANCE)이면 coin/USDT 형태로 조합된다")
        void findCandles_foreignExchange_combinesCoinWithUSDT() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("BINANCE", "ETH", "1d", 60, null);
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());
            ArgumentCaptor<CandleFilter> captor = ArgumentCaptor.forClass(CandleFilter.class);

            // When
            findCandlesService.findCandles(query);

            // Then
            verify(candleQueryPort).findByFilter(captor.capture());
            assertThat(captor.getValue().coin()).isEqualTo("ETH/USDT");
        }
    }

    @Nested
    @DisplayName("interval 변환")
    class IntervalConversion {

        @Test
        @DisplayName("유효한 interval 코드가 CandleInterval enum으로 변환된다")
        void findCandles_validInterval_convertsToCandleInterval() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "4h", 60, null);
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());
            ArgumentCaptor<CandleFilter> captor = ArgumentCaptor.forClass(CandleFilter.class);

            // When
            findCandlesService.findCandles(query);

            // Then
            verify(candleQueryPort).findByFilter(captor.capture());
            assertThat(captor.getValue().interval()).isEqualTo(CandleInterval.FOUR_HOURS);
        }

        @Test
        @DisplayName("유효하지 않은 interval 코드이면 INVALID_CANDLE_INTERVAL 예외가 발생한다")
        void findCandles_invalidInterval_throwsException() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "2h", 60, null);

            // When & Then
            assertThatThrownBy(() -> findCandlesService.findCandles(query))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CANDLE_INTERVAL);
        }
    }

    @Nested
    @DisplayName("limit 기본값 적용")
    class DefaultLimit {

        @Test
        @DisplayName("limit이 null이면 기본값 60이 적용된다")
        void findCandles_nullLimit_appliesDefault60() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "1d", null, null);
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());
            ArgumentCaptor<CandleFilter> captor = ArgumentCaptor.forClass(CandleFilter.class);

            // When
            findCandlesService.findCandles(query);

            // Then
            verify(candleQueryPort).findByFilter(captor.capture());
            assertThat(captor.getValue().limit()).isEqualTo(60);
        }

        @Test
        @DisplayName("limit이 지정되면 해당 값이 사용된다")
        void findCandles_specifiedLimit_usesGivenValue() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "1d", 100, null);
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());
            ArgumentCaptor<CandleFilter> captor = ArgumentCaptor.forClass(CandleFilter.class);

            // When
            findCandlesService.findCandles(query);

            // Then
            verify(candleQueryPort).findByFilter(captor.capture());
            assertThat(captor.getValue().limit()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("cursor 파싱")
    class CursorParsing {

        @Test
        @DisplayName("cursor가 null이면 CandleFilter의 cursor도 null이다")
        void findCandles_nullCursor_filterCursorIsNull() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "1d", 60, null);
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());
            ArgumentCaptor<CandleFilter> captor = ArgumentCaptor.forClass(CandleFilter.class);

            // When
            findCandlesService.findCandles(query);

            // Then
            verify(candleQueryPort).findByFilter(captor.capture());
            assertThat(captor.getValue().cursor()).isNull();
        }

        @Test
        @DisplayName("cursor가 빈 문자열이면 CandleFilter의 cursor는 null이다")
        void findCandles_emptyCursor_filterCursorIsNull() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "1d", 60, "");
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());
            ArgumentCaptor<CandleFilter> captor = ArgumentCaptor.forClass(CandleFilter.class);

            // When
            findCandlesService.findCandles(query);

            // Then
            verify(candleQueryPort).findByFilter(captor.capture());
            assertThat(captor.getValue().cursor()).isNull();
        }

        @Test
        @DisplayName("유효한 ISO 8601 cursor가 Instant로 파싱된다")
        void findCandles_validCursor_parsedToInstant() {
            // Given
            String cursorStr = "2026-03-10T00:00:00Z";
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "1d", 60, cursorStr);
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());
            ArgumentCaptor<CandleFilter> captor = ArgumentCaptor.forClass(CandleFilter.class);

            // When
            findCandlesService.findCandles(query);

            // Then
            verify(candleQueryPort).findByFilter(captor.capture());
            assertThat(captor.getValue().cursor()).isEqualTo(Instant.parse(cursorStr));
        }
    }

    @Nested
    @DisplayName("CandleQueryPort 호출 결과 반환")
    class PortResult {

        @Test
        @DisplayName("CandleQueryPort가 반환한 캔들 목록을 그대로 반환한다")
        void findCandles_returnsPortResult() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "1d", 60, null);
            List<Candle> expected = List.of(
                new Candle(Instant.parse("2026-03-10T00:00:00Z"),
                    new BigDecimal("68500000"), new BigDecimal("69200000"),
                    new BigDecimal("67800000"), new BigDecimal("68900000"))
            );
            when(candleQueryPort.findByFilter(any())).thenReturn(expected);

            // When
            List<Candle> result = findCandlesService.findCandles(query);

            // Then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("결과가 없으면 빈 리스트를 반환한다")
        void findCandles_emptyResult_returnsEmptyList() {
            // Given
            FindCandlesQuery query = new FindCandlesQuery("UPBIT", "BTC", "1d", 60, null);
            when(candleQueryPort.findByFilter(any())).thenReturn(List.of());

            // When
            List<Candle> result = findCandlesService.findCandles(query);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
