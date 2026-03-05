package ksh.tryptobackend.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({TestContainerConfiguration.class, MockAdapterConfiguration.class, BatchMockAdapterConfiguration.class})
public class CucumberSpringConfiguration {
}
