package org.nhindirect.monitor;

import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@DirtiesContext
@TestPropertySource("classpath:bootstrap.properties")
public abstract class SpringBaseTest
{
	@Autowired
	protected TestRestTemplate testRestTemplate;
}
