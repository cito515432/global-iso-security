package com.globalisosecurity.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"SEED_ENABLED=false",
		"SEED_DEMO_DATA=false",
		"RPM_ML_ENABLED=false",
		"SPRING_JPA_HIBERNATE_DDL_AUTO=none",
		"SPRING_JPA_SHOW_SQL=false",
		"JWT_SECRET=ci-only-startup-secret-with-at-least-32-characters"
})
class BackendApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsAvailableAfterContextStartup() {
		var response = restTemplate.getForEntity("/health", String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
	}

}
