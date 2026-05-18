package ksh.tryptocollector.loadtest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/loadtest/ticker")
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadtestTickerController {

    private final SyntheticTickerGenerator generator;

    @PostMapping("/ramp")
    public ResponseEntity<GeneratorStatus> startRamp(@RequestBody StartRampRequest request) {
        log.info("loadtest ramp 시작 요청: phases={}", request.phases().size());
        generator.startRamp(request.toProfile());
        return ResponseEntity.ok(generator.status());
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stop() {
        log.info("loadtest 발행 중지 요청");
        generator.stop();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public GeneratorStatus status() {
        return generator.status();
    }
}
