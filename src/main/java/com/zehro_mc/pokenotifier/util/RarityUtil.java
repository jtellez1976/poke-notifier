package com.zehro_mc.pokenotifier.util;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Set;

public class RarityUtil {

    // --- Listas de Rareza ---

    // ¡Listas actualizadas con los datos de tu archivo AllPokemons.json!
    private static final Set<String> CUSTOM_RARES = Set.of(
            "kadabra", "alakazam", "chansey", "kangaskhan", "scyther", "pinsir", "porygon",
            "omanyte", "omastar", "kabuto", "kabutops", "aerodactyl", "snorlax", "dratini",
            "dragonair", "dragonite", "wobbuffet", "dunsparce", "heracross", "skarmory",
            "smeargle", "blissey", "raikou", "entei", "suicune", "larvitar", "pupitar",
            "tyranitar", "ralts", "kirlia", "gardevoir", "sableye", "trapinch", "vibrava",
            "flygon", "absol", "wynaut", "clamperl", "huntail", "gorebyss", "bronzor",
            "bronzong", "happiny", "spiritomb", "riolou", "lucario", "drapion", "toxicroak",
            "gallade", "froslass", "simisage", "simisear", "simipour", "excadrill", "audino",
            "throh", "sawk", "garbodor", "gothorita", "solosis", "duosion", "minccino",
            "vanillish", "axew", "fraxure", "haxorus", "mienfoo", "mienshao", "druddigon",
            "deino", "zweilous", "hydreigon", "meowstic", "slurpuff", "malamar", "tyrunt",
            "amaura", "hawlucha", "goomy", "sliggoo", "goodra", "oranguru", "passimian",
            "mimikyu", "drampa", "meltan", "orbeetle", "drednaw", "coalossal", "applin",
            "flapple", "appletun", "sandaconda", "toxtricity", "sirfetchd", "mr-rime",
            "duraludon", "kleavor", "rabsca", "cyclizar", "orthworm", "greavard",
            "houndstone", "dondozo", "tatsugiri", "great-tusk", "scream-tail",
            "brute-bonnet", "flutter-mane", "slither-wing", "sandy-shocks", "iron-treads",
            "iron-bundle", "iron-hands", "iron-jugulis", "iron-moth", "iron-thorns",
            "frigibax", "arctibax", "iron-valiant", "dipplin", "poltchageist", "sinistcha",
            "iron-boulder", "iron-crown", "indeedee", "mothim"
    );
    private static final Set<String> CUSTOM_ULTRA_RARES = Set.of(
            "bouffalant", "bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "charizard",
            "squirtle", "wartortle", "blastoise", "ditto", "articuno", "zapdos", "moltres",
            "mewtwo", "mew", "chikorita", "bayleef", "meganium", "cyndaquil", "quilava",
            "typhlosion", "totodile", "croconaw", "feraligatr", "espeon", "umbreon",
            "lugia", "ho-oh", "celebi", "treecko", "grovyle", "sceptile", "torchic",
            "combusken", "blaziken", "mudkip", "marshtomp", "swampert", "lileep",
            "cradily", "anorith", "armaldo", "bagon", "shelgon", "salamence", "beldum",
            "metang", "metagross", "regirock", "regice", "registeel", "latias", "latios",
            "kyogre", "groudon", "rayquaza", "jirachi", "deoxys", "turtwig", "grotle",
            "torterra", "chimchar", "monferno", "infernape", "piplup", "prinplup",
            "empoleon", "cranidos", "rampardos", "shieldon", "bastiodon", "gible",
            "gabite", "garchomp", "leafeon", "glaceon", "uxie", "mesprit", "azelf",
            "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "phione",
            "manaphy", "darkrai", "shaymin", "arceus", "victini", "snivy", "servine",
            "serperior", "tepig", "pignite", "emboar", "oshawott", "dewott", "samurott",
            "leavanny", "tirtouga", "carracosta", "archen", "archeops", "cinccino",
            "gothitelle", "reuniclus", "vanilluxe", "cobalion", "terrakion", "virizion",
            "tornadus", "thundurus", "reshiram", "zekrom", "landorus", "kyurem", "keldeo",
            "meloetta", "genesect", "chespin", "quilladin", "chesnaught", "fennekin",
            "braixen", "delphox", "froakie", "frogadier", "greninja", "sylveon", "xerneas",
            "yveltal", "zygarde", "diancie", "hoopa", "volcanion", "rowlet", "dartrix",
            "decidueye", "litten", "torracat", "incineroar", "popplio", "brionne",
            "primarina", "type-null", "jangmo-o", "hakamo-o", "kommo-o", "tapu-koko",
            "tapu-lele", "tapu-bulu", "tapu-fini", "cosmog", "cosmoem", "solgaleo",
            "lunala", "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela",
            "kartana", "guzzlord", "necrozma", "magearna", "marshadow", "poipole",
            "naganadel", "stakataka", "blacephalon", "zeraora", "grookey", "thwackey",
            "rillaboom", "scorbunny", "raboot", "cinderace", "sobble", "drizzile",
            "inteleon", "dreepy", "drakloak", "dragapult", "zacian", "zamazenta",
            "eternatus", "kubfu", "urshifu", "zarude", "regieleki", "regidrago",
            "glastrier", "spectrier", "calyrex", "enamorus", "sprigatito", "floragato",
            "meowscarada", "fuecoco", "crocalor", "skeledirge", "quaxly", "quaxwell",
            "quaquaval", "dudunsparce", "kingambit", "baxcalibur", "wo-chien",
            "chien-pao", "ting-lu", "chi-yu", "roaring-moon", "koraidon", "miraidon",
            "walking-wake", "iron-leaves", "okidogi", "munkidori", "fezandipiti",
            "ogerpon", "archaludon", "hydrapple", "gouging-fire", "raging-bolt",
            "terapagos", "pecharunt"
    );

    // =====================================================================================

    private static final Set<Integer> LEGENDARIES = Set.of(
            144, 145, 146, 150, 243, 244, 245, 249, 250, 377, 378, 379, 380, 381, 382, 383, 384, 480, 481, 482,
            483, 484, 485, 486, 487, 488, 638, 639, 640, 641, 642, 643, 644, 645, 646, 716, 717, 718, 772, 773,
            785, 786, 787, 788, 789, 790, 791, 792, 800, 888, 889, 890, 891, 892, 894, 895, 896, 897, 898, 905,
            1001, 1002, 1003, 1004, 1007, 1008, 1014, 1015, 1016, 1017, 1024
    );

    private static final Set<Integer> MYTHICALS = Set.of(
            151, 251, 385, 386, 489, 490, 491, 492, 493, 494, 647, 648, 649, 719, 720, 721, 801, 802, 807, 808,
            809, 893, 1025
    );

    private static final Set<Integer> ULTRA_BEASTS = Set.of(
            793, 794, 795, 796, 797, 798, 799, 803, 804, 805, 806
    );

    private static final Set<Integer> PARADOX = Set.of(
            984, 985, 986, 987, 988, 989, 1005, 1009, 1020, 1021, 990, 991, 992, 993, 994, 995, 1006, 1010,
            1022, 1023
    );

    /**
     * Define las categorías de rareza, su nombre para la traducción y su color.
     */
    public enum RarityCategory {
        NONE("none", NamedTextColor.WHITE, 0),
        RARE("rare", NamedTextColor.AQUA, 2),
        ULTRA_RARE("ultra_rare", NamedTextColor.LIGHT_PURPLE, 4),
        PARADOX("paradox", NamedTextColor.DARK_PURPLE, 13),
        ULTRA_BEAST("ultra_beast", NamedTextColor.DARK_AQUA, 10),
        MYTHICAL("mythical", NamedTextColor.RED, 5),
        LEGENDARY("legendary", NamedTextColor.GOLD, 1),
        SHINY("shiny", NamedTextColor.YELLOW, 12);

        private final String translationKey;
        private final TextColor color;
        private final int waypointColor;

        RarityCategory(String translationKey, TextColor color, int waypointColor) {
            this.translationKey = "chat.poke-notifier." + translationKey;
            this.color = color;
            this.waypointColor = waypointColor;
        }

        public Component toComponent() {
            return Component.translatable(this.translationKey).color(this.color);
        }

        public TextColor getTextColor() {
            return this.color;
        }

        public int getWaypointColor() {
            return this.waypointColor;
        }
    }

    /**
     * Determina la categoría de rareza de un Pokémon.
     * La prioridad es: Shiny > Mythical > Legendary > Ultra Beast > Paradox > Custom Ultra Rare > Custom Rare
     */
    public static RarityCategory getRarity(Pokemon pokemon) {
        // --- INICIO DEL CÓDIGO DE PRUEBA ---
        // Para probar, forzamos a bouffalant a ser de una categoría específica.
        // Cambia 'LEGENDARY' por 'MYTHICAL', 'SHINY', 'PARADOX', etc. para probar cada mensaje.
        if (pokemon.getSpecies().getResourceIdentifier().getPath().equals("bouffalant")) {
            return RarityCategory.ULTRA_BEAST;
        }
        // --- FIN DEL CÓDIGO DE PRUEBA ---

        if (pokemon.getAspects().contains("shiny")) {
            return RarityCategory.SHINY;
        }

        int dexId = pokemon.getSpecies().getNationalPokedexNumber();

        if (MYTHICALS.contains(dexId)) {
            return RarityCategory.MYTHICAL;
        }
        if (LEGENDARIES.contains(dexId)) {
            return RarityCategory.LEGENDARY;
        }
        if (ULTRA_BEASTS.contains(dexId)) {
            return RarityCategory.ULTRA_BEAST;
        }
        if (PARADOX.contains(dexId)) {
            return RarityCategory.PARADOX;
        }

        String speciesName = pokemon.getSpecies().getResourceIdentifier().getPath();

        if (CUSTOM_ULTRA_RARES.contains(speciesName)) {
            return RarityCategory.ULTRA_RARE;
        }
        if (CUSTOM_RARES.contains(speciesName)) {
            return RarityCategory.RARE;
        }

        return RarityCategory.NONE;
    }
}