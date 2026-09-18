package io.quarkus.sample.superheroes.fight.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.FightImage;
import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.fight.FightRequest;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.LocationClient;
import io.quarkus.sample.superheroes.fight.client.NarrationClient;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.config.FightConfig;
import io.quarkus.sample.superheroes.fight.mapping.FightMapper;

import io.smallrye.mutiny.Uni;

/**
 * Business logic for the Fight service
 */
@ApplicationScoped
public class FightService {
	private final HeroClient heroClient;
	private final VillainClient villainClient;
  private final NarrationClient narrationClient;
	private final LocationClient locationClient;
	private final FightConfig fightConfig;
  private final FightMapper fightMapper;
	private final Random random = new Random();

	public FightService(HeroClient heroClient, VillainClient villainClient, @RestClient NarrationClient narrationClient, LocationClient locationClient, FightConfig fightConfig, FightMapper fightMapper) {
		this.heroClient = heroClient;
		this.villainClient = villainClient;
    this.narrationClient = narrationClient;
		this.locationClient = locationClient;
		this.fightConfig = fightConfig;
    this.fightMapper = fightMapper;
  }

	/**
	 * Adds a pre-configured delay to a response. Can be used to showcase/demo how to add delays as well as how to use fault tolerance.
	 * @param fighters The {@link Fighters}.
	 * @return A {@link Uni} with a configured delay added
	 * @see FightConfig#process()
	 */
	Uni<Fighters> addDelay(Uni<Fighters> fighters) {
		long delayMillis = this.fightConfig.process().delayMillis();

		return (delayMillis > 0) ?
		       fighters.onItem().delayIt().by(Duration.ofMillis(delayMillis))
			       .invoke(() -> Log.debugf("Adding delay of %d millis to request", delayMillis)) :
		       fighters;
	}

	public Uni<List<Fight>> findAllFights() {
    Log.debug("Getting all fights");
		return Fight.listAll();
	}

	public Uni<Fight> findFightById(String id) {
    Log.debugf("Finding fight by id = %s", id);
		return Fight.findById(new ObjectId(id));
	}

	public Uni<Fighters> findRandomFighters() {
    Log.debug("Finding random fighters");

    var villain = findRandomVillain();

		var hero = findRandomHero();

    return addDelay(
      Uni.combine()
        .all()
        .unis(hero, villain)
        .with(Fighters::new)
    );
	}

  public Uni<FightLocation> findRandomLocation() {
    Log.debug("Finding a random location");
    return this.locationClient.findRandomLocation()
      .invoke(location -> Log.debugf("Got random location: %s", location));
  }

	Uni<Hero> findRandomHero() {
    Log.debug("Finding a random hero");
		return this.heroClient.findRandomHero()
			.invoke(hero -> Log.debugf("Got random hero: %s", hero));
	}

	Uni<Villain> findRandomVillain() {
    Log.debug("Finding a random villain");
		return this.villainClient.findRandomVillain()
			.invoke(villain -> Log.debugf("Got random villain: %s", villain));
	}

  public Uni<String> helloHeroes() {
    Log.debug("Pinging heroes service");
    return this.heroClient.helloHeroes()
      .invoke(hello -> Log.debugf("Got %s from the Heroes microservice", hello));
  }

  public Uni<String> helloNarration() {
    Log.debug("Pinging narration service");
    return this.narrationClient.hello()
      .invoke(hello -> Log.debugf("Got %s back from the Narration microservice", hello));
  }

	public Uni<String> helloLocations() {
		Log.debug("Pinging location service");
		return this.locationClient.helloLocations()
			.invoke(hello -> Log.debugf("Got %s back from the Locations microservice", hello));
	}

  public Uni<String> helloVillains() {
    Log.debug("Pinging villains service");
    return this.villainClient.helloVillains()
      .invoke(hello -> Log.debugf("Got %s from the Villains microservice", hello));
  }

	public Uni<Fight> performFight(@NotNull @Valid FightRequest fightRequest) {
    Log.debugf("Performing a fight with fighters: %s", fightRequest);
    return determineWinner(fightRequest)
      .chain(this::persistFight);
  }

  public Uni<String> narrateFight(FightToNarrate fight) {
    Log.debugf("Narrating fight: %s", fight);
    return this.narrationClient.narrate(fight);
  }

  public Uni<FightImage> generateImageFromNarration(String narration) {
    Log.debugf("Generating image for narration: %s", narration);
    return this.narrationClient.generateImageFromNarration(narration);
  }

	Uni<Fight> persistFight(Fight fight) {
    Log.debugf("Persisting a fight: %s", fight);
		return Fight.persist(fight)
      .replaceWith(fight)
      .map(this.fightMapper::toSchema)
      .replaceWith(fight);
	}

	Uni<Fight> determineWinner(FightRequest fightRequest) {
    Log.debugf("Determining winner between fighters: %s", fightRequest);

		// Amazingly fancy logic to determine the winner...
		return Uni.createFrom().item(() -> {
				Fight fight;

				if (shouldHeroWin(fightRequest)) {
					fight = heroWonFight(fightRequest);
				}
				else if (shouldVillainWin(fightRequest)) {
					fight = villainWonFight(fightRequest);
				}
				else {
					fight = getRandomWinner(fightRequest);
				}

				fight.fightDate = Instant.now();

				return fight;
			}
		);
	}

	boolean shouldHeroWin(FightRequest fightRequest) {
		int heroAdjust = this.random.nextInt(this.fightConfig.hero().adjustBound());
		int villainAdjust = this.random.nextInt(this.fightConfig.villain().adjustBound());

		return (fightRequest.hero().level() + heroAdjust) > (fightRequest.villain().level() + villainAdjust);
	}

	boolean shouldVillainWin(FightRequest fightRequest) {
		return fightRequest.hero().level() < fightRequest.villain().level();
	}

	Fight getRandomWinner(FightRequest fightRequest) {
		return this.random.nextBoolean() ?
		       heroWonFight(fightRequest) :
		       villainWonFight(fightRequest);
	}

	Fight heroWonFight(FightRequest fightRequest) {
		Log.infof("Yes, Hero %s won over %s :o)", fightRequest.hero().name(), fightRequest.villain().name());

		Fight fight = new Fight();
		fight.winnerName = fightRequest.hero().name();
		fight.winnerPicture = fightRequest.hero().picture();
		fight.winnerLevel = fightRequest.hero().level();
    fight.winnerPowers = fightRequest.hero().powers();
		fight.loserName = fightRequest.villain().name();
		fight.loserPicture = fightRequest.villain().picture();
		fight.loserLevel = fightRequest.villain().level();
    fight.loserPowers = fightRequest.villain().powers();
		fight.winnerTeam = this.fightConfig.hero().teamName();
		fight.loserTeam = this.fightConfig.villain().teamName();
    fight.location = fightRequest.location();

		return fight;
	}

	Fight villainWonFight(FightRequest fightRequest) {
		Log.infof("Gee, Villain %s won over %s :o(", fightRequest.villain().name(), fightRequest.hero().name());

		Fight fight = new Fight();
		fight.winnerName = fightRequest.villain().name();
		fight.winnerPicture = fightRequest.villain().picture();
		fight.winnerLevel = fightRequest.villain().level();
    fight.winnerPowers = fightRequest.villain().powers();
		fight.loserName = fightRequest.hero().name();
		fight.loserPicture = fightRequest.hero().picture();
		fight.loserLevel = fightRequest.hero().level();
    fight.loserPowers = fightRequest.hero().powers();
		fight.winnerTeam = this.fightConfig.villain().teamName();
		fight.loserTeam = this.fightConfig.hero().teamName();
    fight.location = fightRequest.location();

		return fight;
	}
}
