package com.ronda.backend;

import java.net.URI;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// Se ejecuta con mvn -Pintegration verify contra PostgreSQL real, nunca contra H2.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendIT {
    @Value("${local.server.port}")
    int port;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void startsWithPostgresAndAppliesTheInitialMigration() {
        assertThat(jdbc.queryForObject("select version()", String.class)).contains("PostgreSQL");
        assertThat(jdbc.queryForObject(
                "select count(*) from ronda.flyway_schema_history where version = '1' and success",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void healthChecksTheDatabaseWithoutExposingConnectionDetails() throws Exception {
        var response = get("/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"").doesNotContain("\"components\"", "\"details\"", "jdbc:postgresql");
    }

    @Test
    void exposesStatusButKeepsFutureBusinessEndpointsClosed() throws Exception {
        assertThat(get("/api/v1/status").statusCode()).isEqualTo(200);
        assertThat(get("/api/v1/usuarios").statusCode()).isEqualTo(403);
    }

    private HttpResponse<String> get(String path) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }
}
