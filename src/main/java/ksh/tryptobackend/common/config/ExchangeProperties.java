package ksh.tryptobackend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class ExchangeProperties {

    private List<ExchangeConfig> exchanges = List.of();

    @Getter
    @Setter
    public static class ExchangeConfig {
        private String name;
        private String marketType;
        private String baseCurrencySymbol;
        private BigDecimal feeRate;
    }
}
