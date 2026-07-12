package ksh.tryptocollector.distribute.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptocollector.model.NormalizedTicker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TickerEventConflatorTest {

    @Mock
    private TickerEventPublisher publisher;

    @InjectMocks
    private TickerEventConflator conflator;

    @Test
    @DisplayName("같은 키로 여러 번 submit 해도 flush 시 마지막 1건만 batch 에 포함된다")
    void givenMultipleSubmitsSameKey_whenFlush_thenOnlyLatestInBatch() {
        NormalizedTicker t1 = ticker("UPBIT", "BTC", "KRW", "100");
        NormalizedTicker t2 = ticker("UPBIT", "BTC", "KRW", "200");
        NormalizedTicker t3 = ticker("UPBIT", "BTC", "KRW", "300");

        conflator.submit(t1);
        conflator.submit(t2);
        conflator.submit(t3);
        conflator.flush();

        verify(publisher).publishBatch(eq("UPBIT"), captureBatch().capture());
    }

    @Test
    @DisplayName("같은 거래소의 서로 다른 심볼은 1 batch 로 묶여 발행된다")
    void givenSameExchangeDifferentSymbols_whenFlush_thenSingleBatch() {
        NormalizedTicker btc = ticker("UPBIT", "BTC", "KRW", "100");
        NormalizedTicker eth = ticker("UPBIT", "ETH", "KRW", "200");

        conflator.submit(btc);
        conflator.submit(eth);
        conflator.flush();

        ArgumentCaptor<List<NormalizedTicker>> captor = captureBatch();
        verify(publisher).publishBatch(eq("UPBIT"), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(btc, eth);
    }

    @Test
    @DisplayName("거래소가 다르면 거래소별로 별도 batch 가 발행된다")
    void givenDifferentExchanges_whenFlush_thenSeparateBatchPerExchange() {
        NormalizedTicker upbit = ticker("UPBIT", "BTC", "KRW", "100");
        NormalizedTicker bithumb = ticker("BITHUMB", "BTC", "KRW", "101");

        conflator.submit(upbit);
        conflator.submit(bithumb);
        conflator.flush();

        ArgumentCaptor<List<NormalizedTicker>> captor = captureBatch();
        verify(publisher).publishBatch(eq("UPBIT"), captor.capture());
        assertThat(captor.getValue()).containsExactly(upbit);

        verify(publisher).publishBatch(eq("BITHUMB"), captor.capture());
        assertThat(captor.getValue()).containsExactly(bithumb);
    }

    @Test
    @DisplayName("submit 없이 flush 만 호출하면 publish 가 일어나지 않는다")
    void givenNoSubmit_whenFlush_thenNoPublish() {
        conflator.flush();
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("flush 후 다시 flush 해도 (새 submit 없으면) 추가 publish 가 없다")
    void givenFlushedThenFlushAgain_thenNoAdditionalPublish() {
        NormalizedTicker t = ticker("UPBIT", "BTC", "KRW", "100");
        conflator.submit(t);
        conflator.flush();
        conflator.flush();

        verify(publisher).publishBatch(eq("UPBIT"), captureBatch().capture());
    }

    private NormalizedTicker ticker(String exchange, String base, String quote, String price) {
        return new NormalizedTicker(
                exchange,
                base,
                quote,
                base + "/" + quote,
                new BigDecimal(price),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                System.currentTimeMillis());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<NormalizedTicker>> captureBatch() {
        return ArgumentCaptor.forClass(List.class);
    }
}
