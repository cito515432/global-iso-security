package com.globalisosecurity.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.globalisosecurity.backend.config.ApplicationReadiness;
import com.globalisosecurity.backend.config.DataInitializer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
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

    @Autowired
    private ApplicationReadiness applicationReadiness;

    @Autowired
    private ApplicationAvailability applicationAvailability;

    @LocalServerPort
    private int port;

    @Test
    void contextLoadsWithoutDataInitializer() {
        assertThat(applicationContext.getBeansOfType(DataInitializer.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ApplicationReadiness.class)).containsOnlyKeys("applicationReadiness");
    }

    @Test
    void springPublishesReadyEventAndBothReadinessEndpointsBecomeReady() throws Exception {
        assertThat(applicationReadiness.isReady()).isTrue();
        assertThat(applicationAvailability.getReadinessState())
                .isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);

        HttpClient client = HttpClient.newHttpClient();
        assertThat(getStatus(client, "/health")).isEqualTo(200);
        assertThat(getStatus(client, "/readiness")).isEqualTo(200);
        assertThat(getStatus(client, "/api/readiness")).isEqualTo(200);
    }

    private int getStatus(HttpClient client, String path) throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }
}
