package com.zehro_mc.pokenotifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConfigPokemon {

    public String[] _instructions = new String[]{
            "Poke Notifier Configuration - Pokemon Lists",
            "This file defines which Pokémon belong to which rarity category.",
            "Pokémon names should be in lowercase.",
            " "
    };

    // Inicializamos los Sets de esta forma para asegurar la compatibilidad con Gson.
    public Set<String> LEGENDARY = new HashSet<>();
    public Set<String> MYTHICAL = new HashSet<>();
    public Set<String> ULTRA_BEAST = new HashSet<>();
    public Set<String> PARADOX = new HashSet<>();
    public Set<String> ULTRA_RARE = new HashSet<>();
    public Set<String> RARE = new HashSet<>();

    public ConfigPokemon() {
        // Populamos las listas con los valores por defecto en el constructor.
        // Las listas por defecto ahora están vacías para que el usuario las configure.
        LEGENDARY.addAll(Arrays.asList(
                "articuno", "zapdos", "moltres", "mewtwo", "raikou", "entei", "suicune", "lugia", "ho-oh", "regirock",
                "regice", "registeel", "latias", "latios", "kyogre", "groudon", "rayquaza", "uxie", "mesprit", "azelf",
                "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "cobalion", "terrakion", "virizion",
                "tornadus", "thundurus", "reshiram", "zekrom", "landorus", "kyurem", "xerneas", "yveltal", "zygarde",
                "silvally", "tapu-koko", "tapu-lele", "tapu-bulu", "tapu-fini", "cosmog", "cosmoem", "solgaleo", "lunala",
                "necrozma", "zacian", "zamazenta", "eternatus", "kubfu", "urshifu", "regieleki", "regidrago", "glastrier",
                "spectrier", "calyrex", "enamorus", "wo-chien", "chien-pao", "ting-lu", "chi-yu", "koraidon", "miraidon",
                "okidogi", "munkidori", "fezandipiti", "ogerpon", "terapagos"
        ));
        MYTHICAL.addAll(Arrays.asList(
                "mew", "celebi", "jirachi", "deoxys", "phione", "manaphy", "darkrai", "shaymin", "arceus", "victini",
                "keldeo", "meloetta", "genesect", "diancie", "hoopa", "volcanion", "magearna", "marshadow", "zeraora",
                "meltan", "melmetal", "zarude", "pecharunt"
        ));
        ULTRA_BEAST.addAll(Arrays.asList(
                "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord", "poipole",
                "naganadel", "stakataka", "blacephalon"
        ));
        PARADOX.addAll(Arrays.asList(
                "great-tusk", "scream-tail", "brute-bonnet", "flutter-mane", "slither-wing", "sandy-shocks",
                "roaring-moon", "walking-wake", "gouging-fire", "raging-bolt", "iron-treads", "iron-bundle",
                "iron-hands", "iron-jugulis", "iron-moth", "iron-thorns", "iron-valiant", "iron-leaves",
                "iron-boulder", "iron-crown"
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
                "dragonite", "wobbuffet", "dunsparce", "heracross", "skarmory", "smeargle", "blissey", "larvitar",
                "pupitar", "tyranitar", "ralts", "kirlia", "gardevoir", "sableye", "trapinch", "vibrava", "flygon",
                "absol", "wynaut", "clamperl", "huntail", "gorebyss", "bronzor", "bronzong", "happiny", "spiritomb",
                "munchlax", "riolucario", "drapion", "toxicroak", "gallade", "froslass", "simisage", "simisear",
                "simipour", "excadrill", "audino", "throh", "sawk", "garbodor", "minccino", "gothorita", "solosis",
                "duosion", "vanillish", "axew", "fraxure", "haxorus", "mienshao", "druddigon", "deino", "zweilous",
                "hydreigon", "larvesta", "volcarona", "meowstic", "slurpuff", "malamar", "tyrunt", "tyrantrum",
                "amaura", "aurorus", "hawlucha", "goomy", "sliggoo", "goodra", "oranguru", "passimian", "mimikyu",
                "drampa", "orbeetle", "drednaw", "coalossal", "applin", "flapple", "appletun", "sandaconda",
                "toxtricity", "sirfetchd", "mr-rime", "indeedee", "frigibax", "arctibax", "gholdengo", "dipplin",
                "poltchageist", "sinistcha", "lokix", "pawmo", "pawmot", "charcadet", "armarouge", "ceruledge",
                "rabsca", "cyclizar", "orthworm", "greavard", "houndstone", "dondozo", "tatsugiri"
        ));
    }
}