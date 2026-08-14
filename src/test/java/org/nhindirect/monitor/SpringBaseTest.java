package org.nhindirect.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@DirtiesContext
@TestPropertySource("classpath:bootstrap.properties")
public abstract class SpringBaseTest
{
	protected WebClient webClient;
	
    @BeforeEach
    public void setUp()
    {
    	webClient = WebClient.builder().baseUrl("http://localhost:8080").build();
    }
}
