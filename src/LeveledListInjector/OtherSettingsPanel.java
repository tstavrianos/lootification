/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import lev.gui.LCheckBox;
import lev.gui.LComboBox;
import skyproc.SPGlobal;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class OtherSettingsPanel extends SPSettingPanel {

    LCheckBox importOnStartup;
    LCheckBox LootifyDragonborn;
    LCheckBox SkipInactiveMods;
    LCheckBox UseMatchingOutfits;
    

    public OtherSettingsPanel(SPMainMenuPanel parent_) {
	super(parent_, "Other Settings", LeveledListInjector.headerColor);
    }

    @Override
    protected void initialize() {
	super.initialize();

	importOnStartup = new LCheckBox("Import Mods on Startup", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
	importOnStartup.tie(YourSaveFile.Settings.IMPORT_AT_START, LeveledListInjector.save, SUMGUI.helpPanel, true);
	importOnStartup.setOffset(2);
	importOnStartup.addShadow();
	setPlacement(importOnStartup);
	AddSetting(importOnStartup);
       
        LootifyDragonborn = new LCheckBox("Lootify the Dragonborn DLC items", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
        LootifyDragonborn.tie(YourSaveFile.Settings.LOOTIFY_DRAGONBORN, LeveledListInjector.save, SUMGUI.helpPanel, true);
        setPlacement(LootifyDragonborn);
        AddSetting(LootifyDragonborn);
        
        SkipInactiveMods = new LCheckBox("Don't process inactive mods", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
        SkipInactiveMods.tie(YourSaveFile.Settings.SKIP_INACTIVE_MODS, LeveledListInjector.save, SUMGUI.helpPanel, true);
        setPlacement(SkipInactiveMods);
        AddSetting(SkipInactiveMods);
        
        UseMatchingOutfits = new LCheckBox("Match outfits when possible", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
        UseMatchingOutfits.tie(YourSaveFile.Settings.USE_MATCHING_OUTFITS, LeveledListInjector.save, SUMGUI.helpPanel, true);
        setPlacement(UseMatchingOutfits);
        AddSetting(UseMatchingOutfits);

//	alignRight();
        
        

    }
}
