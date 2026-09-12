---
title: Hero REST API
description: A reactive HTTP microservice exposing CRUD operations on Heroes, stored in a PostgreSQL database.
layout: page
content-toc: true
---

## Introduction

This is the Hero REST API microservice. It is a reactive HTTP microservice exposing CRUD operations on Heroes. Hero information is stored in a PostgreSQL database. This service is implemented using [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) with reactive endpoints and [Quarkus Hibernate Reactive with Panache's repository pattern](https://quarkus.io/guides/hibernate-reactive-panache#solution-2-using-the-repository-pattern).

This service uses a **contract-first** approach: the REST API interface is generated at build time from the OpenAPI specification (`src/main/resources/openapi/openapi.yml`) using the [Quarkiverse OpenAPI Generator Server extension](https://docs.quarkiverse.io/quarkus-openapi-generator/dev/server.html). The OpenAPI spec is the single source of truth for both the generated JAX-RS interface and the Swagger UI documentation.

Additionally, this application favors constructor injection of beans over field injection (i.e. `@Inject` annotation).

![rest-heroes]({site.image('rest-heroes.png')})

### Exposed Endpoints

The following table lists the available REST endpoints. The OpenAPI document for the REST endpoints is also available.

| Path | HTTP method | Query Param(s) | Response Status | Response Object | Description |
|------|-------------|----------------|-----------------|-----------------|-------------|
| `/api/heroes` | `GET` | `name_filter` | `200` | `List<Hero>` | All Heroes. Empty array (`[]`) if none. Optional `name_filter` query parameter for filtering results by name (case-insensitive). |
| `/api/heroes` | `POST` | | `201` | | New Hero created. `Location` header contains URL to retrieve Hero |
| `/api/heroes` | `POST` | | `400` | | Invalid Hero passed in request body (or no request body found) |
| `/api/heroes` | `DELETE` | | `204` | | Deletes all Heroes |
| `/api/heroes` | `PUT` | | `201` | | Replaces all heroes with the passed-in heroes. `Location` header contains URL to retrieve all Heroes |
| `/api/heroes` | `PUT` | | `400` | | Invalid `Hero`s passed in request body (or no request body found) |
| `/api/heroes/random` | `GET` | | `200` | `Hero` | Random Hero |
| `/api/heroes/random` | `GET` | | `404` | | No Hero found |
| `/api/heroes/\{id}` | `GET` | | `200` | `Hero` | Hero with id == `\{id}` |
| `/api/heroes/\{id}` | `GET` | | `404` | | No Hero with id == `\{id}` found |
| `/api/heroes/\{id}` | `PUT` | | `204` | | Completely replaces a Hero |
| `/api/heroes/\{id}` | `PUT` | | `400` | | Invalid Hero passed in request body (or no request body found) |
| `/api/heroes/\{id}` | `PUT` | | `404` | | No Hero with id == `\{id}` found |
| `/api/heroes/\{id}` | `PATCH` | | `200` | `Hero` | Partially updates a Hero. Returns the complete Hero. |
| `/api/heroes/\{id}` | `PATCH` | | `400` | | Invalid Hero passed in request body (or no request body found) |
| `/api/heroes/\{id}` | `PATCH` | | `404` | | No Hero with id == `\{id}` found |
| `/api/heroes/\{id}` | `DELETE` | | `204` | | Deletes Hero with id == `\{id}` |
| `/api/heroes/hello` | `GET` | | `200` | `String` | Ping "hello" endpoint |

## Contract testing with Pact

[Pact](https://pact.io) is a code-first tool for testing HTTP and message integrations using `contract tests`. Contract tests assert that inter-application messages conform to a shared understanding that is documented in a contract. Without contract testing, the only way to ensure that applications will work correctly together is by using expensive and brittle integration tests.

[Eric Deandrea](https://developers.redhat.com/author/eric-deandrea) and [Holly Cummins](https://hollycummins.com) recently spoke about contract testing with Pact and used the Quarkus Superheroes for their demos. [Watch the replay](https://www.youtube.com/watch?v=vYwkDPrzqV8) and [view the slides](https://hollycummins.com/modern-microservices-testing-pitfalls-devoxx/) if you'd like to learn more about contract testing.

The `rest-heroes` application is a [Pact _Provider_](https://docs.pact.io/provider), and as such, should run provider verification tests against contracts produced by consumers.

Contracts generally should be hosted in a [Pact Broker](https://docs.pact.io/pact_broker) and then automatically discovered in the provider verification tests. One of the main goals of the Superheroes application is to be super simple and just "work" by anyone who may clone the repo. Therefore, the Pact contract is committed into the application's source tree inside the `src/test/resources/pacts` directory. In a realistic scenario, if a broker wasn't used, the consumer's CI/CD would commit the contracts into this repository's source control.

The Pact tests use the [Quarkus Pact extension](https://github.com/quarkiverse/quarkus-pact). This extension is recommended to give the best user experience and ensure compatibility.

## End-to-End UI testing with Playwright

The application UI is tested using [Playwright](https://playwright.dev/java). We are using the [Quarkus Playwright extension](https://docs.quarkiverse.io/quarkus-playwright/dev) for this.

During tests, the UI is loaded in a headless browser and interactions are tested during unit and integration tests.

## Benchmarking with Hyperfoil

There are some [Hyperfoil benchmarks](https://hyperfoil.io) available for this service. See the `hyperfoil` directory in the source repository for more details.

## Running the Application

The application runs on port `8083` (defined by `quarkus.http.port` in `application.yml`).

From the `quarkus-super-heroes/rest-heroes` directory, simply run `./mvnw quarkus:dev` to run [Quarkus Dev Mode](https://quarkus.io/guides/maven-tooling#dev-mode), or running `quarkus dev` using the [Quarkus CLI](https://quarkus.io/guides/cli-tooling). The application will be exposed at http://localhost:8083 and the [Quarkus Dev UI](https://quarkus.io/guides/dev-ui) will be exposed at http://localhost:8083/q/dev.

The application also contains a simple UI, showing the list of Heroes currently stored. This UI is available at the root path (http://localhost:8083). The UI is built using the [Quarkus Qute templating engine](https://quarkus.io/guides/qute).

![heroes-ui]({site.image('heroes-ui.png')})

**NOTE:** Running the application outside of Quarkus dev mode requires standing up a PostgreSQL instance and binding it to the app. By default, the application is configured with the following:

| Description | Environment Variable | Java Property | Value |
|---|---|---|---|
| Database URL | `QUARKUS_DATASOURCE_REACTIVE_URL` | `quarkus.datasource.reactive.url` | `postgresql://localhost:5432/heroes_database` |
| Database username | `QUARKUS_DATASOURCE_USERNAME` | `quarkus.datasource.username` | `superman` |
| Database password | `QUARKUS_DATASOURCE_PASSWORD` | `quarkus.datasource.password` | `superman` |

## Running Locally via Docker Compose

Pre-built images for this application can be found at [`quay.io/quarkus-super-heroes/rest-heroes`](https://quay.io/repository/quarkus-super-heroes/rest-heroes?tab=tags).

Pick one of the versions of the application from the table below and execute the appropriate docker compose command from the `quarkus-super-heroes/rest-heroes` directory.

**NOTE:** You may see errors as the applications start up. This may happen if an application completes startup before one of its required services (i.e. database, etc). This is fine. Once everything completes startup things will work fine.

| Description | Image Tag | Docker Compose Run Command |
|---|---|---|
| JVM Java 25 | `java25-latest` | `docker compose -f deploy/docker-compose/java25.yml up --remove-orphans` |
| Native | `native-latest` | `docker compose -f deploy/docker-compose/native.yml up --remove-orphans` |

These Docker Compose files are meant for standing up this application and the required database only. If you want to stand up the entire system, follow the instructions in the main project README.

Once started the application will be exposed at `http://localhost:8083`.
