/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.awt.event.*;
import lev.gui.*;
import skyproc.SPGlobal;
import skyproc.gui.*;
import skyproc.*;
import java.awt.BorderLayout;
import java.util.ArrayList;

/**
 *
 * @author David
 */
public class OutfitsPanel extends SPSettingPanel {

    private ArrayList<String> outfitKeys;

    private class TierListener implements ActionListener {

        private String set;
        private String newKey;
        private LComboBox box;
        private String tierName;

        TierListener(String a, LComboBox b, String name) {
            set = a;
            box = b;
            newKey = null;
            tierName = name;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String pressed = (String) box.getSelectedItem();
            if ((pressed.compareTo("None") == 0) && (newKey != null)) {
                for (LeveledListInjector.Pair<String, ArrayList<String>> p : LeveledListInjector.tiers) {
                    if (p.getBase().contentEquals(set)) {
                        p.getVar().remove(tierName + newKey);
                    }
                }
                newKey = null;
                box.clearHighlight();
            } else if (pressed.compareTo("None") != 0) {
                if (newKey != null) {
                    for (LeveledListInjector.Pair<String, ArrayList<String>> p : LeveledListInjector.tiers) {
                        if (p.getBase().contentEquals(set)) {
                            p.getVar().remove(tierName + newKey);
                        }
                    }
                }
                boolean found = false;
                for (LeveledListInjector.Pair<String, ArrayList<String>> p : LeveledListInjector.tiers) {
                    if (p.getBase().contentEquals(set)) {
                        if (!p.getVar().contains(tierName + pressed)) {
                            p.getVar().add(tierName + pressed);
                        }
                        found = true;
                    }
                }
                if(!found){
                    LeveledListInjector.Pair<String, ArrayList<String>> q = new LeveledListInjector.Pair<>(set, new ArrayList<String>(0));
                    q.getVar().add(tierName + pressed);
                    LeveledListInjector.tiers.add(q);
                }

                newKey = pressed;
                box.highlightChanged();

            }
        }
    }

    public OutfitsPanel(SPMainMenuPanel parent_) {
        super(parent_, "Outfits", LeveledListInjector.headerColor);
    }

    @Override
    protected void initialize() {
        super.initialize();

        outfitKeys = new ArrayList<>(0);


    }

    @Override
    public void onOpen(SPMainMenuPanel parent) {
        for (LeveledListInjector.Pair<String, ArrayList<ARMO>> p : LeveledListInjector.outfits) {
            String key = p.getBase();
            if (!outfitKeys.contains(key)) {
                outfitKeys.add(key);
                LPanel panel = new LPanel(275, 200);
                panel.setSize(300, 500);

                LLabel name = new LLabel(key, LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);

                LComboBox banditHeavy = new LComboBox("Bandit Heavy Tier:");
                LComboBox banditBoss = new LComboBox("Bandit Boss Tier:");
                LComboBox banditLight = new LComboBox("Bandit Light Tier:");
                LComboBox thalmor = new LComboBox("Thalmor Tier:");
                LComboBox necromancer = new LComboBox("Necromancer Tier:");
                LComboBox warlock = new LComboBox("Warlock Tier:");

                banditHeavy.addItem("None");
                banditBoss.addItem("None");
                banditLight.addItem("None");
                thalmor.addItem("None");
                necromancer.addItem("None");
                warlock.addItem("None");
                for (int i = 0; i < 30; i++) {
                    banditHeavy.addItem(String.valueOf(i));
                    banditBoss.addItem(String.valueOf(i));
                    banditLight.addItem(String.valueOf(i));
                    thalmor.addItem(String.valueOf(i));
                    necromancer.addItem(String.valueOf(i));
                    warlock.addItem(String.valueOf(i));
                }


                banditHeavy.addEnterButton("set", new TierListener(key, banditHeavy, "BanditHeavy_Tier_"));
                banditHeavy.setSize(100, 25);
                banditBoss.addEnterButton("set", new TierListener(key, banditBoss, "BanditBoss_Tier_"));
                banditBoss.setSize(100, 25);
                banditLight.addEnterButton("set", new TierListener(key, banditLight, "BanditLight_Tier_"));
                banditLight.setSize(100, 25);
                thalmor.addEnterButton("set", new TierListener(key, thalmor, "Thalmor_Tier_"));
                thalmor.setSize(100, 25);
                necromancer.addEnterButton("set", new TierListener(key, necromancer, "Necromancer_Tier_"));
                necromancer.setSize(100, 25);
                warlock.addEnterButton("set", new TierListener(key, warlock, "Warlock_Tier_"));
                warlock.setSize(100, 25);

                LLabel banditHLabel = new LLabel("Bandit Heavy Tier:", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                LLabel banditBLabel = new LLabel("Bandit Boss Tier:", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                LLabel banditLLabel = new LLabel("Bandit Light Tier:", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                LLabel thalmorLabel = new LLabel("Thalmor Tier:", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                LLabel necroLabel = new LLabel("Necromancer Tier:", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                LLabel lockLabel = new LLabel("Warlock Tier:", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);

                panel.add(name, BorderLayout.WEST);
                panel.setPlacement(name);
                panel.add(banditHLabel);
                panel.setPlacement(banditHLabel);
                panel.add(banditHeavy);
                panel.setPlacement(banditHeavy);
                panel.add(banditBLabel);
                panel.setPlacement(banditBLabel);
                panel.add(banditBoss);
                panel.setPlacement(banditBoss);
                panel.add(banditLLabel);
                panel.setPlacement(banditLLabel);
                panel.add(banditLight);
                panel.setPlacement(banditLight);
                panel.add(thalmorLabel);
                panel.setPlacement(thalmorLabel);
                panel.add(thalmor);
                panel.setPlacement(thalmor);
                panel.add(necroLabel);
                panel.setPlacement(necroLabel);
                panel.add(necromancer);
                panel.setPlacement(necromancer);
                panel.add(lockLabel);
                panel.setPlacement(lockLabel);
                panel.add(warlock);
                panel.setPlacement(warlock);

                setPlacement(panel);
                Add(panel);
            }
        }
    }
}
