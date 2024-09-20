package com.wylxbot.wylx.Commands.DND.Util;

import com.wylxbot.wylx.Wylx;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.*;

public class SpellBuilder {
    private final static int LIST_MAX_ERRORS = 100;

    private final HashMap<String, Spell> spellMap = new HashMap<>();

    private static final SpellBuilder INSTANCE = new SpellBuilder();

    public static SpellBuilder getInstance() {
        return INSTANCE;
    }

    private SpellBuilder() {
        // Split the csv into lines, should be one spell per line
//        String[] csvLines = ((String) Wylx.getInstance().getDb().getCmdStats().getSetting(GlobalIdentifiers.DND_Spells_CSV)).split("\n");
//        // Add each spell to the spell map
//        for(String line : csvLines){
//            // 0: Name, 1: Source, 2: Level, 3: School, 4: Casting Time, 5: Range, 6: Components, 7: Duration, 8: Description, 9: Spell List(s)
//            String[] lineElements = line.split(",");
//
//            String name = capitalizeWords(lineElements[0]);
//            String source = capitalizeWords(lineElements[1]);
//            int level = Integer.parseInt(lineElements[2]);
//            String school = StringUtils.capitalize(lineElements[3]);
//            String castingTime = StringUtils.capitalize(lineElements[4]);
//            String range = lineElements[5];
//            String components = capitalizeWords(lineElements[6]);
//            String duration = StringUtils.capitalize(lineElements[7]);
//            String[] classSpellLists = capitalizeWords(lineElements[8]).split(" ");
//            String description = lineElements[9].replaceAll(";\\|;", ",");
//
//            spellMap.put(name.toLowerCase().replaceAll(" ua", ""), new Spell(name, source, level, school, castingTime, range, components, duration, description, classSpellLists));
//        }
    }

    public String capitalizeSpellName(String input){
        return capitalizeWords(input);
    }

    private String capitalizeWords(String input) {
        StringBuilder output = new StringBuilder();
        for(String section : input.split(" ")) {
            output.append(StringUtils.capitalize(section)).append(" ");
        }
        return output.substring(0, output.length() - 1);
    }

    public String[] getCloseSpellNames(String spellName){
        LinkedList<String> results = new LinkedList<>(Arrays.asList(new String[5]));
        LinkedList<Integer> distances = new LinkedList<>(Arrays.asList(LIST_MAX_ERRORS, LIST_MAX_ERRORS, LIST_MAX_ERRORS, LIST_MAX_ERRORS, LIST_MAX_ERRORS));

        for(String key : spellMap.keySet()){
            int distance = StringUtils.getLevenshteinDistance(key, spellName);
            Iterator<Integer> distanceIterator = distances.iterator();
            for(int i = 0; i < 5; i++){
                if(distanceIterator.next() > distance){
                    results.add(i, key);
                    distances.add(i, distance);
                    break;
                }
            }
        }

        return results.subList(0, 5).toArray(new String[5]);
    }

    public Spell getSpell(String spellName) {
        return spellMap.get(spellName);
    }

    public Color getSchoolColor(String school){
        return switch (school) {
            case "Conjuration" -> Color.PINK;
            case "Necromancy" -> Color.BLACK;
            case "Evocation" -> Color.CYAN;
            case "Abjuration" -> Color.ORANGE;
            case "Transmutation" -> Color.RED;
            case "Divination" -> Color.MAGENTA;
            case "Enchantment" -> Color.GREEN;
            case "Illusion" -> Color.BLUE;
            default -> Color.DARK_GRAY;
        };
    }
}
