package chronoMods.ui.deathScreen;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.daily.TimeHelper;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon.CurrentScreen;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.dungeons.TheEnding;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.integrations.PublisherIntegration;
import com.megacrit.cardcrawl.localization.ScoreBonusStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.metrics.Metrics;
import com.megacrit.cardcrawl.metrics.Metrics.MetricRequestType;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.relics.SpiritPoop;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.VictoryRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.screens.stats.*;
import com.megacrit.cardcrawl.screens.*;
import com.megacrit.cardcrawl.ui.buttons.ReturnToMenuButton;
import com.megacrit.cardcrawl.ui.buttons.RetryButton;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.unlock.misc.DefectUnlock;
import com.megacrit.cardcrawl.unlock.misc.TheSilentUnlock;
import com.megacrit.cardcrawl.vfx.AscensionLevelUpTextEffect;
import com.megacrit.cardcrawl.vfx.AscensionUnlockedTextEffect;
import com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect;
import com.megacrit.cardcrawl.vfx.UnlockTextEffect;
import com.megacrit.cardcrawl.audio.MusicMaster;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.CardCrawlGame.GameMode;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.GameTips;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.metrics.MetricData;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import com.megacrit.cardcrawl.screens.DungeonTransitionScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;

import com.evacipated.cardcrawl.modthespire.lib.*;

import chronoMods.*;
import chronoMods.steam.*;
import chronoMods.ui.deathScreen.*;
import chronoMods.ui.hud.*;
import chronoMods.ui.lobby.*;
import chronoMods.ui.mainMenu.*;
import chronoMods.utilities.*;

public class RaceEndScreen {
	private static final Logger logger = LogManager.getLogger(RaceEndScreen.class.getName());
	private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("DeathScreen");
	public static final String[] TEXT = uiStrings.TEXT;

	private MonsterGroup monsters;
	private String deathText;
	private ArrayList<DeathScreenFloatyEffect> particles = new ArrayList<>();
	private static final float NUM_PARTICLES = 50;
	private float deathAnimWaitTimer = 1f;
	private static final float DEATH_TEXT_TIME = 5f;
	private float deathTextTimer = DEATH_TEXT_TIME;
	private Color defeatTextColor = Color.WHITE.cpy();
	private Color deathTextColor = Settings.BLUE_TEXT_COLOR.cpy();
	private static final float DEATH_TEXT_Y = Settings.HEIGHT - 360f * Settings.scale;
	public ReturnToMenuButton returnButton;
	public RetryButton retryButton;

	// Stats
	private static final float STAT_OFFSET_Y = 80f * Settings.scale;
	private static final float STAT_START_Y = Settings.HEIGHT / 2f - 20f * Settings.scale;
	public static final float STATS_TRANSITION_TIME = 0.5f;
	public static final float STAT_ANIM_INTERVAL = 0.1f;
	private float statsTimer = 0f, statAnimateTimer = 0f;

	// Others
	public boolean isVictory;
	public boolean showingStats = false;
	private boolean playedWhir = false;
	private long whirId;
	public static float playtime = 0F;

	public RaceEndScreen(MonsterGroup m) {

		// Remove existing death screens
		AbstractDungeon.deathScreen = null;
		AbstractDungeon.victoryScreen = null;

		// Cleanup
		playtime = (long) CardCrawlGame.playtime;

		if (playtime < 0L) {
			playtime = 0L;
		}

		AbstractDungeon.getCurrRoom().clearEvent();

		AbstractDungeon.is_victory = false;
		for (AbstractCard c : AbstractDungeon.player.hand.group) {
			c.unhover();
		}
		AbstractDungeon.dungeonMapScreen.closeInstantly();
		AbstractDungeon.screen = CurrentScreen.DEATH;
		AbstractDungeon.overlayMenu.showBlackScreen(1f);
		AbstractDungeon.previousScreen = null;
		AbstractDungeon.overlayMenu.cancelButton.hideInstantly();
		AbstractDungeon.isScreenUp = true;
		deathText = getDeathText();
		monsters = m;
		logger.info("PLAYTIME: " + playtime);

		if (SaveHelper.shouldDeleteSave()) {
			SaveAndContinue.deleteSave(AbstractDungeon.player);
		}

		CardCrawlGame.playerPref.flush();

		// Victory or Retry
		isVictory = AbstractDungeon.getCurrRoom() instanceof VictoryRoom;

		returnButton = new ReturnToMenuButton();
		retryButton = new RetryButton();

		if (isVictory) {
			returnButton.appear(Settings.WIDTH / 2f, Settings.HEIGHT * 0.15f, TEXT[0]);

			AbstractDungeon.dynamicBanner.appear(TEXT[1]);
		} else {
			retryButton.appear(Settings.WIDTH / 2f, Settings.HEIGHT * 0.15f, TEXT[33]);

			AbstractDungeon.dynamicBanner.appear(getDeathBannerText());
		}

		// Kill the music
		CardCrawlGame.music.dispose();

		if (AbstractDungeon.getCurrRoom() instanceof RestRoom) {
			((RestRoom) AbstractDungeon.getCurrRoom()).cutFireSound();
		}

		// Play death SFX
		CardCrawlGame.sound.play("DEATH_STINGER", true);

		// Play death BGM
		String bgmKey = null;
		switch (MathUtils.random(0, 3)) {
			case 0:
				bgmKey = "STS_DeathStinger_1_v3_MUSIC.ogg";
				break;
			case 1:
				bgmKey = "STS_DeathStinger_2_v3_MUSIC.ogg";
				break;
			case 2:
				bgmKey = "STS_DeathStinger_3_v3_MUSIC.ogg";
				break;
			case 3:
				bgmKey = "STS_DeathStinger_4_v3_MUSIC.ogg";
				break;
			default:
				break;
		}
		CardCrawlGame.music.playTempBgmInstantly(bgmKey, false);

		defeatTextColor.a = 0f;
		deathTextColor.a = 0f;

	}

	private String getDeathBannerText() {
		ArrayList<String> list = new ArrayList<>();
		list.add(TEXT[7]);
		list.add(TEXT[8]);
		list.add(TEXT[9]);
		list.add(TEXT[10]);
		list.add(TEXT[11]);
		list.add(TEXT[12]);
		list.add(TEXT[13]);
		list.add(TEXT[14]);

		return list.get(MathUtils.random(list.size() - 1));
	}

	private String getDeathText() {
		ArrayList<String> list = new ArrayList<>();
		list.add(TEXT[15]);
		list.add(TEXT[16]);
		list.add(TEXT[17]);
		list.add(TEXT[18]);
		list.add(TEXT[19]);
		list.add(TEXT[20]);
		list.add(TEXT[21]);
		list.add(TEXT[22]);
		list.add(TEXT[23]);
		list.add(TEXT[24]);
		list.add(TEXT[25]);
		list.add(TEXT[26]);
		list.add(TEXT[27]);
		list.add(TEXT[28]);
		list.add(TEXT[29]);

		if (AbstractDungeon.player.chosenClass == PlayerClass.THE_SILENT) {
			list.add("...");
		}

		return list.get(MathUtils.random(list.size() - 1));
	}

	public void hide() {
		returnButton.hide();
		AbstractDungeon.dynamicBanner.hide();
	}

	public void reopen() {
		reopen(false);
	}

	public void reopen(boolean fromVictoryUnlock) {
		if (isVictory) {
			AbstractDungeon.dynamicBanner.appearInstantly(TEXT[1]);
			returnButton.appear(Settings.WIDTH / 2f, Settings.HEIGHT * 0.15f, TEXT[34]);
		} else {
			AbstractDungeon.dynamicBanner.appearInstantly(TEXT[30]);
			retryButton.appear(Settings.WIDTH / 2f, Settings.HEIGHT * 0.15f, TEXT[33]);
		}
		AbstractDungeon.overlayMenu.showBlackScreen(1f);
	}

	public void update() {
		// Return Button
		returnButton.update();

		if (returnButton.hb.clicked || (returnButton.show && CInputActionSet.select.isJustPressed())) {
			CInputActionSet.topPanel.unpress();
			if (Settings.isControllerMode) {
				Gdx.input.setCursorPosition(10, Settings.HEIGHT / 2);
			}
			returnButton.hb.clicked = false;

			if (!showingStats) {
				showingStats = true;
				statsTimer = STATS_TRANSITION_TIME;
				logger.info("Clicked");

				retryButton = new RetryButton();
				returnButton = new ReturnToMenuButton();
			} else {
				if (isVictory) {
					returnButton.hide();
				} else {
					returnButton.hide();
					retryButton.hide();
				}
			}
		}

		// Retry Button
		retryButton.update();

		if (InputHelper.justClickedLeft && retryButton.hb.hovered || (retryButton.show && CInputActionSet.select.isJustPressed())) {
			retryButton.hb.clicked = false;

			restartRun();
		}

		// Timers, particles, and animations

		if (deathAnimWaitTimer != 0f) {
			deathAnimWaitTimer -= Gdx.graphics.getDeltaTime();
			if (deathAnimWaitTimer < 0f) {
				deathAnimWaitTimer = 0f;
				AbstractDungeon.player.playDeathAnimation();
			}
		} else {
			deathTextTimer -= Gdx.graphics.getDeltaTime();
			if (deathTextTimer < 0f) {
				deathTextTimer = 0f;
			}

			deathTextColor.a = Interpolation.fade.apply(0f, 1f, 1f - deathTextTimer / DEATH_TEXT_TIME);
			defeatTextColor.a = Interpolation.fade.apply(0f, 1f, 1f - deathTextTimer / DEATH_TEXT_TIME);
		}

		if (monsters != null) {
			monsters.update();
			monsters.updateAnimations();
		}

		if (particles.size() < NUM_PARTICLES) {
			particles.add(new DeathScreenFloatyEffect());
		}
	}


    public static void restartRun()
    {
    	RaceEndScreen.playtime = CardCrawlGame.playtime;
        CardCrawlGame.music.fadeAll();
        AbstractDungeon.getCurrRoom().clearEvent();
        AbstractDungeon.closeCurrentScreen();
        
        CardCrawlGame.dungeonTransitionScreen = new DungeonTransitionScreen("Exordium");
        
        AbstractDungeon.reset();
        Settings.hasEmeraldKey = false;
        Settings.hasRubyKey = false;
        Settings.hasSapphireKey = false;
        ShopScreen.resetPurgeCost();
        CardCrawlGame.tips.initialize();
        CardCrawlGame.metricData.clearData();
        CardHelper.clear();
        TipTracker.refresh();
        System.gc();

        if (CardCrawlGame.chosenCharacter == null) {
          CardCrawlGame.chosenCharacter = AbstractDungeon.player.chosenClass;
        }
        // if (!Settings.seedSet)
        // {
        //   Long sTime = Long.valueOf(System.nanoTime());
        //   Random rng = new Random(sTime);
        //   Settings.seedSourceTimestamp = sTime.longValue();
        //   Settings.seed = Long.valueOf(SeedHelper.generateUnoffensiveSeed(rng));
        //   SeedHelper.cachedSeed = null;
        // }
        AbstractDungeon.generateSeeds();
        
        CardCrawlGame.mode = CardCrawlGame.GameMode.CHAR_SELECT;
    }

    @SpirePatch(clz=AbstractDungeon.class, method="dungeonTransitionSetup")
    public static class RestoreTimer
    {
        public static void Postfix()
        {
            CardCrawlGame.playtime = RaceEndScreen.playtime;
        }
    }


	// private void updateStatsScreen() {
	// 	if (showingStats) {
	// 		progressBarAlpha = MathHelper.slowColorLerpSnap(progressBarAlpha, 1f);

	// 		statsTimer -= Gdx.graphics.getDeltaTime();
	// 		if (statsTimer < 0f) {
	// 			statsTimer = 0f;
	// 		}

	// 		returnButton.y = Interpolation.pow3In.apply(
	// 			Settings.HEIGHT * 0.1f,
	// 			Settings.HEIGHT * 0.15f,
	// 			statsTimer * 1f / STATS_TRANSITION_TIME);

	// 		AbstractDungeon.dynamicBanner.y = Interpolation.pow3In.apply(
	// 			Settings.HEIGHT - 220f * Settings.scale,
	// 			Settings.HEIGHT - 280f * Settings.scale,
	// 			statsTimer * 1f / STATS_TRANSITION_TIME);

	// 		for (GameOverStat i : stats) {
	// 			i.update();
	// 		}

	// 		if (statAnimateTimer < 0f) {
	// 			boolean allStatsShown = true;

	// 			for (GameOverStat i : stats) {
	// 				if (i.hidden) {
	// 					i.hidden = false;
	// 					statAnimateTimer = STAT_ANIM_INTERVAL;
	// 					allStatsShown = false;
	// 					break;
	// 				}
	// 			}

	// 			// Animate Progress Bar
	// 			if (allStatsShown) {
	// 				animateProgressBar();
	// 			}
	// 		} else {
	// 			statAnimateTimer -= Gdx.graphics.getDeltaTime();
	// 		}
	// 	}
	// }

	public void render(SpriteBatch sb) {
		for (Iterator<DeathScreenFloatyEffect> i = particles.iterator(); i.hasNext();) {
			DeathScreenFloatyEffect e = i.next();
			if (e.renderBehind) {
				e.render(sb);
			}
			e.update();
			if (e.isDone) {
				i.remove();
			}
		}

		AbstractDungeon.player.render(sb);
		if (monsters != null) {
			monsters.render(sb);
		}

		sb.setBlendFunction(GL30.GL_SRC_ALPHA, GL30.GL_ONE); // Additive Mode
		for (DeathScreenFloatyEffect e : particles) {
			if (!e.renderBehind) {
				e.render(sb);
			}
		}
		sb.setBlendFunction(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA); // NORMAL

		renderStatsScreen(sb);

		if (!showingStats && !isVictory) {
			FontHelper.renderFontCentered(
				sb,
				FontHelper.topPanelInfoFont,
				deathText,
				Settings.WIDTH / 2f,
				DEATH_TEXT_Y,
				deathTextColor);
		}

		returnButton.render(sb);
	}

	private void renderStatsScreen(SpriteBatch sb) {
		// if (showingStats) {
			sb.setColor(new Color(1f, 1f, 1f, 1f));
			// sb.setColor(new Color(0f, 0f, 0f, (1f - statsTimer) * 0.6f));
			// sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);

			// Chooses the starting y position for rendering stats (changes based on 1 or 2 columns)
			float y = STAT_START_Y + (TogetherManager.players.size() * STAT_OFFSET_Y / 2f);
			float x = 780f * Settings.scale;

			// Renders the stats!
			for (RemotePlayer player : TogetherManager.players) {

				// Render Portrait
				if (player.portraitImg != null) {
					sb.draw(player.portraitImg, x+26.0F, y+12.0F, 56f, 56f);
				}

				// Render Portrait frame
				// sb.draw(TogetherManager.portraitFrames.get(1), this.x, this.y);
			    sb.draw(TogetherManager.portraitFrames.get(0), x - 160.0F * Settings.scale, y - 96.0F * Settings.scale, 0.0F, 0.0F, 432.0F, 243.0F, Settings.scale, Settings.scale, 0.0F, 0, 0, 1920, 1080, false, false);

				// Draw the user name
				FontHelper.renderSmartText(sb, FontHelper.topPanelInfoFont, player.userName, x + 96.0F, y + 64.0F, Settings.CREAM_COLOR);

				// Draw time
				sb.draw(ImageMaster.TIMER_ICON, x + 88.0F,  y + 4.0F, 36f * Settings.scale, 36f * Settings.scale);
				if (player.finalTime <=0.1F) {
					FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, "Not Completed", x + 124.0F,  y + 32.0F, Settings.RED_COLOR);
				} else {
					FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, VersusTimer.returnTimeString(player.finalTime), x + 124.0F,  y + 32.0F, Settings.CREAM_COLOR);
				}

				y -= STAT_OFFSET_Y;
			}
		// }
	}
}