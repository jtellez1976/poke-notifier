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
    public Set<String> PSEUDO_LEGENDARY = new HashSet<>();
    public Set<String> RARE = new HashSet<>();
    public Set<String> UNCOMMON = new HashSet<>();
    public Set<String> COMMON = new HashSet<>();

    public ConfigPokemon() {
        // Populamos las listas con los valores por defecto en el constructor.
        LEGENDARY.addAll(Arrays.asList(
                "articuno", "zapdos", "moltres", "mewtwo", "raikou", "entei", "suicune", "lugia", "ho-oh", "regirock",
                "regice", "registeel", "latias", "latios", "kyogre", "groudon", "rayquaza", "uxie", "mesprit", "azelf",
                "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "cobalion", "terrakion", "virizion", "tornadus", "thundurus", "reshiram", "zekrom", "landorus", "kyurem",
                "xerneas", "yveltal", "zygarde", "tapu-koko", "tapu-lele", "tapu-bulu", "tapu-fini", "cosmog", "cosmoem", "solgaleo", "lunala", "necrozma", "zacian",
                "zamazenta", "eternatus", "kubfu", "urshifu", "regieleki", "regidrago", "glastrier", "spectrier", "calyrex", "enamorus", "wo-chien", "chien-pao", "ting-lu", "chi-yu", "koraidon", "miraidon", "ogerpon", "fezandipiti", "munkidori", "okidogi", "terapagos"));
        MYTHICAL.addAll(Arrays.asList("mew", "celebi", "jirachi", "deoxys", "phione", "manaphy", "darkrai", "shaymin", "arceus", "victini", "keldeo", "meloetta", "genesect", "diancie", "hoopa", "volcanion", "magearna", "marshadow", "zeraora", "meltan", "melmetal", "zarude", "pecharunt"));
        ULTRA_BEAST.addAll(Arrays.asList("nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord", "poipole", "naganadel", "stakataka", "blacephalon"));
        PARADOX.addAll(Arrays.asList("great-tusk", "scream-tail", "brute-bonnet", "flutter-mane", "slither-wing", "sandy-shocks", "roaring-moon", "iron-tusk", "iron-bundle", "iron-hands", "iron-jugulis", "iron-moth", "iron-thorns", "iron-valiant", "walking-wake", "iron-leaves", "gouging-fire", "raging-bolt", "iron-boulder", "iron-crown"));
        PSEUDO_LEGENDARY.addAll(Arrays.asList("dragonite", "tyranitar", "salamence", "metagross", "garchomp", "hydreigon", "goodra", "kommo-o", "dragapult", "baxcalibur"));
        RARE.addAll(Arrays.asList("ditto", "eevee", "porygon", "aerodactyl", "dratini", "larvitar", "beldum", "bagon", "gible", "deino", "goomy", "jangmo-o", "dreepy", "frigibax", "feebas", "rotom", "spiritomb", "zorua", "larvesta", "honedge", "mimikyu", "dhelmise", "sinistea", "impidimp", "milcery", "falinks", "snom", "cufant", "dracozolt", "arctozolt", "dracovish", "arctovish", "tinkatink", "charcadet", "gimmighoul", "frillish", "pumpkaboo", "bergmite", "mareanie", "sandygast", "pyukumuku", "minior", "turtonator", "togedemaru", "bruxish", "drampa", "comfey", "oranguru", "passimian", "wishiwashi", "salandit", "stufful", "bounsweet", "morelull", "fomantis", "dewpider", "wimpod", "crabrawler", "cutiefly", "rockruff", "mudbray", "litten", "popplio", "rowlet", "pikipek", "yungoos", "grubbin", "skrelp", "clauncher", "helioptile", "tyrunt", "amaura", "inkay", "swirlix", "spritzee", "binacle", "golett", "cryogonal", "axew", "mienfoo", "bouffalant", "druddigon", "pawniard", "vullaby", "rufflet", "litwick", "ferroseed", "klink", "elgyem", "stunfisk", "sigilyph", "tirtouga", "archen", "solosis", "vanillite", "ducklett", "joltik", "foongus", "alomomola", "audino", "throh", "sawk", "maractus", "scraggy", "darumaka", "sandile", "dwebble", "venipede", "sewaddle", "timburr", "roggenrola", "drilbur", "munna", "blitzle", "riolu", "lucario", "happiny", "blissey", "bonsly", "mime-jr", "chingling", "skorupi", "croagunk", "carnivine", "finneon", "mantyke", "snover", "bronzor", "cherubi", "burmy", "combee", "pachirisu", "drifloon", "shellos", "stunky", "glameow", "cranidos", "shieldon", "shinx", "kricketot", "starly", "turtwig", "chimchar", "piplup", "spheal", "snorunt", "absol", "duskull", "shuppet", "tropius", "clamperl", "luvdisc", "relicanth", "lileep", "anorith", "trapinch", "cacnea", "swablu", "zangoose", "seviper", "corphish", "baltoy", "barboach", "shroomish", "nincada", "slakoth", "ralts", "surskit", "wurmple", "poochyena", "zigzagoon", "treecko", "torchic", "mudkip", "smeargle", "shuckle", "heracross", "qwilfish", "dunsparce", "gligar", "snubbull", "girafarig", "pineco", "forretress", "wobbuffet", "unown", "misdreavus", "murkrow", "aipom", "yanma", "sudowoodo", "hoppip", "natu", "mareep", "togepi", "cleffa", "igglybuff", "pichu", "chinchou", "lanturn", "hoothoot", "sentret", "lapras", "snorlax", "omanyte", "kabuto", "tauros", "pinsir", "scyther", "jynx", "electabuzz", "magmar", "mr-mime", "staryu", "starmie", "seadra", "horsea", "koffing", "weezing", "rhyhorn", "rhydon", "lickitung", "tangela", "cubone", "marowak", "exeggcute", "exeggutor", "hitmonlee", "hitmonchan", "drowzee", "hypno", "onix", "shellder", "cloyster", "grimer", "muk", "doduo", "dodrio", "farfetchd", "seel", "dewgong", "magnemite", "magneton", "slowpoke", "slowbro", "ponyta", "rapidash", "tentacool", "tentacruel", "geodude", "graveler", "golem", "bellsprout", "weepinbell", "victreebel", "machop", "machoke", "machamp", "abra", "kadabra", "alakazam", "poliwag", "poliwhirl", "poliwrath", "mankey", "primeape", "growlithe", "arcanine", "psyduck", "golduck", "meowth", "persian", "diglett", "dugtrio", "venonat", "venomoth", "paras", "parasect", "vulpix", "ninetales", "jigglypuff", "wigglytuff", "zubat", "golbat", "oddish", "gloom", "vileplume", "clefairy", "clefable", "nidoran-f", "nidorina", "nidoqueen", "nidoran-m", "nidorino", "nidoking", "sandshrew", "sandslash", "ekans", "arbok", "spearow", "fearow", "rattata", "raticate", "pidgey", "pidgeotto", "pidgeot", "weedle", "kakuna", "beedrill", "caterpie", "metapod", "butterfree", "wartortle", "blastoise", "charmeleon", "charizard", "ivysaur", "venusaur"));
        UNCOMMON.addAll(Arrays.asList("bulbasaur", "charmander", "squirtle", "chikorita", "cyndaquil", "totodile", "treecko", "torchic", "mudkip", "turtwig", "chimchar", "piplup", "snivy", "tepig", "oshawott", "chespin", "fennekin", "froakie", "rowlet", "litten", "popplio", "grookey", "scorbunny", "sobble", "sprigatito", "fuecoco", "quaxly"));
    }
}