package com.zehro_mc.pokenotifier;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Config {

    public List<String> _instructions = List.of(
            "--- Poke-Notifier Configuration Instructions ---",
            "This file allows you to customize which Pokémon trigger a notification.",
            " ",
            "HOW TO EDIT:",
            " - To add a Pokémon to a category, add its name in lowercase inside the square brackets [ ].",
            " - Make sure to put the name in double quotes and add a comma after it if it's not the last one in the list.",
            " - Example: To make 'badoof' rare, add \"badoof\", to the RARE list.",
            " - Pokémon names should be all lowercase and without spaces (e.g., 'roaringmoon', 'ironvaliant', 'mr.rime').",
            " ",
            "IMPORTANT:",
            " - DO NOT delete the category names like \"LEGENDARY\", \"MYTHICAL\", etc., or the file will break.",
            " - You can safely delete any Pokémon name from a list if you don't want it in that category.",
            " - The mod checks categories from top to bottom (MYTHICAL is checked before LEGENDARY, which is checked before RARE, etc.).",
            " - After saving your changes, use the command '/pokenotifier reloadconfig' in-game to apply them.",
            " "
    );

    // --- Test Mode ---
    // If true, the notifier will also trigger for Pokémon spawned with commands (e.g., /pokespawn).
    // Useful for testing notifications without waiting for natural spawns.
    public boolean enable_test_mode = false;

    // --- General Settings ---
    // The maximum distance in blocks a player can be from a Pokémon to receive a notification.
    public int notification_distance = 200;
    // The duration in seconds that a rare Pokémon will glow after spawning.
    public int glowing_duration_seconds = 120; // 2 minutes


    public Set<String> LEGENDARY = new HashSet<>(Set.of(
            "articuno", "zapdos", "moltres", "mewtwo", "raikou", "entei", "suicune", "lugia", "ho-oh", "regirock",
            "regice", "registeel", "latias", "latios", "kyogre", "groudon", "rayquaza", "uxie", "mesprit", "azelf",
            "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "cobalion", "terrakion", "virizion",
            "tornadus", "thundurus", "reshiram", "zekrom", "landorus", "kyurem", "xerneas", "yveltal", "zygarde",
            "typenull", "silvally", "tapukoko", "tapulele", "tapubulu", "tapufini", "cosmog", "cosmoem", "solgaleo", "lunala", "necrozma",
            "zacian", "zamazenta", "eternatus", "kubfu", "urshifu", "regieleki", "regidrago", "glastrier",
            "spectrier", "calyrex", "enamorus", "wo-chien", "chien-pao", "ting-lu", "chi-yu", "koraidon", "miraidon",
            "okidogi", "munkidori", "fezandipiti", "ogerpon", "terapagos"
    ));

    public Set<String> MYTHICAL = new HashSet<>(Set.of(
            "mew", "celebi", "jirachi", "deoxys", "phione", "manaphy", "darkrai", "shaymin", "arceus", "victini",
            "keldeo", "meloetta", "genesect", "diancie", "hoopa", "volcanion", "magearna", "marshadow", "zeraora",
            "meltan", "melmetal", "zarude", "pecharunt"
    ));

    public Set<String> ULTRA_BEAST = new HashSet<>(Set.of(
            "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord", "poipole",
            "naganadel", "stakataka", "blacephalon"
    ));

    public Set<String> PARADOX = new HashSet<>(Set.of(
            "greattusk", "screamtail", "brutebonnet", "fluttermane", "slitherwing", "sandyshocks", "roaringmoon",
            "walkingwake", "gougingfire", "ragingbolt", "irontreads", "ironbundle", "ironhands", "ironjugulis",
            "ironmoth", "ironthorns", "ironvaliant", "ironleaves", "ironboulder", "ironcrown"
    ));

    public Set<String> PSEUDO_LEGENDARY = new HashSet<>(Set.of(
            "dragonite", "tyranitar", "salamence", "metagross", "garchomp", "hydreigon", "goodra", "kommo-o",
            "dragapult", "baxcalibur"
    ));

    public Set<String> RARE = new HashSet<>(Set.of(
            "bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "charizard", "squirtle", "wartortle",
            "blastoise", "kadabra", "alakazam", "chansey", "kangaskhan", "scyther", "pinsir", "porygon", "omanyte", "omastar",
            "kabuto", "kabutops", "aerodactyl", "snorlax", "vaporeon", "jolteon", "flareon", "dratini", "dragonair",
            "wobbuffet", "dunsparce", "skarmory", "heracross", "smeargle", "blissey", "larvitar", "pupitar", "ralts",
            "kirlia", "gardevoir", "sableye", "trapinch", "vibrava", "flygon", "absol", "wynaut", "clamperl",
            "huntail", "gorebyss", "bronzor", "bronzong", "happiny", "spiritomb", "mothim", "riolou", "lucario",
            "drapion", "toxicroak", "gallade", "froslass", "simisage", "simisear", "simipour", "excadrill",
            "audino", "throh", "sawk", "garbodor", "gothorita", "solosis", "duosion", "minccino", "vanillish",
            "axew", "fraxure", "haxorus", "mienfoo", "mienshao", "druddigon", "deino", "zweilous", "larvesta", "volcarona", "meowstic",
            "slurpuff", "malamar", "tyrunt", "tyrantrum", "amaura", "aurorus", "hawlucha", "goomy", "sliggoo",
            "oranguru", "passimian", "mimikyu", "drampa", "orbeetle", "drednaw", "coalossal", "applin", "flapple",
            "appletun", "sandaconda", "toxtricity", "sirfetch’d", "mr.rime", "duraludon", "kleavor", "rabsca",
            "lokix", "pawmo", "pawmot", "charcadet", "armarouge", "ceruledge", "cyclizar", "orthworm",
            "greavard", "houndstone", "dondozo", "tatsugiri", "gholdengo", "frigibax", "arctibax", "dipplin",
            "poltchageist", "sinistcha", "indeedee", "treecko", "grovyle", "sceptile", "torchic", "combusken", "blaziken",
            "mudkip", "marshtomp", "swampert", "lileep", "cradily", "anorith", "armaldo", "bagon", "shelgon", "beldum", "metang",
            "turtwig", "grotle", "torterra", "chimchar", "monferno", "infernape", "piplup", "prinplup", "empoleon",
            "cranidos", "rampardos", "shieldon", "bastiodon", "gible", "gabite", "snivy", "servine", "serperior",
            "tepig", "pignite", "emboar", "oshawott", "dewott", "samurott", "pidove", "tranquill", "unfezant",
            "leavanny", "tirtouga", "carracosta", "archen", "archeops", "cinccino", "gothitelle", "reuniclus", "vanilluxe",
            "chespin", "quilladin", "chesnaught", "fennekin", "braixen", "delphox", "froakie", "frogadier", "greninja",
            "sylveon", "rowlet", "dartrix", "decidueye", "litten", "torracat", "incineroar", "popplio", "brionne", "primarina",
            "jangmo-o", "hakamo-o", "grookey", "thwackey", "rillaboom", "scorbunny", "raboot", "cinderace", "sobble",
            "drizzile", "inteleon", "dracozolt", "arctozolt", "dracovish", "arctovish", "dreepy", "drakloak",
            "sprigatito", "floragato", "meowscarada", "fuecoco", "crocalor", "skeledirge", "quaxly", "quaxwell", "quaquaval",
            "dudunsparce", "kingambit", "archaludon", "hydrapple"
    ));

    public Set<String> UNCOMMON = new HashSet<>(Set.of(
            "pikachu", "raichu", "nidoran♀", "nidorina", "nidoqueen", "nidoran♂", "nidorino", "nidoking",
            "clefairy", "clefable", "vulpix", "ninetales", "abra", "ponyta", "rapidash", "grimer", "muk",
            "onix", "hitmonlee", "hitmonchan", "lickitung", "staryu", "starmie", "mr.mime", "jynx", "electabuzz",
            "magmar", "lapras", "eevee", "pichu", "marill", "azumarill", "sudowoodo", "ampharos", "yanma",
            "murkrow", "gligar", "steelix", "scizor", "shuckle", "sneasel", "piloswine", "corsola", "delibird",
            "porygon2", "tyrogue", "hitmontop", "smoochum", "elekid", "silcoon", "beautifly", "cascoon", "dustox",
            "surskit", "masquerain", "whismur", "loudred", "exploud", "nosepass", "swalot", "grumpig", "spinda",
            "altaria", "zangoose", "seviper", "wailmer", "wailord", "claydol", "castform", "kecleon", "banette",
            "tropius", "chimecho", "glalie", "relicanth", "wormadam", "pachirisu", "drifloon", "drifblim",
            "purugly", "chingling", "skuntank", "chatot", "carnivine", "mantyke", "weavile", "magnezone",
            "lickilicky", "yanmega", "mamoswine", "porygon-z", "probopass", "rotom", "zebstrika", "seismitoad", "cofagrigus",
            "zoroark", "cryogonal", "bisharp", "mandibuzz", "diggersby", "pyroar", "gogoat", "aegislash",
            "aromatisse", "barbaracle", "heliolisk", "noivern", "gumshoos", "vikavolt", "crabomibale",
            "oricorio", "lycanroc", "toxapex", "golisopod", "palossand", "pyukumuku", "minior",
            "turtonator", "togedemaru", "bruxish", "greedent", "dottler", "thievul", "silicobra", "cramorant",
            "arboliva", "bellibolt", "kilowattrel", "toedscruel", "klawf", "scovillain", "espathra", "revavroom",
            "cetitan", "veluza", "spidops"
    ));
}