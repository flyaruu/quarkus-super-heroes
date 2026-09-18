package io.quarkus.sample.superheroes.fight.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.InternalServerErrorException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.FightImage;
import io.quarkus.sample.superheroes.fight.FightRequest;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.ShorterTimeoutsProfile;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.LocationClient;
import io.quarkus.sample.superheroes.fight.client.NarrationClient;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.config.FightConfig;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

/**
 * Tests for the service layer ({@link FightService}).
 */
@QuarkusTest
@TestProfile(ShorterTimeoutsProfile.class)
class FightServiceTests extends FightServiceTestsBase {
  private static final String FALLBACK_NARRATION = """
                                                   High above a bustling city, a symbol of hope and justice soared through the sky, while chaos reigned below, with malevolent laughter echoing through the streets.
                                                   With unwavering determination, the figure swiftly descended, effortlessly evading explosive attacks, closing the gap, and delivering a decisive blow that silenced the wicked laughter.
                                                   
                                                   In the end, the battle concluded with a clear victory for the forces of good, as their commitment to peace triumphed over the chaos and villainy that had threatened the city.
                                                   The people knew that their protector had once again ensured their safety.
                                                   """;

  private static final FightImage IMAGE = new FightImage("https://somewhere.com/someImage.png", "Fallback image");

  @InjectMock
  HeroClient heroClient;

	@InjectMock
  VillainClient villainClient;

	@InjectMock
	LocationClient locationClient;

  @InjectMock
  @RestClient
  NarrationClient narrationClient;

  @Inject
  FightConfig fightConfig;

	@Test
	void findAllFightsNoneFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.listAll())
			.thenReturn(Uni.createFrom().item(List.of()));

		var allFights = this.fightService.findAllFights()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(allFights)
			.isNotNull()
			.isEmpty();

		PanacheMock.verify(Fight.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void findAllFights() {
		PanacheMock.mock(Fight.class);
		when(Fight.listAll())
			.thenReturn(Uni.createFrom().item(List.of(createFightHeroWon())));

		var allFights = this.fightService.findAllFights()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(allFights)
			.isNotNull()
			.isNotEmpty()
      .singleElement()
      .usingRecursiveComparison()
      .isEqualTo(createFightHeroWon());

		PanacheMock.verify(Fight.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void findFightByIdFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.findById(DEFAULT_FIGHT_ID))
			.thenReturn(Uni.createFrom().item(createFightHeroWon()));

		var fight = this.fightService.findFightById(DEFAULT_FIGHT_ID.toString())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFightHeroWon());

		PanacheMock.verify(Fight.class).findById(eq(DEFAULT_FIGHT_ID));
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void findFightByIdNotFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.findById(DEFAULT_FIGHT_ID))
			.thenReturn(Uni.createFrom().nullItem());

		var fight = this.fightService.findFightById(DEFAULT_FIGHT_ID.toString())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNull();

		PanacheMock.verify(Fight.class).findById(eq(DEFAULT_FIGHT_ID));
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}


	@Test
	void performFightNullFighters() {
		PanacheMock.mock(Fight.class);

		var cve = catchThrowableOfType(
			ConstraintViolationException.class,
			() -> this.fightService.performFight(null)
		);

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.singleElement()
			.isNotNull()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				null,
				"must not be null"
			);

		verify(this.fightService, never()).determineWinner(any(FightRequest.class));
		verify(this.fightService, never()).shouldHeroWin(any(FightRequest.class));
		verify(this.fightService, never()).shouldVillainWin(any(FightRequest.class));
		verify(this.fightService, never()).getRandomWinner(any(FightRequest.class));
		verify(this.fightService, never()).heroWonFight(any(FightRequest.class));
		verify(this.fightService, never()).villainWonFight(any(FightRequest.class));
		verify(this.fightService, never()).persistFight(any(Fight.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	void performFightInvalidFighters() {
		PanacheMock.mock(Fight.class);

		var cve = catchThrowableOfType(
			ConstraintViolationException.class,
			() -> this.fightService.performFight(new FightRequest(null, createDefaultVillain(), createDefaultFightLocation()))
		);

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.singleElement()
			.isNotNull()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				null,
				"must not be null"
			);

		verify(this.fightService, never()).determineWinner(any(FightRequest.class));
		verify(this.fightService, never()).shouldHeroWin(any(FightRequest.class));
		verify(this.fightService, never()).shouldVillainWin(any(FightRequest.class));
		verify(this.fightService, never()).getRandomWinner(any(FightRequest.class));
		verify(this.fightService, never()).heroWonFight(any(FightRequest.class));
		verify(this.fightService, never()).villainWonFight(any(FightRequest.class));
		verify(this.fightService, never()).persistFight(any(Fight.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}


	@Test
	void performFightHeroShouldWin() {
    var fightOutcome = createFightHeroWon();
		var fightMatcher = fightMatcher(fightOutcome);
    var defaultFightRequest = createDefaultFightRequest();

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(eq(defaultFightRequest), any());

		doReturn(true)
      .when(this.fightService)
      .shouldHeroWin(defaultFightRequest);

		doReturn(fightOutcome)
      .when(this.fightService)
      .heroWonFight(defaultFightRequest);

		var fight = this.fightService.performFight(createDefaultFightRequest())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
      .usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		verify(this.fightService).determineWinner(defaultFightRequest);
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldHeroWin(defaultFightRequest);
		verify(this.fightService).heroWonFight(defaultFightRequest);
		verify(this.fightService, never()).shouldVillainWin(any(FightRequest.class));
		verify(this.fightService, never()).villainWonFight(any(FightRequest.class));
		verify(this.fightService, never()).getRandomWinner(any(FightRequest.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void performFightVillainShouldWin() {
    var fightOutcome = createFightVillainWon();
    var fightMatcher = fightMatcher(fightOutcome);
    var defaultFightRequest = createDefaultFightRequest();

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(false)
      .when(this.fightService)
      .shouldHeroWin(defaultFightRequest);

		doReturn(true)
      .when(this.fightService)
      .shouldVillainWin(defaultFightRequest);

		doReturn(fightOutcome)
      .when(this.fightService)
      .villainWonFight(defaultFightRequest);

		var fight = this.fightService.performFight(createDefaultFightRequest())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		verify(this.fightService).determineWinner(defaultFightRequest);
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldVillainWin(defaultFightRequest);
		verify(this.fightService).shouldHeroWin(defaultFightRequest);
		verify(this.fightService).villainWonFight(defaultFightRequest);
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		verify(this.fightService, never()).heroWonFight(any(FightRequest.class));
		verify(this.fightService, never()).getRandomWinner(any(FightRequest.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void performFightRandomWinner() {
    var fightOutcome = createFightVillainWon();
    var fightMatcher = fightMatcher(fightOutcome);
		var defaultFightRequest = createDefaultFightRequest();

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(false)
      .when(this.fightService)
      .shouldHeroWin(defaultFightRequest);

		doReturn(false)
      .when(this.fightService)
      .shouldVillainWin(defaultFightRequest);

		doReturn(fightOutcome)
      .when(this.fightService)
      .getRandomWinner(defaultFightRequest);

		var fight = this.fightService.performFight(createDefaultFightRequest())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		verify(this.fightService).determineWinner(defaultFightRequest);
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldVillainWin(defaultFightRequest);
		verify(this.fightService).shouldHeroWin(defaultFightRequest);
		verify(this.fightService).getRandomWinner(defaultFightRequest);
		verify(this.fightService, never()).villainWonFight(any(FightRequest.class));
		verify(this.fightService, never()).heroWonFight(any(FightRequest.class));
    verify(this.fightService, never()).narrateFight(any(FightToNarrate.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	void didHeroWinTrue() {
		var f = createDefaultFightRequest();
    var h = new Hero(f.hero().name(), (Integer.MAX_VALUE - this.fightConfig.hero().adjustBound()), f.hero().picture(), f.hero().powers());
    var v = new Villain(f.villain().name(), Integer.MIN_VALUE, f.villain().picture(), f.villain().powers());
    var fightRequest = new FightRequest(h, v, f.location());

		assertThat(this.fightService.shouldHeroWin(fightRequest))
			.isTrue();
	}

	@Test
	void didHeroWinFalse() {
		var f = createDefaultFightRequest();
    var h = new Hero(f.hero().name(), Integer.MIN_VALUE, f.hero().picture(), f.hero().powers());
    var v = new Villain(f.villain().name(), Integer.MAX_VALUE - this.fightConfig.hero().adjustBound(), f.villain().picture(), f.villain().powers());
    var fightRequest = new FightRequest(h, v, f.location());

		assertThat(this.fightService.shouldHeroWin(fightRequest))
			.isFalse();
	}

	@Test
	void didVillainWinTrue() {
		var f = createDefaultFightRequest();
    var h = new Hero(f.hero().name(), Integer.MIN_VALUE, f.hero().picture(), f.hero().powers());
    var v = new Villain(f.villain().name(), Integer.MAX_VALUE, f.villain().picture(), f.villain().powers());
    var fightRequest = new FightRequest(h, v, f.location());

		assertThat(this.fightService.shouldVillainWin(fightRequest))
			.isTrue();
	}

	@Test
	void didVillainWinFalse() {
		var f = createDefaultFightRequest();
    var h = new Hero(f.hero().name(), Integer.MAX_VALUE, f.hero().picture(), f.hero().powers());
    var v = new Villain(f.villain().name(), Integer.MIN_VALUE, f.villain().picture(), f.villain().powers());
    var fightRequest = new FightRequest(h, v, f.location());

		assertThat(this.fightService.shouldVillainWin(fightRequest))
			.isFalse();
	}

  private Fighters createFallbackFighters() {
    return new Fighters(createFallbackHero(), createFallbackVillain());
  }

  private FightImage getFallbackImage() {
    var image = this.fightConfig.narration().fallbackImageGeneration();
    return new FightImage(image.imageUrl(), image.imageNarration());
  }

  private static Fight createFightHeroWon() {
		var fight = new Fight();
		fight.id = DEFAULT_FIGHT_ID;
		fight.fightDate = DEFAULT_FIGHT_DATE;
		fight.winnerName = DEFAULT_HERO_NAME;
		fight.winnerLevel = DEFAULT_HERO_LEVEL;
		fight.winnerPicture = DEFAULT_HERO_PICTURE;
    fight.winnerPowers = DEFAULT_HERO_POWERS;
		fight.loserName = DEFAULT_VILLAIN_NAME;
		fight.loserLevel = DEFAULT_VILLAIN_LEVEL;
		fight.loserPicture = DEFAULT_VILLAIN_PICTURE;
    fight.loserPowers = DEFAULT_VILLAIN_POWERS;
		fight.winnerTeam = HEROES_TEAM_NAME;
		fight.loserTeam = VILLAINS_TEAM_NAME;
    fight.location = createDefaultFightLocation();

		return fight;
	}

	private static Fight createFightVillainWon() {
		var fight = new Fight();
		fight.id = DEFAULT_FIGHT_ID;
		fight.fightDate = DEFAULT_FIGHT_DATE;
		fight.winnerName = DEFAULT_VILLAIN_NAME;
		fight.winnerLevel = DEFAULT_VILLAIN_LEVEL;
		fight.winnerPicture = DEFAULT_VILLAIN_PICTURE;
    fight.winnerPowers = DEFAULT_VILLAIN_POWERS;
		fight.winnerTeam = VILLAINS_TEAM_NAME;
		fight.loserName = DEFAULT_HERO_NAME;
		fight.loserLevel = DEFAULT_HERO_LEVEL;
		fight.loserPicture = DEFAULT_HERO_PICTURE;
    fight.loserPowers = DEFAULT_HERO_POWERS;
		fight.loserTeam = HEROES_TEAM_NAME;
    fight.location = createDefaultFightLocation();

		return fight;
	}
}
