---
title: Villain REST API
description: A classical HTTP microservice exposing CRUD operations on Villains, stored in a PostgreSQL database.
layout: page
content-toc: true
---

## Introduction

This is the Villain REST API microservice. It is a classical HTTP microservice exposing CRUD operations on Villains. Villain information is stored in a PostgreSQL database. This service is implemented using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) with blocking endpoints and [Quarkus Hibernate ORM with Panache's active record pattern](https://quarkus.io/guides/hibernate-orm-panache#solution-1-using-the-active-record-pattern).

This service uses a **contract-first** approach: the REST API interface is generated at build time from the OpenAPI specification (`src/main/resources/openapi/openapi.yml`) using the [Quarkiverse OpenAPI Generator Server extension](https://docs.quarkiverse.io/quarkus-openapi-generator/dev/server.html). The OpenAPI spec is the single source of truth for both the generated JAX-RS interface and the Swagger UI documentation.

Additionally, this application favors field injection of beans (i.e. `@Inject` annotation) over constructor injection.

![rest-villains]({site.image('rest-villains.png')})

### Exposed Endpoints

The following table lists the available REST endpoints. The OpenAPI document for the REST endpoints is also available.

| Path | HTTP method | Query Param(s) | Response Status | Response Object | Description |
|------|-------------|----------------|-----------------|-----------------|-------------|
| `/api/villains` | `GET` | `name_filter` | `200` | `List<Villain>` | All Villains. Empty array (`[]`) if none. Optional `name_filter` query parameter for filtering results by name (case-insensitive). |
| `/api/villains` | `POST` | | `201` | | New Villain created. `Location` header contains URL to retrieve Villain |
| `/api/villains` | `POST` | | `400` | | Invalid Villain passed in request body (or no request body found) |
| `/api/villains` | `DELETE` | | `204` | | Deletes all Villains |
| `/api/villains` | `PUT` | | `201` | | Replaces all villains with the passed-in villains. `Location` header contains URL to retrieve all Villains |
| `/api/villains` | `PUT` | | `400` | | Invalid `Villain`s passed in request body (or no request body found) |
| `/api/villains/random` | `GET` | | `200` | `Villain` | Random Villain |
| `/api/villains/random` | `GET` | | `404` | | No Villain found |
| `/api/villains/\{id}` | `GET` | | `200` | `Villain` | Villain with id == `\{id}` |
| `/api/villains/\{id}` | `GET` | | `404` | | No Villain with id == `\{id}` found |
| `/api/villains/\{id}` | `PUT` | | `204` | | Completely replaces a Villain |
| `/api/villains/\{id}` | `PUT` | | `400` | | Invalid Villain passed in request body (or no request body found) |
| `/api/villains/\{id}` | `PUT` | | `404` | | No Villain with id == `\{id}` found |
| `/api/villains/\{id}` | `PATCH` | | `200` | `Villain` | Partially updates a Villain. Returns the complete Villain. |
| `/api/villains/\{id}` | `PATCH` | | `400` | | Invalid Villain passed in request body (or no request body found) |
| `/api/villains/\{id}` | `PATCH` | | `404` | | No Villain with id == `\{id}` found |
| `/api/villains/\{id}` | `DELETE` | | `204` | | Deletes Villain with id == `\{id}` |
| `/api/villains/hello` | `GET` | | `200` | `String` | Ping "hello" endpoint |

## Contract testing with Pact

[Pact](https://pact.io) is a code-first tool for testing HTTP and message integrations using `contract tests`. Contract tests assert that inter-application messages conform to a shared understanding that is documented in a contract. Without contract testing, the only way to ensure that applications will work correctly together is by using expensive and brittle integration tests.

[Eric Deandrea](https://developers.redhat.com/author/eric-deandrea) and [Holly Cummins](https://hollycummins.com) recently spoke about contract testing with Pact and used the Quarkus Superheroes for their demos. [Watch the replay](https://www.youtube.com/watch?v=vYwkDPrzqV8) and [view the slides](https://hollycummins.com/modern-microservices-testing-pitfalls-devoxx/) if you'd like to learn more about contract testing.

The `rest-villains` application is a [Pact _Provider_](https://docs.pact.io/provider), and as such, should run provider verification tests against contracts produced by consumers.

Contracts generally should be hosted in a [Pact Broker](https://docs.pact.io/pact_broker) and then automatically discovered in the provider verification tests. One of the main goals of the Superheroes application is to be super simple and just "work" by anyone who may clone the repo. Therefore, the Pact contract is committed into the application's source tree inside the `src/test/resources/pacts` directory. In a realistic scenario, if a broker wasn't used, the consumer's CI/CD would commit the contracts into this repository's source control.

The Pact tests use the [Quarkus Pact extension](https://github.com/quarkiverse/quarkus-pact). This extension is recommended to give the best user experience and ensure compatibility.

## End-to-End UI testing with Playwright

The application UI is tested using [Playwright](https://playwright.dev/java). We are using the [Quarkus Playwright extension](https://docs.quarkiverse.io/quarkus-playwright/dev) for this.

During tests, the UI is loaded in a headless browser and interactions are tested during unit and integration tests.

## Benchmarking with Hyperfoil

There are some [Hyperfoil benchmarks](https://hyperfoil.io) available for this service. See the `hyperfoil` directory in the source repository for more details.

## Running the Application

The application runs on port `8084` (defined by `quarkus.http.port` in `application.properties`).

From the `quarkus-super-heroes/rest-villains` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), or running `quarkus dev` using the [Quarkus CLI](https://quarkus.io/guides/cli-tooling). The application will be exposed at http://localhost:8084 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8084/q/dev.

The application also contains a simple UI, showing the list of Villains currently stored. This UI is available at the root path (http://localhost:8084). The UI is built using the [Quarkus Qute templating engine](https://quarkus.io/guides/qute).

![villains-ui]({site.image('villains-ui.png')})

**NOTE:** Running the application outside of Quarkus dev mode requires standing up a PostgreSQL instance and binding it to the app. By default, the application is configured with the following:

| Description | Environment Variable | Java Property | Value |
|---|---|---|---|
| Database URL | `QUARKUS_DATASOURCE_JDBC_URL` | `quarkus.datasource.jdbc.url` | `jdbc:postgresql://localhost:5432/villains_database` |
| Database username | `QUARKUS_DATASOURCE_USERNAME` | `quarkus.datasource.username` | `superbad` |
| Database password | `QUARKUS_DATASOURCE_PASSWORD` | `quarkus.datasource.password` | `superbad` |

## Running Locally via Docker Compose

Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/rest-villains`](https://quay.io/repository/quarkus-super-heroes/rest-villains?tab=tags).

Pick one of the versions of the application from the table below and execute the appropriate docker compose command from the `quarkus-super-heroes/rest-villains` directory.

**NOTE:** You may see errors as the applications start up. This may happen if an application completes startup before one of its required services (i.e. database, etc). This is fine. Once everything completes startup things will work fine.

| Description | Image Tag | Docker Compose Run Command |
|---|---|---|
| JVM Java 25 | `java25-latest` | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` |
| Native | `native-latest` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |

These Docker Compose files are meant for standing up this application and the required database only. If you want to stand up the entire system, follow the instructions in the main project README.

Once started the application will be exposed at `http://localhost:8084`.
