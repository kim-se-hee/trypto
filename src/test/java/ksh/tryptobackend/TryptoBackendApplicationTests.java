package ksh.tryptobackend;

import ksh.tryptobackend.acceptance.MockAdapterConfiguration;
import ksh.tryptobackend.acceptance.TestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestContainerConfiguration.class, MockAdapterConfiguration.class})
class TryptoBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
