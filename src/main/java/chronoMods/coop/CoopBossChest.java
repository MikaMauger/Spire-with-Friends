package chronoMods.coop.relics;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.rewards.chests.*;
import com.megacrit.cardcrawl.rewards.*;
import com.megacrit.cardcrawl.cards.*;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.dungeons.*;
import com.megacrit.cardcrawl.relics.*;
import com.megacrit.cardcrawl.core.*;
import com.megacrit.cardcrawl.rooms.*;
import com.megacrit.cardcrawl.screens.select.*;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.BlightHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import java.util.ArrayList;

import chronoMods.*;
import com.evacipated.cardcrawl.modthespire.lib.*;
import basemod.*;
import basemod.interfaces.*;

public class CoopBossChest extends AbstractChest {
  private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("BossChest");
  public static final String[] TEXT = uiStrings.TEXT;
    
  public ArrayList<AbstractBlight> blights = new ArrayList<>();

    @SpirePatch(clz=BossRelicSelectScreen.class, method="update")
    public static class InsertNextChestIntoBossRoom
    {
        @SpireInsertPatch(rloc=130-93, localvars={})
        public static void Insert(BossRelicSelectScreen __instance)
        {
            if (TogetherManager.gameMode == TogetherManager.mode.Coop) { 
                TogetherManager.logger.info ("We're putting in the boss relic! Hooray");
                ((TreasureRoomBoss)AbstractDungeon.getCurrRoom()).chest = new CoopBossChest();
            }
        }
    }


  public CoopBossChest() {
    this.img = ImageMaster.BOSS_CHEST;
    this.openedImg = ImageMaster.BOSS_CHEST_OPEN;
    this.hb = new Hitbox(256.0F * Settings.scale, 200.0F * Settings.scale);
    this.hb.move(CHEST_LOC_X, CHEST_LOC_Y - 100.0F * Settings.scale);

    int choice;
    for (int i = 0; i < 2; i++) {
        choice = AbstractDungeon.relicRng.random(TogetherManager.teamBlights.size() - 1);
        this.blights.add(TogetherManager.teamBlights.get(choice));
        TogetherManager.teamBlights.remove(choice);
    }

    AbstractDungeon.overlayMenu.proceedButton.hide();
    (AbstractDungeon.getCurrRoom()).phase = AbstractRoom.RoomPhase.INCOMPLETE;
  }
  
  public void update() {
    super.update();
    if ((AbstractDungeon.getCurrRoom()).phase == AbstractRoom.RoomPhase.INCOMPLETE)
        AbstractDungeon.overlayMenu.proceedButton.hide(); 
    else
        AbstractDungeon.overlayMenu.proceedButton.show(); 
  }

  public void open(boolean bossChest) {
      CardCrawlGame.sound.play("CHEST_OPEN");
      TogetherManager.teamRelicScreen.open(this.blights);
  }
  
  public void close() {
    CardCrawlGame.sound.play("CHEST_OPEN");
    this.isOpen = false;
  }
}