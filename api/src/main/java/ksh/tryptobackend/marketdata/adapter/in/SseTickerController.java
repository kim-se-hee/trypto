package ksh.tryptobackend.marketdata.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse/tickers")
@RequiredArgsConstructor
public class SseTickerController {

    // 0L = 무제한. 클라이언트가 거래소 탭을 바꾸거나 연결을 끊을 때까지 emitter 를 유지한다.
    private static final long EMITTER_TIMEOUT_MILLIS = 0L;

    private final SseTickerEmitterRegistry registry;

    @GetMapping(value = "/{exchangeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable long exchangeId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        registry.register(exchangeId, emitter);
        return emitter;
    }
}
