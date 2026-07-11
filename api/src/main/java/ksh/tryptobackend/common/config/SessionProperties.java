package ksh.tryptobackend.common.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth.session")
public class SessionProperties {

    private boolean secure = false;
    private Duration ttl = Duration.ofDays(7);
}
