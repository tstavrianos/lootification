/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.util.ArrayList;
import java.util.Arrays;
import skyproc.*;

/**
 *
 * @author David Tynan
 */
public class ArmorTools {

    public static ArrayList<Pair<KYWD, KYWD>> armorMatches;
    private static ArrayList<ArrayList<FormID>> armorVariants = new ArrayList<>(0);
    private static ArrayList<Pair<KYWD, ArrayList<ARMO>>> matchingSets = new ArrayList<>(0);
    //private static Mod merger;
    //private static Mod patch;

    public static class Pair<L, R> {

        private L l;
        private R r;

        public Pair(L l, R r) {
            this.l = l;
            this.r = r;
        }

        public L getBase() {
            return l;
        }

        public R getVar() {
            return r;
        }

        public void setBase(L l) {
            this.l = l;
        }

        public void setVar(R r) {
            this.r = r;
        }
    }

//    public static void setMergeAndPatch(Mod m, Mod p) {
//        merger = m;
//        patch = p;
//    }
    static void buildOutfitsArmors(FLST baseArmorKeysFLST, Mod merger, Mod patch) {
        FormID curForm;
        ARMO curARMO;
        OTFT curOTFT;
        
//        FormID f = new FormID("107347", "Skyrim.esm");
        //SPGlobal.log("outfits glist", f.toString());
//        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
        //SPGlobal.log("outfits glist", glist + "");
        //setupSets(merger, patch);

//        glist.set(LeveledRecord.LVLFlag.UseAll, false);
        for (OTFT lotft : merger.getOutfits()) {
            String lotftName = lotft.getEDID();
            boolean tiered = isTiered(lotftName);
            if (tiered && LeveledListInjector.save.getBool(YourSaveFile.Settings.USE_MATCHING_OUTFITS)) {
                //lotft.clearInventoryItems();
                ArrayList<FormID> inv = lotft.getInventoryList();
                for (FormID form : inv) {
                    if (form.getMaster().print().startsWith("Skyrim")) {
                        lotft.removeInventoryItem(form);
                    }
                }

                String bits = getBits(lotftName);
                LVLI subList = new LVLI("DienesLVLI" + lotftName + bits + "List");
                subList.set(LeveledRecord.LVLFlag.UseAll, false);
                String tierKey = getTierKey(lotftName);


                insertTieredArmors(subList, tierKey, bits, merger, patch);
                try {
                if (subList.getEntry(0).getLevel() > 1) {
                    subList.getEntry(0).setLevel(1);
                }
                } catch (RuntimeException e) {
                    String error = e.getMessage() + "\nOutfit: " + lotft + " is coded to have a matching set but could not find any."
                            + "\nYou are almost certainly not using a tesedit merged patch correctly.";
                    RuntimeException ex = new RuntimeException(error);
                    ex.setStackTrace(e.getStackTrace());
                    throw ex;
                }
                lotft.addInventoryItem(subList.getForm());

                if (needsShield(lotftName)) {
                    lotft.addInventoryItem(shieldForm(lotftName));
                }

                patch.addRecord(subList);
                patch.addRecord(lotft);
            } else {
                ArrayList<FormID> a = lotft.getInventoryList();
                boolean changed = false;
                if (LeveledListInjector.save.getBool(YourSaveFile.Settings.USE_MATCHING_OUTFITS)) {
                    FormID form1;
                    for (int i = 0; i < a.size(); i++) {
                        form1 = a.get(i);
                        ARMO arm = (ARMO) merger.getMajor(form1, GRUP_TYPE.ARMO);
                        if (arm != null) {
                            KYWD k = hasKeyStartsWith(arm, "dienes_outfit", merger);
                            if (k != null) {
                                ArrayList<ARMO> b = getAllWithKey(k, a, merger);
                                if (b.size() > 1) {
                                    String lvliName = getNameFromArrayWithKey(b, k, merger);
                                    LVLI list = (LVLI) patch.getMajor(lvliName, GRUP_TYPE.LVLI);
                                    if (list != null) {
                                        for (ARMO arm2 : b) {
                                            lotft.removeInventoryItem(arm2.getForm());
                                        }
                                        lotft.addInventoryItem(list.getForm());
                                    } else {
                                        for (ARMO arm2 : b) {
                                            lotft.removeInventoryItem(arm2.getForm());
                                        }
                                        LVLI newList = new LVLI(lvliName);
                                        newList.set(LeveledRecord.LVLFlag.UseAll, true);
                                        newList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                                        newList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
//                                LVLI subList = (LVLI) patch.makeCopy(glist, lvliName.replace("Outfit", "OutfitSublist"));
//                                addArmorFromArray(subList, b);
//                                patch.addRecord(subList);
//                                newList.addEntry(subList.getForm(), 1, 1);
                                        addAlternateOutfits(newList, b, merger, patch);

                                        lotft.addInventoryItem(newList.getForm());
                                        patch.addRecord(newList);

                                    }
                                    changed = true;
                                    i = -1;
                                    a = lotft.getInventoryList();
                                }
                            }
                        }
                    }
                }

                //matching set armor moved to sublist, link any remaining weapons or armor
                //first refresh whats in the outfit
                a = lotft.getInventoryList();
                for (FormID form : a) {
                    ARMO obj = (ARMO) merger.getMajor(form, GRUP_TYPE.ARMO);
                    if (obj != null) {
                        KYWD baseKey = armorHasAnyKeyword(obj, baseArmorKeysFLST, merger);

                        if ((baseKey != null) && (hasVariant(obj))) {
                            String eid = "DienesLVLI" + obj.getEDID();
                            MajorRecord r = merger.getMajor(eid, GRUP_TYPE.LVLI);
                            if (r == null) {
                                LVLI subList = new LVLI(eid);
                                InsertArmorVariants(subList, form);
                                patch.addRecord(subList);
                                lotft.removeInventoryItem(form);
                                lotft.addInventoryItem(subList.getForm());
                                changed = true;
                            } else {
                                lotft.removeInventoryItem(form);
                                lotft.addInventoryItem(r.getForm());
                                changed = true;
                            }
                        }
                    }
                }
                if (changed) {
                    patch.addRecord(lotft);
                }
            }
        }
    }

    public static ArrayList<FormID> containsArmorSet(ArrayList<FormID> inventory, Mod merger) {
        ArrayList<FormID> set = new ArrayList<>(0);
        ArrayList<String> suffixes = new ArrayList<>(Arrays.asList("Boots", "Cuirass", "Gauntlets", "Helmet", "Shield"));
        boolean matchFound = false;
        for (int count = 0; count < inventory.size() && matchFound == false; count++) {
            ARMO obj = (ARMO) merger.getMajor(inventory.get(count), GRUP_TYPE.ARMO);
            if (obj != null) {
                String armorType;
                String name = obj.getEDID();
                if (name.startsWith("Ench")) {
                    name = name.substring(4);
                }
                int i;
                for (String s : suffixes) {
                    i = name.indexOf(s);
                    if (i > 0) {
                        name = name.substring(0, i);
                    }
                }
                armorType = name;
                for (int rest = count; rest < inventory.size(); rest++) {
                    ARMO other = (ARMO) merger.getMajor(inventory.get(count), GRUP_TYPE.ARMO);
                    if (other != null) {
                        String compare = other.getEDID();
                        if (compare.contains(armorType)) {
                            set.add(other.getForm());
                            matchFound = true;
                        }
                    }
                }
                if (matchFound) {
                    set.add(obj.getForm());
                }
            }
        }
        return set;
    }

    private static ArrayList<FormID> lvliContainsArmorSet(LVLI llist, Mod merger) {
        ArrayList<FormID> contentForms = new ArrayList<>(0);
        ArrayList<LeveledEntry> levContents = llist.getEntries();

        for (LeveledEntry levEntry : levContents) {
            contentForms.add(levEntry.getForm());
        }
        ArrayList<FormID> ret = containsArmorSet(contentForms, merger);

        return ret;
    }

    static void linkLVLIArmors(FLST baseArmorKeysFLST, Mod merger, Mod patch) {
//        FormID f = new FormID("107347", "Skyrim.esm");
//        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
//        glist.set(LeveledRecord.LVLFlag.UseAll, false);

        for (LVLI llist : merger.getLeveledItems()) {
            //SPGlobal.log("Link Armor List", llist.getEDID());

//            //check if LVLI is one we made
            boolean found = false;
            if (llist.getEDID().startsWith("DienesLVLI")) {
                found = true;
            }
//            for (FormID set : matchingSetVariants) {
//                if (llist.getForm().equals(set)) {
//                    found = true;
//                }
//            }


            if (found == false) {

                boolean changed = false;
//                if (llist.get(LeveledRecord.LVLFlag.UseAll)) {
//                    //remove any matching outfits
//                    //ArrayList<FormID> set = lvliContainsArmorSet(llist, merger);
//                    while (set.size() > 0) {
//                        linkArmorSet(llist, set, merger, patch);
//                        set = lvliContainsArmorSet(llist, merger);
//                        changed = true;
//                    }
//                }
                //SPGlobal.log(llist.getEDID(), "num entries" + llist.numEntries());
                for (int i = 0; i < llist.numEntries(); i++) {
                    LeveledEntry entry = llist.getEntry(i);
                    FormID test = entry.getForm();
                    //SPGlobal.log("list entry " + i, entry.getForm() + "");
                    ARMO obj = (ARMO) merger.getMajor(test, GRUP_TYPE.ARMO);
                    if (obj != null) {
                        //SPGlobal.log("list entry " + i, obj.getEDID());
                        KYWD base = armorHasAnyKeyword(obj, baseArmorKeysFLST, merger);

                        boolean hasVar = hasVariant(obj);
                        if ((base != null) && (hasVar)) {
                            //SPGlobal.log(obj.getEDID(), "has keyword" + base);

                            String eid = "DienesLVLI" + obj.getEDID();
                            MajorRecord r;

                            r = merger.getMajor(eid, GRUP_TYPE.LVLI);
                            if (r == null) {
                                r = patch.getMajor(eid, GRUP_TYPE.LVLI);
                            }
                            if (r == null) {
                                //SPGlobal.log(obj.getEDID(), "new sublist needed");
                                LVLI subList = new LVLI(eid);
                                InsertArmorVariants(subList, entry.getForm());
                                patch.addRecord(subList);
                                llist.removeEntry(i);
                                llist.addEntry(new LeveledEntry(subList.getForm(), entry.getLevel(), entry.getCount()));
                                i = -1;
                                changed = true;
                            } else {
                                //SPGlobal.log(obj.getEDID(), "sublist found " + r.getEDID());
                                llist.removeEntry(i);
                                llist.addEntry(new LeveledEntry(r.getForm(), entry.getLevel(), entry.getCount()));
                                changed = true;
                                i = -1;
                            }
                        }
                    }
                }
                if (changed) {
                    patch.addRecord(llist);
                }
            }
        }
    }

    static void buildArmorBases(Mod merger, FLST baseKeys) {
        for (ARMO armor : merger.getArmors()) {
            KYWD baseKey = armorHasAnyKeyword(armor, baseKeys, merger);
            if (baseKey != null) {
                //SPGlobal.log(armor.getEDID(), "is base armor");
                ArrayList<FormID> alts = new ArrayList<>(0);
                alts.add(0, armor.getForm());
                armorVariants.add(alts);
            }
        }
    }

    static void buildArmorVariants(Mod merger, Mod patch, FLST baseKeys, FLST varKeys) {
        SPGlobal.log("Build Variants", "Building Base Armors");
        //buildArmorBases(merger, baseKeys);
        SPGlobal.log("Build Variants", "Building Variant Armors");

        for (ARMO armor : merger.getArmors()) {
            //SPGlobal.log("armor", armor.getEDID());
            KYWD variantKey = armorHasAnyKeyword(armor, varKeys, merger);
            if (variantKey != null) {
                //SPGlobal.log(armor.getEDID(), "is variant");
                FormID ench = armor.getEnchantment();
                if (ench.isNull()) {
                    for (int j = 0; j < armorVariants.size(); j++) {
                        ArrayList<FormID> a2 = armorVariants.get(j);
                        ARMO form = (ARMO) merger.getMajor((FormID) a2.get(0), GRUP_TYPE.ARMO);

                        boolean passed = true;
                        //SPGlobal.log("comparing to", form.getEDID());


                        if (armorHasKeyword(form, getBaseArmor(variantKey), merger)) {

                            //SPGlobal.log(form.getEDID(), "has base keyword");

                            ARMO replace = form;
                            FormID tmp = replace.getTemplate();
                            if (!tmp.isNull()) {
                                replace = (ARMO) merger.getMajor(tmp, GRUP_TYPE.ARMO);
                            }
                            for (skyproc.genenums.FirstPersonFlags c : skyproc.genenums.FirstPersonFlags.values()) {
                                //skyproc.genenums.FirstPersonFlags[] test = skyproc.genenums.FirstPersonFlags.values();
                                //SPGlobal.log("getFlags", c.toString());
                                boolean armorFlag = armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
                                boolean formFlag = replace.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);

                                boolean flagMatch = (armorFlag == formFlag);
                                //SPGlobal.log("flag match" + c, armorFlag + " " + formFlag + " " + flagMatch);
                                if (flagMatch == false) {
                                    passed = false;
                                }
                            }
                            if (!passed) {
                                KYWD helm = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
                                if (armorHasKeyword(replace, helm, merger) && armorHasKeyword(armor, helm, merger)) {
                                    passed = true;
                                }
                            }
                            if (passed) {
                                //SPGlobal.log("variant found", armor.getEDID() + " is variant of " + form.getEDID());
                                FormID template = form.getTemplate();
                                //SPGlobal.log("template", template.getFormStr());
                                if (template.isNull()) {
                                    a2.add(armor.getForm());
                                    //SPGlobal.log("variant added", a2.contains(armor.getForm()) + " " + a2.size());
                                } else {
                                    //SPGlobal.log("Enchant found", armor.getEDID() + "  " + form.getEDID());
                                    String name = generateArmorName(armor, form, merger);
                                    String newEdid = generateArmorEDID(armor, form, merger);
                                    ARMO armorDupe = (ARMO) patch.makeCopy(armor, "DienesARMO" + newEdid);
                                    //SPGlobal.log("armor copied", armorDupe.getEDID());
                                    armorDupe.setEnchantment(form.getEnchantment());
                                    armorDupe.setName(name);
                                    armorDupe.setTemplate(armor.getForm());
                                    a2.add(armorDupe.getForm());
                                    patch.addRecord(armorDupe);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static String getSetName(ArrayList<FormID> set) {
        String name = String.valueOf(set.hashCode());
        return name;
    }

    static KYWD getBaseArmor(KYWD k) {
        KYWD ret = null;
        for (Pair p : armorMatches) {
            KYWD var = (KYWD) p.getVar();
            //SPGlobal.log("getBaseArmor", k.getEDID() + " " + var.getEDID() + " " + var.equals(k));
            if (var.equals(k)) {
                ret = (KYWD) p.getBase();
            }
        }
        return ret;
    }

    static void setupArmorMatches(FLST base, FLST var, Mod merger) {
        armorMatches = new ArrayList<>();
        ArrayList<FormID> bases = base.getFormIDEntries();
        ArrayList<FormID> vars = var.getFormIDEntries();
        for (int i = 0; i < bases.size(); i++) {
            KYWD newBase = (KYWD) merger.getMajor(bases.get(i), GRUP_TYPE.KYWD);
            KYWD newVar = (KYWD) merger.getMajor(vars.get(i), GRUP_TYPE.KYWD);
            //SPGlobal.log("Armor pair", newBase.getEDID() + " " + newVar.getEDID());
            Pair<KYWD, KYWD> p = new Pair(newBase, newVar);
            armorMatches.add(p);
            //SPGlobal.log("Armor pair", p.getBase().getEDID() + " " + p.getVar().getEDID());
        }
    }

    static String generateArmorEDID(ARMO newArmor, ARMO armor, Mod m) {
//        String name = newArmor.getEDID();
//        String baseName = armor.getEDID();
//        String prefix = "";
//        String suffix = "";
//        ARMO template = (ARMO) m.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
//        int prefixLen = baseName.indexOf(template.getEDID());
//        if (prefixLen > 0) {
//            prefix = baseName.substring(0, prefixLen);
//        }
//        int suffixLen = baseName.length() - template.getEDID().length() + prefixLen;
//        if (suffixLen > 0) {
//            suffix = baseName.substring(template.getEDID().length() + prefixLen);
//        }
//        String ret = prefix + name + suffix;
//        return ret;

        String name = newArmor.getEDID();
        String baseName = armor.getEDID();
        String templateName;
        String ret = "";
        ARMO template = (ARMO) m.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
        if (template != null) {
            templateName = template.getEDID();
            if (baseName.contains(templateName)) {
                ret = baseName.replace(templateName, name);
            } else {
                String lcseq = lcs(baseName, templateName);
                if (baseName.contains(lcseq)) {
                    ret = baseName.replace(lcseq, name);
                } else {
                    String gcs = longestCommonSubstring(baseName, templateName);
                    ret = baseName.replace(gcs, name);
                }
            }
        }

        return ret;
    }

    static String generateArmorName(ARMO newArmor, ARMO armor, Mod m) {
//        String name = newArmor.getName();
//        String baseName = armor.getName();
//        String prefix = "";
//        String suffix = "";
//        ARMO template = (ARMO) m.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
//        SPGlobal.log(armor.getName(), template.getName());
//        int prefixLen = baseName.indexOf(template.getName());
//        SPGlobal.log(name, "" + prefixLen);
//        if (prefixLen > 0) {
//            prefix = baseName.substring(0, prefixLen);
//        }
//        int suffixLen = baseName.length() - template.getName().length() + prefixLen;
//        if (suffixLen > 0) {
//            suffix = baseName.substring(template.getName().length() + prefixLen);
//        }
//        String ret = prefix + name + suffix;
//        return ret;

        String name = newArmor.getName();
        String baseName = armor.getName();
        String templateName;
        String ret = "";
        ARMO template = (ARMO) m.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
        if (template != null) {
            templateName = template.getName();
            if (baseName.contains(templateName)) {
                ret = baseName.replace(templateName, name);
            } else {
                String lcseq = lcs(baseName, templateName);
                if (baseName.contains(lcseq)) {
                    ret = baseName.replace(lcseq, name);
                } else {
                    String gcs = longestCommonSubstring(baseName, templateName);
                    ret = baseName.replace(gcs, name);
                }
            }
        }

        return ret;
    }

    static KYWD armorHasAnyKeyword(ARMO rec, FLST f, Mod m) {
        ArrayList<FormID> a = f.getFormIDEntries();
        KYWD hasKey = null;
        for (int i = 0; i < a.size(); i++) {
            FormID temp = (FormID) a.get(i);
            KYWD armorKey = (KYWD) m.getMajor(temp, GRUP_TYPE.KYWD);
            if (armorHasKeyword(rec, armorKey, m)) {
                hasKey = armorKey;
                continue;
            }
        }
        //SPGlobal.log("HasAnyKeyword", rec.toString() + " " + hasKey);
        return hasKey;
    }

    static boolean armorHasKeyword(ARMO rec, KYWD varKey, Mod m) {
        ArrayList<FormID> a;
        boolean hasKey = false;
        ARMO replace = rec;
        FormID tmp = replace.getTemplate();
        //SPGlobal.log("hasKeyword", varKey.getEDID() + " " + replace.getEDID() + " " + tmp.getFormStr());
        if (!tmp.isNull()) {
            replace = (ARMO) m.getMajor(tmp, GRUP_TYPE.ARMO);
        }
        //SPGlobal.log(replace.getEDID(), varKey.getEDID());

        KeywordSet k;
        try {
            k = replace.getKeywordSet();
        } catch (Exception e) {
            String error = "Armor: " + rec.getEDID() + ", from " + rec.getFormMaster().toString() + ", has unresolvable template entry: " + tmp.toString();
            SPGlobal.logSpecial(LeveledListInjector.lk.err, "Bad Data", error);
            SPGlobal.logError("ERROR!", error);
            throw (e);
        }
            a = k.getKeywordRefs();
            for (FormID temp : a) {
                KYWD refKey = (KYWD) m.getMajor(temp, GRUP_TYPE.KYWD);
                //SPGlobal.log("formid", temp.toString());
                //SPGlobal.log("KYWD compare", refKey.getEDID() + " " + varKey.getEDID() + " " + (varKey.equals(refKey)));
                if (varKey.equals(refKey)) {
                    hasKey = true;
                }
            }
        return hasKey;
    }

    static private void InsertArmorVariants(LVLI list, FormID base) {
        ArrayList<LeveledEntry> listEntries = list.getEntries();
        ArrayList<FormID> forms = new ArrayList<>(0);
        for (LeveledEntry e : listEntries) {
            FormID f = e.getForm();
            forms.add(f);
        }
        for (ArrayList a : armorVariants) {
            if (a.contains(base)) {
                for (int i = 0; i < a.size(); i++) {
                    FormID f = (FormID) a.get(i);
                    if (!forms.contains(f)) {
                        list.addEntry(new LeveledEntry(f, 1, 1));
                    }
                }
            }
        }
    }

    static private void linkArmorSet(LVLI llist, ArrayList<FormID> set, Mod merger, Mod patch) {
        String eid = "DienesLVLI" + getSetName(set) + "level1";
        LVLI r = (LVLI) merger.getMajor(eid, GRUP_TYPE.LVLI);
        FormID f = new FormID("107347", "Skyrim.esm");
        MajorRecord glist = merger.getMajor(f, GRUP_TYPE.LVLI);

        if (r == null) {
            LVLI setList = (LVLI) patch.makeCopy(glist, eid);
            for (int index = 0; index < set.size(); index++) {
                FormID item = set.get(index);
                ARMO temp = (ARMO) merger.getMajor(item, GRUP_TYPE.ARMO);
                if (temp != null) {
                    setList.addEntry(item, 1, 1);
                    for (int i = 0; i < llist.numEntries(); i++) {
                        FormID tempForm = llist.getEntry(i).getForm();
                        if (item.equals(tempForm)) {
                            llist.removeEntry(i);
                            continue;
                        }
                    }

                    index = index - 1;
                }
            }
            merger.addRecord(setList);
            llist.addEntry(setList.getForm(), 1, 1);
            //matchingSetVariants.add(setList.getForm());

        } else {
            for (int index = 0; index < set.size(); index++) {
                FormID item = set.get(index);
                for (int i = 0; i < llist.numEntries(); i++) {
                    FormID tempForm = llist.getEntry(i).getForm();
                    if (item.equals(tempForm)) {
                        llist.removeEntry(i);
                        continue;
                    }
                }

                index = index - 1;
            }
            llist.addEntry(r.getForm(), 1, 1);

        }
    }

    private static boolean hasVariant(ARMO base) {
        boolean ret = false;
        for (ArrayList<FormID> vars : armorVariants) {
            //SPGlobal.log("hasVariant", base.getForm() + " " + vars.size());
            boolean contains = vars.contains(base.getForm());
            if ((contains) && ((vars.size() > 1) || LeveledListInjector.listify)) {
//            if (vars.contains(base.getForm())) {
                ret = true;
            }
        }

        return ret;
    }

    public static void modLVLIArmors(Mod merger, Mod patch) {
        for (LVLI llist : merger.getLeveledItems()) {
            String lname = llist.getEDID();
            if (lname.contains("DienesLVLI")) {
                ARMO armor = (ARMO) merger.getMajor(llist.getEntry(0).getForm(), GRUP_TYPE.ARMO);
                if (armor != null) {
                    if (hasVariant(armor)) {
                        InsertArmorVariants(llist, armor.getForm());
                        patch.addRecord(llist);
                    }
                }
            }
        }
    }

    private static String longestCommonSubstring(String S1, String S2) {
        int Start = 0;
        int Max = 0;
        for (int i = 0; i < S1.length(); i++) {
            for (int j = 0; j < S2.length(); j++) {
                int x = 0;
                while (S1.charAt(i + x) == S2.charAt(j + x)) {
                    x++;
                    if (((i + x) >= S1.length()) || ((j + x) >= S2.length())) {
                        break;
                    }
                }
                if (x > Max) {
                    Max = x;
                    Start = i;
                }
            }
        }
        return S1.substring(Start, (Start + Max));
    }

    public static String lcs(String a, String b) {
        int[][] lengths = new int[a.length() + 1][b.length() + 1];

        // row 0 and column 0 are initialized to 0 already

        for (int i = 0; i < a.length(); i++) {
            for (int j = 0; j < b.length(); j++) {
                if (a.charAt(i) == b.charAt(j)) {
                    lengths[i + 1][j + 1] = lengths[i][j] + 1;
                } else {
                    lengths[i + 1][j + 1] = Math.max(lengths[i + 1][j], lengths[i][j + 1]);
                }
            }
        }

        // read the substring out from the matrix
        StringBuffer sb = new StringBuffer();
        for (int x = a.length(), y = b.length(); x != 0 && y != 0;) {
            if (lengths[x][y] == lengths[x - 1][y]) {
                x--;
            } else if (lengths[x][y] == lengths[x][y - 1]) {
                y--;
            } else {
                assert a.charAt(x - 1) == b.charAt(y - 1);
                sb.append(a.charAt(x - 1));
                x--;
                y--;
            }
        }

        return sb.reverse().toString();
    }

    static void setupSets(Mod merger, Mod patch) {
        for (ARMO armor : merger.getArmors()) {
            if (armor.getTemplate().equals(FormID.NULL)) {
                KYWD outfitKey = hasKeyStartsWith(armor, "dienes_outfit", merger);
                if (outfitKey != null) {
                    boolean found = false;
                    for (Pair<KYWD, ArrayList<ARMO>> p : matchingSets) {
                        if (p.getBase().equals(outfitKey)) {
                            if (!p.getVar().contains(armor)) {
                                p.getVar().add(armor);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found == false) {
                        Pair<KYWD, ArrayList<ARMO>> q = new Pair<>(outfitKey, new ArrayList<ARMO>(0));
                        q.getVar().add(armor);
                        matchingSets.add(q);
                    }
                }
            }
        }
    }

    static KYWD hasKeyStartsWith(ARMO armor, String start, Mod merger) {
        KYWD ret = null;

        ArrayList<FormID> a;

        ARMO replace = armor;
        FormID tmp = replace.getTemplate();
        //SPGlobal.log("hasKeyword", varKey.getEDID() + " " + replace.getEDID() + " " + tmp.getFormStr());
        if (!tmp.isNull()) {
            replace = (ARMO) merger.getMajor(tmp, GRUP_TYPE.ARMO);
        }
        //SPGlobal.log(replace.getEDID(), varKey.getEDID());
        KeywordSet k = replace.getKeywordSet();
        a = k.getKeywordRefs();
        for (FormID temp : a) {
            KYWD refKey = (KYWD) merger.getMajor(temp, GRUP_TYPE.KYWD);
            //SPGlobal.log("formid", temp.toString());
            //SPGlobal.log("KYWD compare", refKey.getEDID() + " " + varKey.getEDID() + " " + (varKey.equals(refKey)));

            if (refKey.getEDID().startsWith(start)) {
                ret = refKey;
            }
        }

        return ret;
    }

    static ArrayList<ARMO> getAllWithKey(KYWD key, ArrayList<FormID> a, Mod merger) {
        ArrayList<ARMO> ret = new ArrayList<>(0);
        for (FormID f : a) {
            ARMO arm = (ARMO) merger.getMajor(f, GRUP_TYPE.ARMO);
            if (arm != null) {
                if (armorHasKeyword(arm, key, merger)) {
                    ret.add(arm);
                }
            }
        }
        return ret;
    }

    static ArrayList<ARMO> getAllWithKeyARMO(KYWD key, ArrayList<ARMO> a, Mod merger) {
        ArrayList<ARMO> ret = new ArrayList<>(0);
        for (ARMO arm : a) {

            if (armorHasKeyword(arm, key, merger)) {
                ret.add(arm);
            }

        }
        return ret;
    }

    static String getNameFromArrayWithKey(ArrayList<ARMO> a, KYWD k, Mod merger) {
        String ret = null;
        if (k.getEDID().contains("dienes_outfit")) {
            ret = "DienesLVLIOutfit" + k.getEDID().substring(13);
        } else {
            ret = "DienesLVLIOutfit" + k.getEDID();
        }
        boolean h = false;
        for (ARMO arm : a) {
            if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
                    || arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
                    || arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
                h = true;
                break;
            }
        }
        boolean c = false;
        for (ARMO arm : a) {
            if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
                c = true;
                break;
            }
        }
        boolean g = false;
        for (ARMO arm : a) {
            if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
                g = true;
                break;
            }
        }
        boolean b = false;
        for (ARMO arm : a) {
            if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
                b = true;
                break;
            }
        }
        KYWD shield = (KYWD) merger.getMajor("ArmorShield", GRUP_TYPE.KYWD);
        boolean s = false;
        for (ARMO arm : a) {
            if (armorHasKeyword(arm, shield, merger)) {
                s = true;
                break;
            }
        }
        if (h) {
            ret = ret + "H";
        }
        if (c) {
            ret = ret + "C";
        }
        if (g) {
            ret = ret + "G";
        }
        if (b) {
            ret = ret + "B";
        }
        if (s) {
            ret = ret + "S";
        }

        return ret;
    }

    static void addArmorFromArray(LVLI list, ArrayList<ARMO> a, Mod merger, Mod patch) {
//        FormID f = new FormID("107347", "Skyrim.esm");
//        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);

        ArrayList<ARMO> h = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD), a, merger);
        ArrayList<ARMO> c = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorCuirass", GRUP_TYPE.KYWD), a, merger);
        ArrayList<ARMO> g = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorGauntlets", GRUP_TYPE.KYWD), a, merger);
        ArrayList<ARMO> b = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorBoots", GRUP_TYPE.KYWD), a, merger);
        ArrayList<ARMO> s = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorShield", GRUP_TYPE.KYWD), a, merger);

        if (h.size() > 1) {
            String name = "DienesLVLI_" + hasKeyStartsWith(h.get(0), "dienes_outfit", merger).getEDID() + "HelmetsSublist";
            LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
            if (subList == null) {
                subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
            }
            if (subList != null) {
                list.addEntry(subList.getForm(), 1, 1);
            } else {
                subList = new LVLI(name);//(LVLI) patch.makeCopy(glist, name);
                subList.set(LeveledRecord.LVLFlag.UseAll, false);
                for (ARMO arm : h) {
                    subList.addEntry(arm.getForm(), 1, 1);
                }
                patch.addRecord(subList);
            }
        } else if (h.size() == 1) {
            list.addEntry(h.get(0).getForm(), 1, 1);
        }
        if (c.size() > 1) {
            String name = "DienesLVLI_" + hasKeyStartsWith(c.get(0), "dienes_outfit", merger).getEDID() + "CuirassesSublist";
            LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
            if (subList == null) {
                subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
            }
            if (subList != null) {
                list.addEntry(subList.getForm(), 1, 1);
            } else {
                subList = new LVLI(name); //(LVLI) patch.makeCopy(glist, name);
                subList.set(LeveledRecord.LVLFlag.UseAll, false);
                for (ARMO arm : c) {
                    subList.addEntry(arm.getForm(), 1, 1);
                }
                patch.addRecord(subList);
            }
        } else if (c.size() == 1) {
            list.addEntry(c.get(0).getForm(), 1, 1);
        }
        if (g.size() > 1) {
            String name = "DienesLVLI_" + hasKeyStartsWith(g.get(0), "dienes_outfit", merger).getEDID() + "GauntletsSublist";
            LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
            if (subList == null) {
                subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
            }
            if (subList != null) {
                list.addEntry(subList.getForm(), 1, 1);
            } else {
                subList = new LVLI(name); //(LVLI) patch.makeCopy(glist, name);
                subList.set(LeveledRecord.LVLFlag.UseAll, false);
                for (ARMO arm : g) {
                    subList.addEntry(arm.getForm(), 1, 1);
                }
                patch.addRecord(subList);
            }
        } else if (g.size() == 1) {
            list.addEntry(g.get(0).getForm(), 1, 1);
        }
        if (b.size() > 1) {
            String name = "DienesLVLI_" + hasKeyStartsWith(b.get(0), "dienes_outfit", merger).getEDID() + "BootsSublist";
            LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
            if (subList == null) {
                subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
            }
            if (subList != null) {
                list.addEntry(subList.getForm(), 1, 1);
            } else {
                subList = new LVLI(name); //(LVLI) patch.makeCopy(glist, name);
                subList.set(LeveledRecord.LVLFlag.UseAll, false);
                for (ARMO arm : b) {
                    subList.addEntry(arm.getForm(), 1, 1);
                }
                patch.addRecord(subList);
            }
        } else if (b.size() == 1) {
            list.addEntry(b.get(0).getForm(), 1, 1);
        }
        if (s.size() > 1) {
            String name = "DienesLVLI_" + hasKeyStartsWith(s.get(0), "dienes_outfit", merger).getEDID() + "ShieldsSublist";
            LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
            if (subList == null) {
                subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
            }
            if (subList != null) {
                list.addEntry(subList.getForm(), 1, 1);
            } else {
                subList = new LVLI(name); //(LVLI) patch.makeCopy(glist, name);
                subList.set(LeveledRecord.LVLFlag.UseAll, false);
                for (ARMO arm : s) {
                    subList.addEntry(arm.getForm(), 1, 1);
                }
                patch.addRecord(subList);
            }
        } else if (s.size() == 1) {
            list.addEntry(s.get(0).getForm(), 1, 1);
        }

    }

    static void addAlternateSets(LVLI list, ArrayList<ARMO> a, Mod merger, Mod patch) {
//        FormID f = new FormID("107347", "Skyrim.esm");
        //SPGlobal.log("outfits glist", f.toString());
//        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);

        KYWD k = null;
        ArrayList<Pair<KYWD, ArrayList<ARMO>>> varSets = new ArrayList<>(0);
        for (ARMO arm : a) {
            k = hasKeyStartsWith(arm, "dienes_outfit", merger);
            for (Pair<KYWD, ArrayList<ARMO>> p1 : matchingSets) {

                boolean key = k.equals(p1.getBase());

                if (key) {
                    for (ARMO armor : p1.getVar()) {
                        boolean passed = true;
                        for (skyproc.genenums.FirstPersonFlags c : skyproc.genenums.FirstPersonFlags.values()) {
                            boolean armorFlag = armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
                            boolean formFlag = arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);

                            boolean flagMatch = (armorFlag == formFlag);

                            if (flagMatch == false) {
                                passed = false;
                            }
                        }
                        if (!passed) {
                            KYWD helm = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
                            if (armorHasKeyword(arm, helm, merger) && armorHasKeyword(armor, helm, merger)) {
                                passed = true;
                            }
                        }
                        if (passed) {
                            boolean found = false;
                            KYWD slotKey = getSlotKYWD(armor, merger);
                            if (slotKey == null) {
                                int test = 1;
                            } else {
                                for (Pair<KYWD, ArrayList<ARMO>> p : varSets) {
                                    if (p.getBase().equals(slotKey)) {
                                        ArrayList<ARMO> q = p.getVar();
                                        if (!q.contains(armor)) {
                                            q.add(armor);
                                        }
                                        found = true;
                                        break;
                                    }
                                }
                                if (found == false) {
                                    Pair<KYWD, ArrayList<ARMO>> p = new Pair(slotKey, new ArrayList<ARMO>(0));
                                    p.getVar().add(armor);
                                    varSets.add(p);
                                }
                            }
                        }
                    }
                }
            }
        }

        String bits = getBitsFromArray(a, merger);
        for (char c : bits.toCharArray()) {
            for (Pair<KYWD, ArrayList<ARMO>> p : varSets) {

                if (arrayHasBits(p.getVar(), String.valueOf(c), merger)) {
                    if (p.getVar().size() > 1) {
                        String lvliName = getNameFromArrayWithKey(p.getVar(), k, merger) + "variants";
                        LVLI list2 = (LVLI) patch.getMajor(lvliName, GRUP_TYPE.LVLI);
                        if (list2 != null) {
                            list.addEntry(list2.getForm(), 1, 1);
                            patch.addRecord(list);
                        } else {
                            LVLI subList = new LVLI(lvliName); //(LVLI) patch.makeCopy(glist, lvliName);
                            subList.set(LeveledRecord.LVLFlag.UseAll, false);
                            addArmorByBit(subList, p.getVar(), String.valueOf(c), merger);
                            patch.addRecord(subList);
                            list.addEntry(subList.getForm(), 1, 1);
                            patch.addRecord(list);
                        }
                    } else {
                        boolean found = false;
                        for (LeveledEntry entry : list) {
                            if (entry.getForm().equals(p.getVar().get(0).getForm())) {
                                found = true;
                            }
                        }
                        if (!found) {
                            list.addEntry(p.getVar().get(0).getForm(), 1, 1);
                        }
                    }
                }
            }
        }
    }

    static void addAlternateOutfits(LVLI list, ArrayList<ARMO> a, Mod merger, Mod patch) {
//        FormID f = new FormID("107347", "Skyrim.esm");
        //SPGlobal.log("outfits glist", f.toString());
//        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);

        KYWD k = null;
        ArrayList<Pair<KYWD, ArrayList<ARMO>>> varSets = new ArrayList<>(0);
        for (ARMO arm : a) {
            k = hasKeyStartsWith(arm, "LLI_BASE", merger);
            boolean notBase = false;
            if (k == null) {
                k = hasKeyStartsWith(arm, "dienes_outfit", merger);
                notBase = true;
            }

            for (Pair<KYWD, ArrayList<ARMO>> p1 : matchingSets) {

                boolean key = false;
                if (notBase) {
                    key = p1.getBase().equals(k);
                } else {
                    KYWD ret = null;
                    for (Pair p : armorMatches) {
                        KYWD var = (KYWD) p.getBase();
                        //SPGlobal.log("getBaseArmor", k.getEDID() + " " + var.getEDID() + " " + var.equals(k));
                        if (var.equals(k)) {
                            ret = (KYWD) p.getVar();
                        }
                    }
                    key = armorHasKeyword(p1.getVar().get(0), ret, merger) || armorHasKeyword(p1.getVar().get(0), k, merger);
                }

                if (key) {
                    for (ARMO armor : p1.getVar()) {
                        boolean passed = true;
                        for (skyproc.genenums.FirstPersonFlags c : skyproc.genenums.FirstPersonFlags.values()) {
                            boolean armorFlag = armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
                            boolean formFlag = arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);

                            boolean flagMatch = (armorFlag == formFlag);

                            if (flagMatch == false) {
                                passed = false;
                            }
                        }
                        if (!passed) {
                            KYWD helm = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
                            if (armorHasKeyword(arm, helm, merger) && armorHasKeyword(armor, helm, merger)) {
                                passed = true;
                            }
                        }
                        if (passed) {
                            boolean found = false;
                            KYWD slotKey = getSlotKYWD(armor, merger);
                            if (slotKey == null) {
                                int test = 1;
                            } else {
                                for (Pair<KYWD, ArrayList<ARMO>> p : varSets) {
                                    if (p.getBase().equals(slotKey)) {
                                        ArrayList<ARMO> q = p.getVar();
                                        if (!q.contains(armor)) {
                                            q.add(armor);
                                        }
                                        found = true;
                                        break;
                                    }
                                }
                                if (found == false) {
                                    Pair<KYWD, ArrayList<ARMO>> p = new Pair(slotKey, new ArrayList<ARMO>(0));
                                    p.getVar().add(armor);
                                    varSets.add(p);
                                }
                            }
                        }
                    }
                }
            }
        }

        String bits = getBitsFromArray(a, merger);
        for (char c : bits.toCharArray()) {
            for (Pair<KYWD, ArrayList<ARMO>> p : varSets) {

                if (arrayHasBits(p.getVar(), String.valueOf(c), merger)) {
                    if (p.getVar().size() > 1) {
                        String lvliName = getNameFromArrayWithKey(p.getVar(), k, merger) + "variants";
                        LVLI list2 = (LVLI) patch.getMajor(lvliName, GRUP_TYPE.LVLI);
                        if (list2 != null) {
                            list.addEntry(list2.getForm(), 1, 1);
                            patch.addRecord(list);
                        } else {
                            LVLI subList = new LVLI(lvliName); //(LVLI) patch.makeCopy(glist, lvliName);
                            addArmorByBit(subList, p.getVar(), String.valueOf(c), merger);
                            patch.addRecord(subList);
                            list.addEntry(subList.getForm(), 1, 1);
                            patch.addRecord(list);
                        }
                    } else {
                        boolean found = false;
                        for (LeveledEntry entry : list) {
                            if (entry.getForm().equals(p.getVar().get(0).getForm())) {
                                found = true;
                            }
                        }
                        if (!found) {
                            list.addEntry(p.getVar().get(0).getForm(), 1, 1);
                        }
                    }
                }
            }
        }
    }

    static void insertTieredArmors(LVLI list, String keyPrefix, String bits, Mod merger, Mod patch) {
//        FormID f = new FormID("107347", "Skyrim.esm");
//        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
        boolean changed = false;

        if (keyPrefix.contains("Boss") || keyPrefix.contains("Thalmor")) {
            list.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
        }

        for (int lev = 1; lev < 100; lev++) {
            int tier = lev / 3;
            String tierName = keyPrefix + String.valueOf(tier);
            KYWD key = (KYWD) merger.getMajor(tierName, GRUP_TYPE.KYWD);
            if (key != null) {
                ArrayList<ArrayList<ARMO>> array = getArrayOfTieredArmorSetsByKeyword(key, merger);
                String edid = "DienesLVLI_" + keyPrefix + String.valueOf(tier);
                for (ArrayList<ARMO> ar : array) {
                    if (arrayHasBits(ar, bits, merger)) {

                        LVLI subList = (LVLI) patch.getMajor(edid, GRUP_TYPE.LVLI);
                        if (subList == null) {
                            //SPGlobal.logError("LLI Error:", "Could not find LVLI " + edid);
                            subList = new LVLI(edid); //(LVLI) patch.makeCopy(glist, edid);
                            subList.set(LeveledRecord.LVLFlag.UseAll, false);
                            patch.addRecord(subList);
                        }
                        boolean change = addListIfNotLevel(list, subList, lev);
                        if (change) {
                            changed = true;
                        }
                        String setListName = "DienesLVLI_" + hasKeyStartsWith(ar.get(0), "dienes_outfit", merger).getEDID().substring(14) + bits;
                        LVLI setList = (LVLI) merger.getMajor(setListName, GRUP_TYPE.LVLI);
                        LVLI setList2 = (LVLI) patch.getMajor(setListName, GRUP_TYPE.LVLI);
                        if (setList == null) {
                            setList = setList2;
                        }
                        if (setList != null) {
                            change = addListIfNotLevel(subList, setList, 1);
                            if (change) {
                                changed = true;
                            }
                        } else {
                            LVLI set = new LVLI(setListName); //(LVLI) patch.makeCopy(glist, setListName);
                            set.set(LeveledRecord.LVLFlag.UseAll, true);
                            set.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                            set.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
                            ArrayList<ArrayList<ARMO>> abits = new ArrayList<>(0);
                            for (char c : bits.toCharArray()) {
                                abits.add(addArmorByBitToArray(ar, String.valueOf(c), merger));
                            }
                            for (ArrayList<ARMO> a : abits) {
                                addAlternateSets(set, a, merger, patch);
                            }
                            patch.addRecord(set);
                            subList.addEntry(set.getForm(), 1, 1);
                            patch.addRecord(subList);
                            changed = true;
                        }

                    }
                }
                if ((array.isEmpty()) && (edid.contentEquals("DienesLVLI_Thalmor_Tier_9"))) {
                    LVLI subList = new LVLI(edid); //(LVLI) patch.makeCopy(glist, edid);
                    subList.set(LeveledRecord.LVLFlag.UseAll, true);
                    subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                    subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
                    FormID boots = new FormID("01391a", "Skyrim.esm");
                    FormID helm = new FormID("01391d", "Skyrim.esm");
                    FormID cuirass = new FormID("01392a", "Skyrim.esm");
                    FormID gloves = new FormID("01391c", "Skyrim.esm");
                    subList.addEntry(boots, 1, 1);
                    subList.addEntry(helm, 1, 1);
                    subList.addEntry(cuirass, 1, 1);
                    subList.addEntry(gloves, 1, 1);

                    addListIfNotLevel(list, subList, lev);
                    patch.addRecord(subList);
                    changed = true;
                }
                if ((array.isEmpty()) && (edid.contentEquals("DienesLVLI_Necromancer_Tier_0"))) {
                    LVLI subList = new LVLI(edid); //(LVLI) patch.makeCopy(glist, edid);
                    subList.set(LeveledRecord.LVLFlag.UseAll, true);
                    subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                    subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
                    FormID boots = new FormID("0c36e8", "Skyrim.esm");
                    FormID robesList = new FormID("105251", "Skyrim.esm");
                    subList.addEntry(boots, 1, 1);
                    subList.addEntry(robesList, 1, 1);

                    addListIfNotLevel(list, subList, lev);
                    patch.addRecord(subList);
                    changed = true;
                }
                if ((array.isEmpty()) && (edid.contentEquals("DienesLVLI_Warlock_Tier_0"))) {
                    LVLI subList = new LVLI(edid); //(LVLI) patch.makeCopy(glist, edid);
                    subList.set(LeveledRecord.LVLFlag.UseAll, true);
                    subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                    subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
                    FormID boots = new FormID("0c5d12", "Skyrim.esm");
                    FormID robesList = new FormID("105ef9", "Skyrim.esm");
                    subList.addEntry(boots, 1, 1);
                    subList.addEntry(robesList, 1, 1);

                    addListIfNotLevel(list, subList, lev);
                    patch.addRecord(subList);
                    changed = true;
                }
            }
        }
        if (changed) {
            patch.addRecord(list);
        }
    }

    static ArrayList getArrayOfTieredArmorSetsByKeyword(KYWD key, Mod merger) {
        ArrayList<ArrayList<ARMO>> ret = new ArrayList<>(0);
        for (Pair<KYWD, ArrayList<ARMO>> p : matchingSets) {
            if (armorHasKeyword(p.getVar().get(0), key, merger)) {
                ret.add(p.getVar());
            }
        }

        return ret;
    }

    static boolean arrayHasBits(ArrayList<ARMO> ar, String bits, Mod merger) {
        boolean ret = true;
        if (bits.contains("H")) {
            boolean passed = false;
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
                        || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
                        || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
                    passed = true;
                }
            }
            if (passed == false) {
                ret = false;
            }
        }
        if (bits.contains("C")) {
            boolean passed = false;
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
                    passed = true;
                }
            }
            if (passed == false) {
                ret = false;
            }
        }
        if (bits.contains("G")) {
            boolean passed = false;
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
                    passed = true;
                }
            }
            if (passed == false) {
                ret = false;
            }
        }
        if (bits.contains("B")) {
            boolean passed = false;
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
                    passed = true;
                }
            }
            if (passed == false) {
                ret = false;
            }
        }
        if (bits.contains("S")) {
            boolean passed = false;
            for (ARMO a : ar) {
                KYWD k = hasKeyStartsWith(a, "ArmorShield", merger);
                if (k != null) {
                    passed = true;
                }
            }
            if (passed == false) {
                ret = false;
            }
        }

        return ret;
    }

    static String getBitsFromArray(ArrayList<ARMO> a, Mod merger) {
        String ret = "";
        KYWD helm = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
        boolean h = false;
        for (ARMO arm : a) {
            if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
                    || arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
                    || arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
                h = true;
                break;
            }
        }
        KYWD cuirass = (KYWD) merger.getMajor("ArmorCuirass", GRUP_TYPE.KYWD);
        boolean c = false;
        for (ARMO arm : a) {
            if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
                c = true;
                break;
            }
        }
        KYWD gauntlets = (KYWD) merger.getMajor("ArmorGauntlets", GRUP_TYPE.KYWD);
        boolean g = false;
        for (ARMO arm : a) {
            if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
                g = true;
                break;
            }
        }
        KYWD boots = (KYWD) merger.getMajor("ArmorBoots", GRUP_TYPE.KYWD);
        boolean b = false;
        for (ARMO arm : a) {
            if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
                b = true;
                break;
            }
        }
        KYWD shield = (KYWD) merger.getMajor("ArmorShield", GRUP_TYPE.KYWD);
        boolean s = false;
        for (ARMO arm : a) {
            if (armorHasKeyword(arm, shield, merger)) {
                s = true;
                break;
            }
        }
        if (h) {
            ret = ret + "H";
        }
        if (c) {
            ret = ret + "C";
        }
        if (g) {
            ret = ret + "G";
        }
        if (b) {
            ret = ret + "B";
        }
        if (s) {
            ret = ret + "S";
        }

        return ret;
    }

    static boolean addListIfNotLevel(LVLI list, LVLI subList, int level) {
        boolean added = false;
        boolean found = false;
        ArrayList<LeveledEntry> ar = list.getEntries();
        for (LeveledEntry l : ar) {
            if (l.getLevel() == level) {
                if (l.getForm().equals(subList.getForm())) {
                    found = true;
                }
            }
        }
        if (!found) {
            added = true;
            list.addEntry(subList.getForm(), level, 1);
        }
        return added;
    }

    static void addArmorByBit(LVLI set, ArrayList<ARMO> ar, String bits, Mod merger) {
        if (bits.contains("H")) {
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
                        || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
                        || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
                    set.addEntry(a.getForm(), 1, 1);
                }
            }
        }
        if (bits.contains("C")) {
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
                    set.addEntry(a.getForm(), 1, 1);
                }
            }
        }
        if (bits.contains("G")) {
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
                    set.addEntry(a.getForm(), 1, 1);
                }
            }
        }
        if (bits.contains("B")) {
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
                    set.addEntry(a.getForm(), 1, 1);
                }
            }
        }
        if (bits.contains("S")) {
            for (ARMO a : ar) {
                KYWD k = hasKeyStartsWith(a, "ArmorShield", merger);
                if (k != null) {
                    set.addEntry(a.getForm(), 1, 1);
                }
            }
        }

    }

    static ArrayList<ARMO> addArmorByBitToArray(ArrayList<ARMO> ar, String bits, Mod merger) {
        ArrayList<ARMO> ret = new ArrayList<>(0);

        if (bits.contains("H")) {
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
                        || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
                        || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
                    ret.add(a);
                }
            }
        }
        if (bits.contains("C")) {
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
                    ret.add(a);
                }
            }
        }
        if (bits.contains("G")) {
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
                    ret.add(a);
                }
            }
        }
        if (bits.contains("B")) {
            for (ARMO a : ar) {
                if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
                    ret.add(a);
                }
            }
        }
        if (bits.contains("S")) {
            for (ARMO a : ar) {
                KYWD k = hasKeyStartsWith(a, "ArmorShield", merger);
                if (k != null) {
                    ret.add(a);
                }
            }
        }

        return ret;
    }

    static boolean isTiered(String name) {
        boolean ret = false;
        ArrayList<String> names = new ArrayList<>();
        names.add("BanditArmorMeleeHeavyOutfit");
        names.add("BanditArmorMeleeHeavyNoShieldOutfit");
        names.add("BanditArmorHeavyBossOutfit");
        names.add("BanditArmorHeavyBossNoShieldOutfit");
        names.add("BanditArmorMeleeShield20Outfit");
        names.add("BanditArmorMeleeNoShieldOutfit");
        names.add("ThalmorArmorWithHelmetOutfit");
        names.add("WarlockOutfitLeveled");
        names.add("NecromancerOutfit");
        names.add("NecromancerOutfitHood50");

        for (String s : names) {
            if (name.contentEquals(s)) {
                ret = true;
            }
        }

        return ret;
    }

    static String getTierKey(String name) {
        String ret = null;

        if (name.startsWith("BanditArmorMeleeHeavyOutfit")) {
            ret = "BanditHeavy_Tier_";
        }
        if (name.startsWith("BanditArmorMeleeShield20Outfit")) {
            ret = "BanditLight_Tier_";
        }
        if (name.startsWith("BanditArmorMeleeHeavyNoShieldOutfit")) {
            ret = "BanditHeavy_Tier_";
        }
        if (name.startsWith("BanditArmorHeavyBossOutfit")) {
            ret = "BanditBoss_Tier_";
        }
        if (name.startsWith("BanditArmorHeavyBossNoShieldOutfit")) {
            ret = "BanditBoss_Tier_";
        }
        if (name.startsWith("BanditArmorMeleeNoShieldOutfit")) {
            ret = "BanditLight_Tier_";
        }
        if (name.startsWith("ThalmorArmorWithHelmetOutfit")) {
            ret = "Thalmor_Tier_";
        }
        if (name.startsWith("WarlockOutfitLeveled")) {
            ret = "Warlock_Tier_";
        }
        if (name.contentEquals("NecromancerOutfit")) {
            ret = "Necromancer_Tier_";
        }
        if (name.contentEquals("NecromancerOutfitHood50")) {
            ret = "Necromancer_Tier_";
        }

        return ret;
    }

    static String getBits(String name) {
        String ret = null;
        if (name.startsWith("BanditArmorMeleeHeavyOutfit")) {
            ret = "HCGB";
        }
        if (name.startsWith("BanditArmorMeleeHeavyNoShieldOutfit")) {
            ret = "HCGB";
        }
        if (name.startsWith("BanditArmorHeavyBossOutfit")) {
            ret = "HCGB";
        }
        if (name.startsWith("BanditArmorHeavyBossNoShieldOutfit")) {
            ret = "HCGB";
        }
        if (name.startsWith("BanditArmorMeleeShield20Outfit")) {
            ret = "HCBG";
        }
        if (name.startsWith("BanditArmorMeleeNoShieldOutfit")) {
            ret = "CBG";
        }
        if (name.startsWith("ThalmorArmorWithHelmetOutfit")) {
            ret = "HCBG";
        }
        if (name.startsWith("WarlockOutfitLeveled")) {
            ret = "HCBG";
        }
        if (name.contentEquals("NecromancerOutfit")) {
            ret = "HCGB";
        }
        if (name.contentEquals("NecromancerOutfitHood50")) {
            ret = "HCGB";
        }


        return ret;
    }

    static boolean needsShield(String name) {
        boolean ret = false;
        if (name.startsWith("BanditArmorMeleeHeavyOutfit")) {
            ret = true;
        }
        if (name.startsWith("BanditArmorHeavyBossOutfit")) {
            ret = true;
        }
        if (name.startsWith("BanditArmorMeleeShield20Outfit")) {
            ret = true;
        }

        return ret;
    }

    static FormID shieldForm(String name) {
        FormID ret = FormID.NULL;
        if (name.startsWith("BanditArmorMeleeHeavyOutfit")) {
            ret = new FormID("039d2d", "Skyrim.esm");
        }
        if (name.startsWith("BanditArmorHeavyBossOutfit")) {
            ret = new FormID("03df22", "Skyrim.esm");
        }
        if (name.startsWith("BanditArmorMeleeShield20Outfit")) {
            ret = new FormID("0c0196", "Skyrim.esm");
        }

        return ret;
    }

    static KYWD getSlotKYWD(ARMO armor, Mod merger) {
        KYWD ret = null;
        if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
                || armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
                || armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
            ret = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
        }
        if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
            ret = (KYWD) merger.getMajor("ArmorCuirass", GRUP_TYPE.KYWD);
        }
        if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
            ret = (KYWD) merger.getMajor("ArmorGauntlets", GRUP_TYPE.KYWD);
        }
        if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
            ret = (KYWD) merger.getMajor("ArmorBoots", GRUP_TYPE.KYWD);
        }
        if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.SHIELD)) {
            ret = (KYWD) merger.getMajor("ArmorShield", GRUP_TYPE.KYWD);
        }
        if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.RING)) {
            ret = (KYWD) merger.getMajor("ClothingRing", GRUP_TYPE.KYWD);
        }
        if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.AMULET)) {
            ret = (KYWD) merger.getMajor("ClothingNecklace", GRUP_TYPE.KYWD);
        }

        return ret;
    }
}
