package io.quarkus.sample.superheroes.fight.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.quarkus.sample.superheroes.fight.HeroesVillainsNarrationWiremockServerResource;
import io.quarkus.sample.superheroes.fight.InjectWireMock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

/**
 * Tests for the {@link io.quarkus.sample.superheroes.fight.client.VillainClient}. Uses wiremock to stub responses and verify interactions.
 * @see HeroesVillainsNarrationWiremockServerResource
 */
@QuarkusTest
@WithTestResource(HeroesVillainsNarrationWiremockServerResource.class)
class VillainClientTests {
  private static final String VILLAIN_API_BASE_URI = "/api/villains";
  private static final String VILLAIN_RANDOM_URI = VILLAIN_API_BASE_URI + "/random";
  private static final String VILLAIN_HELLO_URI = VILLAIN_API_BASE_URI + "/hello";

  private static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
  private static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
  private static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
  private static final int DEFAULT_VILLAIN_LEVEL = 42;
  private static final String DEFAULT_HELLO_RESPONSE = "Hello villains!";

  private static final Villain DEFAULT_VILLAIN = new Villain(
    DEFAULT_VILLAIN_NAME,
    DEFAULT_VILLAIN_LEVEL,
    DEFAULT_VILLAIN_PICTURE,
    DEFAULT_VILLAIN_POWERS
  );

  @Inject
  VillainClient villainClient;

  @InjectWireMock
  WireMockServer wireMockServer;

  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  void beforeEach() {
    this.wireMockServer.resetAll();
  }

  @Test
  void findsRandom() {
    this.wireMockServer.stubFor(
      get(urlEqualTo(VILLAIN_RANDOM_URI))
        .willReturn(okForContentType(APPLICATION_JSON, getDefaultVillainJson()))
    );

    IntStream.range(0, 5)
      .forEach(i -> {
        var villain = this.villainClient.findRandomVillain()
          .subscribe().withSubscriber(UniAssertSubscriber.create())
          .assertSubscribed()
          .awaitItem(Duration.ofSeconds(10))
          .getItem();

        assertThat(villain)
          .isNotNull()
          .isEqualTo(DEFAULT_VILLAIN);
      });

    this.wireMockServer.verify(5,
      getRequestedFor(urlEqualTo(VILLAIN_RANDOM_URI))
        .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
    );
  }

  @Test
  void recoversFrom404() {
    this.wireMockServer.stubFor(
      get(urlEqualTo(VILLAIN_RANDOM_URI))
        .willReturn(notFound())
    );

    IntStream.range(0, 5)
      .forEach(i -> this.villainClient.findRandomVillain()
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .assertSubscribed()
        .awaitItem(Duration.ofSeconds(5))
        .assertItem(null)
      );

    this.wireMockServer.verify(5,
      getRequestedFor(urlEqualTo(VILLAIN_RANDOM_URI))
        .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
    );
  }


  @Test
  void helloVillains() {
    this.wireMockServer.stubFor(
      get(urlEqualTo(VILLAIN_HELLO_URI))
        .willReturn(okForContentType(TEXT_PLAIN, DEFAULT_HELLO_RESPONSE))
    );

    this.villainClient.helloVillains()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .assertItem(DEFAULT_HELLO_RESPONSE);

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(VILLAIN_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

  private String getDefaultVillainJson() {
    try {
      return this.objectMapper.writeValueAsString(DEFAULT_VILLAIN);
    }
    catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }
}
