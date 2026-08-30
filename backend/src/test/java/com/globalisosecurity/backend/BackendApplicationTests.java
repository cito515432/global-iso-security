package com.globalisosecurity.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.globalisosecurity.backend.config.DataInitializer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
		webEnvironment = RANDOM_PORT,
		properties = {
			"spring.datasource.url=jdbc:h2:mem:globaliso_startup;MODE=MySQL;DB_CLOSE_DELAY=-1",
			"spring.datasource.username=sa",
			"spring.datasource.password=",
			"spring.jpa.hibernate.ddl-auto=none",
			"spring.jpa.show-sql=false",
			"jwt.secret=ci-startup-secret-with-at-least-32-characters",
			"app.seed.enabled=false",
			"app.seed.demo-data=false",
			"app.rpm.ml.enabled=false"
		})
class BackendApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@LocalServerPort
	private int port;

	@Test
	void contextLoads() {
		assertThat(applicationContext.getBeansOfType(DataInitializer.class)).isEmpty();
	}

	@Test
	void applicationReachesHealthAndReadinessWithoutInitializer() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> health = client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/health")).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(health.statusCode()).isEqualTo(200);

		HttpResponse<String> readiness = client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/readiness")).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(readiness.statusCode()).isEqualTo(200);
	}

}
