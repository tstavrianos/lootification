/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

//import lev.gui.LCheckBox;
//import lev.gui.LTextPane;
import lev.gui.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WelcomePanel extends SPSettingPanel {

    LTextPane introText;
    LCheckBox processWeapons;
    LCheckBox processArmors;
    LCheckBox processOutfits;
    
    LComboBox<String> test;

    public WelcomePanel(SPMainMenuPanel parent_) {
        super(parent_, LeveledListInjector.myPatchName, LeveledListInjector.headerColor);
    }

    @Override
    protected void initialize() {
        super.initialize();

        introText = new LTextPane(settingsPanel.getWidth() - 40, 200, LeveledListInjector.settingsColor);
        introText.setText(LeveledListInjector.welcomeText);
        introText.setEditable(false);
        introText.setFont(LeveledListInjector.settingsFont);
        introText.setCentered();
        setPlacement(introText);
        Add(introText);

//        processWeapons = new LCheckBox("Process weapons", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
//        processWeapons.tie(YourSaveFile.Settings.PROCESS_WEAPONS, LeveledListInjector.save, SUMGUI.helpPanel, true);
//        setPlacement(processWeapons);
//        AddSetting(processWeapons);
//
//        processArmors = new LCheckBox("Process armors", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
//        processArmors.tie(YourSaveFile.Settings.PROCESS_ARMORS, LeveledListInjector.save, SUMGUI.helpPanel, true);
//        setPlacement(processArmors);
//        AddSetting(processArmors);
//        
//        processOutfits = new LCheckBox("Process outfits", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
//        processOutfits.tie(YourSaveFile.Settings.PROCESS_OUTFITS, LeveledListInjector.save, SUMGUI.helpPanel, true);
//        setPlacement(processOutfits);
//        AddSetting(processOutfits);
//        

        
        //alignRight();
    }
}
