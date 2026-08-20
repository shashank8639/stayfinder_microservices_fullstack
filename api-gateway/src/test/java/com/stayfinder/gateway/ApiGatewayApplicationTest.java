package com.stayfinder.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks"
        }
)
@AutoConfigureWebTestClient
class ApiGatewayApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointIsUp() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void publicHotelsCatalogDoesNotRequireJwt() {
        webTestClient.get()
                .uri("/api/bookings/hotels")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void myBookingsWithoutJwtIsUnauthorized() {
        webTestClient.get()
                .uri("/api/bookings/my")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void publicFoodMenuDoesNotRequireJwt() {
        webTestClient.get()
                .uri("/api/food/hotels/1/menu")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void authMeWithoutJwtIsUnauthorized() {
        webTestClient.get()
                .uri("/api/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
