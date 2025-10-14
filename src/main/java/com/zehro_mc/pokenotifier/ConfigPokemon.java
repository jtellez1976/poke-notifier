/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines the default lists of Pokémon for each rarity category.
 * This class is serialized to config-pokemon.json.
 */
public class ConfigPokemon {

    public String[] _instructions = new String[]{
            "Poke Notifier Configuration - Pokemon Lists",
            "This file defines which Pokémon belong to which rarity category.",
            "Pokémon names should be in lowercase."
    };

    public Set<String> LEGENDARY = new HashSet<>();
    public Set<String> MYTHICAL = new HashSet<>();
    public Set<String> ULTRA_BEAST = new HashSet<>();
    public Set<String> PARADOX = new HashSet<>();
    public Set<String> ULTRA_RARE = new HashSet<>();
    public Set<String> RARE = new HashSet<>();

    public ConfigPokemon() {
        // Populate the lists with default values in the constructor.
        LEGENDARY.addAll(Arrays.asList(
                "articuno", "zapdos", "moltres", "mewtwo", "raikou", "entei", "suicune", "lugia", "hooh", "regirock",
                "regice", "registeel", "latias", "latios", "kyogre", "groudon", "rayquaza", "uxie", "mesprit", "azelf",
                "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "cobalion", "terrakion", "virizion",
                "tornadus", "thundurus", "reshiram", "zekrom", "landorus", "kyurem", "xerneas", "yveltal", "zygarde",
                "silvally", "tapukoko", "tapulele", "tapubulu", "tapufini", "cosmog", "cosmoem", "solgaleo", "lunala",
                "necrozma", "zacian", "zamazenta", "eternatus", "kubfu", "urshifu", "regieleki", "regidrago", "glastrier",
                "spectrier", "calyrex", "enamorus", "wochien", "chienpao", "tinglu", "chiyu", "koraidon", "miraidon",
                "okidogi", "munkidori", "fezandipiti", "ogerpon", "terapagos"
        ));
        MYTHICAL.addAll(Arrays.asList(
                "mew", "celebi", "jirachi", "deoxys", "phione", "manaphy", "darkrai", "shaymin", "arceus", "victini", "sirfetchd",
                "keldeo", "meloetta", "genesect", "diancie", "hoopa", "volcanion", "magearna", "marshadow", "zeraora",
                "meltan", "melmetal", "zarude", "pecharunt"
        ));
        ULTRA_BEAST.addAll(Arrays.asList(
                "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord", "poipole",
                "naganadel", "stakataka", "blacephalon"
        ));
        PARADOX.addAll(Arrays.asList(
                "greattusk", "screamtail", "brutebonnet", "fluttermane", "slitherwing", "sandyshocks",
                "roaringmoon", "walkingwake", "gougingfire", "ragingbolt", "irontreads", "ironbundle",
                "ironhands", "ironjugulis", "ironmoth", "ironthorns", "ironvaliant", "ironleaves",
                "ironboulder", "ironcrown"
        ));
        ULTRA_RARE.addAll(Arrays.asList(
                "bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "charizard", "squirtle", "wartortle",
                "blastoise", "ditto", "espeon", "umbreon", "chikorita", "bayleef", "meganium", "cyndaquil", "quilava",
                "typhlosion", "totodile", "croconaw", "feraligatr", "treecko", "grovyle", "sceptile", "torchic",
                "combusken", "blaziken", "mudkip", "marshtomp", "swampert", "turtwig", "grotle", "torterra",
                "chimchar", "monferno", "infernape", "piplup", "prinplup", "empoleon", "leafeon", "glaceon", "snivy",
                "servine", "serperior", "tepig", "pignite", "emboar", "oshawott", "dewott", "samurott", "pidove",
                "tranquill", "unfezant", "sewaddle", "swadloon", "leavanny", "tirtouga", "carracosta", "archen",
                "archeops", "cinccino", "gothitelle", "reuniclus", "vanilluxe", "chespin", "quilladin", "chesnaught",
                "fennekin", "braixen", "delphox", "froakie", "frogadier", "greninja", "sylveon", "rowlet", "dartrix",
                "decidueye", "litten", "torracat", "incineroar", "popplio", "brionne", "primarina", "grookey",
                "thwackey", "rillaboom", "scorbunny", "raboot", "cinderace", "sobble", "drizzile", "inteleon",
                "dracozolt", "arctozolt", "dracovish", "arctovish", "sprigatito", "floragato", "meowscarada",
                "fuecoco", "crocalor", "skeledirge", "quaxly", "quaxwell", "quaquaval", "dudunsparce", "kingambit",
                "baxcalibur", "archaludon", "hydrapple"
        ));
        RARE.addAll(Arrays.asList(
                "kadabra", "alakazam", "chansey", "kangaskhan", "scyther", "pinsir", "vaporeon", "jolteon", "flareon",
                "porygon", "omanyte", "omastar", "kabuto", "kabutops", "aerodactyl", "snorlax", "dratini", "dragonair",
                "dragonite", "wobbuffet", "dunsparce", "heracross", "skarmory", "smeargle", "blissey", "larvitar", "mrrime",
                "pupitar", "tyranitar", "ralts", "kirlia", "gardevoir", "sableye", "trapinch", "vibrava", "flygon",
                "absol", "wynaut", "clamperl", "huntail", "gorebyss", "bronzor", "bronzong", "happiny", "spiritomb",
                "munchlax", "lucario", "drapion", "toxicroak", "gallade", "froslass", "simisage", "simisear",
                "simipour", "excadrill", "audino", "throh", "sawk", "garbodor", "minccino", "gothorita", "solosis",
                "duosion", "vanillish", "axew", "fraxure", "haxorus", "mienshao", "druddigon", "deino", "zweilous",
                "hydreigon", "larvesta", "volcarona", "meowstic", "slurpuff", "malamar", "tyrunt", "tyrantrum", "mrmime",
                "amaura", "aurorus", "hawlucha", "goomy", "sliggoo", "goodra", "oranguru", "passimian", "mimikyu", "riolu",
                "drampa", "orbeetle", "drednaw", "coalossal", "applin", "flapple", "appletun", "sandaconda", "toxtricity",
                "indeedee", "frigibax", "arctibax", "gholdengo", "dipplin", "poltchageist", "sinistcha", "lokix", "pawmo",
                "pawmot", "charcadet", "armarouge", "ceruledge", "rabsca", "cyclizar", "orthworm", "greavard",
                "houndstone", "dondozo", "tatsugiri"
        ));
    }
}