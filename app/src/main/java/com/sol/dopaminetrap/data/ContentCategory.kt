package com.sol.dopaminetrap.data

enum class CategoryType { INTEREST, CONCERN }

enum class ConcernLevel { NONE, LOW, MEDIUM, HIGH, CRITICAL }

enum class ContentCategory(
    val displayName: String,
    val type: CategoryType,
    val concernLevel: ConcernLevel,
    val keywords: List<String>
) {

    // ── INTERESE ──────────────────────────────────────────────────────────────

    SPORT_MISCARE(
        "Sport & Mișcare", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "fotbal", "football", "soccer", "înot", "swimming", "gimnastică", "gymnastics",
            "baschet", "basketball", "tenis", "tennis", "alergare", "running", "ciclism",
            "cycling", "karate", "judo", "box", "boxing", "handbal", "handball", "volei",
            "volleyball", "rugby", "atletism", "athletics", "yoga", "pilates", "fitness",
            "workout", "antrenament", "training", "#sport", "#fitness", "#gym", "#workout",
            "parkour", "skate", "skateboard", "surf", "snowboard", "schi", "patinaj",
            "skating", "tir cu arcul", "archery", "scrimă", "fencing", "meci", "campionat",
            "championship", "competiție", "competition", "olimpiadă", "olympics"
        )
    ),
    MUZICA_ARTE(
        "Muzică & Arte", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "muzică", "music", "cântec", "song", "melodie", "melody", "chitară", "guitar",
            "pian", "piano", "vioară", "violin", "tobe", "drums", "concert", "festival",
            "album", "single", "artist", "trupă", "band", "rap", "pop", "rock", "clasic",
            "classical", "jazz", "hip hop", "desen", "drawing", "pictură", "painting",
            "artă", "art", "sculptură", "sculpture", "origami", "craft", "handmade",
            "#music", "#art", "#drawing", "#painting", "acuarelă", "watercolor",
            "ilustrație", "illustration", "grafică", "fotografie", "photography",
            "ukulele", "flaut", "flute", "saxofon", "saxophone", "cor", "choir"
        )
    ),
    DANS(
        "Dans & Coregrafie", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "dans", "dance", "dancing", "coregrafie", "choreography", "ballet", "balet",
            "breakdance", "salsa", "tango", "vals", "waltz", "zumba", "#dance", "#dancing",
            "#choreography", "dance tutorial", "dance challenge", "trending dance",
            "hip hop dance", "contemporary", "contemporan", "modern dance"
        )
    ),
    GAMING(
        "Gaming", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "minecraft", "roblox", "gaming", "gamer", "game", "joc", "jocuri", "fortnite",
            "among us", "clash", "brawl stars", "pokemon", "mario", "zelda", "playstation",
            "xbox", "nintendo", "switch", "pc gaming", "streamer", "#gaming", "#gamer",
            "#minecraft", "#roblox", "console", "consolă", "multiplayer", "speedrun",
            "level up", "achievement", "quest", "respawn", "skin", "build", "crafting",
            "survival", "sandbox", "rpg", "strategy", "puzzle game"
        )
    ),
    STIINTA_NATURA(
        "Știință & Natură", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "știință", "science", "experiment", "natură", "nature", "animale", "animals",
            "dinosaur", "dinozaur", "spațiu", "space", "astronomie", "astronomy", "cosmos",
            "planetă", "planet", "stea", "star", "univers", "universe", "biologie", "biology",
            "chimie", "chemistry", "fizică", "physics", "geografie", "geography", "geologie",
            "geology", "ocean", "sea", "jungle", "junglă", "pădure", "forest",
            "#science", "#nature", "#animals", "wildlife", "ecosistem", "ecosystem",
            "mediu", "environment", "insecte", "insects", "reptile", "mamifere", "mammals",
            "vulcan", "volcano", "cutremur", "earthquake", "fosile", "fossils",
            "microscop", "microscope", "laborator", "laboratory"
        )
    ),
    EDUCATIE_LECTURA(
        "Educație & Lectură", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "educație", "education", "lectură", "reading", "carte", "book", "matematică",
            "mathematics", "math", "algebră", "algebra", "geometrie", "geometry",
            "istorie", "history", "literatură", "literature", "poem", "poetry",
            "limbă", "language", "engleză", "english", "franceză", "french",
            "germană", "german", "spaniolă", "spanish", "learn", "învățare",
            "tutorial", "how to", "cum să", "#education", "#learning", "#books",
            "quiz", "trivia", "curiozități", "facts", "did you know", "știai că",
            "lecție", "lesson", "homework", "temă", "proiect", "project",
            "inventator", "inventor", "descoperire", "discovery"
        )
    ),
    UMOR_ENTERTAINMENT(
        "Umor & Entertainment", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "funny", "amuzant", "comedie", "comedy", "glume", "jokes", "meme", "prank",
            "sketch", "parody", "parodie", "fail", "bloopers", "#funny", "#comedy",
            "magie", "magic", "iluzionism", "trick", "magician", "superhero", "supererou",
            "cosplay", "anime", "cartoon", "desene animate", "film", "movie", "serial",
            "show", "stand up", "roast friendly", "sarcasm", "ironie", "satira"
        )
    ),
    GATIT_ALIMENTATIE(
        "Gătit & Alimentație", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "gătit", "cooking", "rețetă", "recipe", "mâncare", "food", "desert", "dessert",
            "tort", "cake", "pizza", "paste", "sushi", "baking", "copt", "#food",
            "#cooking", "#recipe", "#baking", "chef", "bucătar", "kitchen", "restaurant",
            "smoothie", "healthy food", "mâncare sănătoasă", "meal prep", "snack",
            "gustare", "mic dejun", "breakfast", "prânz", "lunch", "cină", "dinner",
            "ciocolată", "chocolate", "înghețată", "ice cream", "fructe", "fruits",
            "legume", "vegetables", "nutriție", "nutrition"
        )
    ),
    ANIMALE_PETS(
        "Animale & Pets", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "câine", "dog", "pisică", "cat", "animal", "pets", "pet", "hamster", "iepure",
            "rabbit", "papagal", "parrot", "pește", "fish", "acvariu", "aquarium",
            "#pets", "#dog", "#cat", "#animals", "puppy", "căței", "kitten",
            "adorabil", "cute", "funny animal", "animal videos", "wildlife", "sălbatic",
            "delfin", "dolphin", "elefant", "elephant", "leu", "lion", "urs", "bear",
            "vulpe", "fox", "veverița", "squirrel", "broască", "frog", "țestoasă", "turtle"
        )
    ),
    FASHION_STYLE(
        "Modă & Stil", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "modă", "fashion", "stil", "style", "outfit", "haine", "clothes",
            "pantofi", "shoes", "geantă", "bag", "accesorii", "accessories", "trend",
            "#fashion", "#style", "#outfit", "#ootd", "designer", "brand",
            "colecție", "collection", "lookbook", "haul", "shopping", "cumpărături",
            "bijuterii", "jewelry", "pălărie", "hat", "eșarfă", "scarf"
        )
    ),
    CALATORIE_CULTURA(
        "Călătorii & Cultură", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "călătorie", "travel", "vacanță", "vacation", "holiday", "turism", "tourism",
            "țară", "country", "oraș", "city", "munte", "mountain", "plajă", "beach",
            "cultură", "culture", "tradiție", "tradition", "festival", "sărbătoare",
            "#travel", "#vacation", "#wanderlust", "vlog", "aventură", "adventure",
            "explorare", "explore", "backpacking", "destinație", "destination",
            "muzeu", "museum", "castel", "castle", "monument", "landmark"
        )
    ),
    DIY_CREATIV(
        "DIY & Creativitate", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "diy", "handmade", "craft", "lego", "construcție", "build", "robot",
            "robotică", "robotics", "arduino", "3d print", "origami", "quilling",
            "scrapbook", "bijuterii", "jewelry", "tricotat", "knitting", "croșetat",
            "crochet", "cusut", "sewing", "lemn", "woodworking", "#diy", "#handmade",
            "#craft", "cum se face", "how to make", "bricolaj", "modelism",
            "pictează", "sculptează", "creează"
        )
    ),
    SOCIAL_PRIETENII(
        "Social & Prietenii", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "prieteni", "friends", "friendship", "prietenie", "echipă", "team",
            "împreună", "together", "colegi", "classmates", "best friend", "bff",
            "petrecere", "party", "distracție", "fun", "#friends", "#friendship",
            "#squad", "comunitate", "community", "volunteer", "voluntariat",
            "ajutor", "help", "empatie", "empathy", "kindness", "bunătate",
            "fac bine", "spread kindness", "prietenie adevărată"
        )
    ),

    // ── SEMNALE DE ATENȚIE — Sănătate mintală ────────────────────────────────

    TRISTETE_MELANCOLIE(
        "Tristețe & Melancolie", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "sad", "trist", "tristețe", "sadness", "depressed", "deprimat", "depresie",
            "depression", "crying", "plâng", "tears", "lacrimi", "hopeless", "fără speranță",
            "alone", "singur", "lonely", "singurătate", "loneliness", "empty", "gol pe dinăuntru",
            "nu mai vreau", "i don't care anymore", "nothing matters", "nimic nu contează",
            "broken", "distrus", "hurt", "rănit", "pain", "durere", "#sad", "#depressed",
            "#depression", "#lonely", "nobody cares", "nimeni nu înțelege", "invisible",
            "invizibil", "worthless", "inutil", "failure", "ratat", "loser",
            "feeling lost", "mă simt pierdut", "dark thoughts", "gânduri negre",
            "nu mai am chef", "nu mai simt nimic", "totul e fără rost"
        )
    ),
    ANXIETATE_FRICA(
        "Anxietate & Frici", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "anxietate", "anxiety", "anxious", "anxios", "panic", "panică", "panic attack",
            "atac de panică", "worried", "îngrijorat", "stress", "stres", "scared", "frică",
            "fear", "teamă", "overwhelmed", "copleșit", "nervous", "neliniștit",
            "phobia", "fobie", "#anxiety", "#mentalhealth", "can't breathe",
            "nu pot respira", "heart racing", "overthinking", "gândesc prea mult",
            "mi-e frică", "sunt speriat", "frică de viitor"
        )
    ),
    STIMA_SINE_SCAZUTA(
        "Stimă de sine scăzută", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "ugly", "urât", "stupid", "prost", "dumb", "idiot",
            "not good enough", "nu sunt suficient", "hate myself", "mă urăsc",
            "hate my body", "urăsc corpul meu", "imperfect", "insecure", "nesigur",
            "comparing myself", "mă compar", "not pretty", "nu sunt frumoasă",
            "#insecure", "#selfhate", "low self esteem", "stimă scăzută",
            "nu valorez nimic", "toți sunt mai buni", "nu pot face nimic bine"
        )
    ),
    CONTINUT_SUICIDAR(
        "Conținut suicidar", CategoryType.CONCERN, ConcernLevel.CRITICAL,
        listOf(
            "suicid", "suicide", "sinucidere", "sinucid", "kill myself", "mă omor",
            "want to die", "vreau să mor", "end my life", "nu vreau să mai trăiesc",
            "better off dead", "ar fi mai bine mort", "goodbye forever",
            "no reason to live", "nu am motive să trăiesc", "#suicide",
            "overdose", "supradoză", "methods", "metode", "nu mai vreau să exist"
        )
    ),
    AUTOMUTILARE(
        "Automutilare", CategoryType.CONCERN, ConcernLevel.CRITICAL,
        listOf(
            "self harm", "automutilare", "cutting", "tăiat", "burns", "arsuri",
            "hurt myself", "mă rănesc", "scars", "cicatrici", "#selfharm", "#cutting",
            "blades", "lame", "wounds", "răni", "injure myself", "mă tai",
            "îmi fac rău", "durerea fizică ajută"
        )
    ),

    // ── SEMNALE DE ATENȚIE — Comportament social ──────────────────────────────

    BULLYING_VICTIMA(
        "Bullying (victimă)", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "bullied", "bully", "bullying", "hărțuire", "harassed", "hărțuit",
            "mocked", "ridiculizat", "made fun of", "râd de mine", "excluded", "exclus",
            "no one likes me", "nimeni nu mă place", "they hate me", "toți mă urăsc",
            "mean to me", "cyberbullying", "#antibullying", "victim", "victimă",
            "abuzat", "toți se iau de mine", "mă ignoră toți", "sunt exclus"
        )
    ),
    BULLYING_AGRESOR(
        "Conținut care glorifică agresiunea", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "fight", "bătaie", "beating", "hit them", "roast", "humiliate", "umilind",
            "making fun of", "batjocură", "picking on", "#fight", "gang up",
            "hate on", "cancel", "dragging", "exposing", "destroy them",
            "să îl bătem", "să îi facem viața grea", "bullying e amuzant"
        )
    ),
    URA_DISCRIMINARE(
        "Ură & Discriminare", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "racist", "rasist", "racism", "rasism", "hate speech", "discurs de ură",
            "xenophobia", "xenofobie", "discrimination", "discriminare", "sexist",
            "homophobic", "homofob", "slur", "insulte", "#hate",
            "inferior", "subuman", "not welcome", "mergeți acasă", "nu aveți loc aici"
        )
    ),
    IZOLARE_SOCIALA(
        "Izolare deliberată", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "avoid everyone", "evit pe toți", "don't need friends", "nu am nevoie de prieteni",
            "people are toxic", "oamenii sunt toxici", "trust no one", "nu ai încredere",
            "better alone", "mai bine singur", "antisocial",
            "don't go outside", "nu ieși afară", "stay inside", "stai în casă",
            "social anxiety", "anxietate socială", "îmi e frică de oameni",
            "prefer să fiu singur mereu", "nu mai vreau prieteni"
        )
    ),

    // ── SEMNALE DE ATENȚIE — Conținut inadecvat vârstei ──────────────────────

    CONTINUT_ADULT(
        "Conținut pentru adulți", CategoryType.CONCERN, ConcernLevel.CRITICAL,
        listOf(
            "nsfw", "18+", "adult content", "onlyfans", "sexy", "hot body",
            "strip", "lingerie", "explicit", "sexual", "xxx",
            "provocative", "provocator", "seductive", "seducător",
            "erotic", "erotic content", "nudity", "nuditate"
        )
    ),
    VIOLENTA_EXTREMA(
        "Violență extremă", CategoryType.CONCERN, ConcernLevel.CRITICAL,
        listOf(
            "gore", "kill", "murder", "ucidere", "graphic violence",
            "violență grafică", "torture", "tortură", "brutal", "decapitate",
            "stabbing", "înjunghiere", "shooting real", "war crimes",
            "#gore", "#brutal", "extreme violence", "sânge mult", "crime reale"
        )
    ),
    CONTINUT_ARME(
        "Glorificarea armelor", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "gun collection", "pistol", "armă de foc", "weapon", "knife fight",
            "cuțit", "blade", "shoot people", "assault rifle", "pușcă de asalt",
            "#guns", "#weapons", "armed", "înarmat", "cumpăr arme",
            "illegal gun", "armă ilegală", "știu să trag"
        )
    ),
    HORROR_EXTREM(
        "Horror extrem", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "extreme horror", "horror extrem", "creepypasta", "scary story",
            "pennywise", "slender man", "jeff the killer", "smile dog",
            "backrooms", "liminal horror", "analog horror", "cursed image",
            "scary video", "video înfricoșător", "coșmar", "nightmare content",
            "me spooked", "mă sperie teribil", "nu pot dormi de frică"
        )
    ),
    CONTINUT_OCULT(
        "Conținut ocult / manipulare", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "occult", "ocult", "ritual", "satanic", "satanist", "cult", "sectă",
            "dark magic", "magie neagră", "spell", "vrajă", "curse", "blestem",
            "demon", "possessed", "posedat", "sacrifice", "sacrificiu",
            "brainwash", "manipulare", "mind control", "control mental",
            "join our group", "alătură-te nouă", "nu spune nimănui"
        )
    ),

    // ── SEMNALE DE ATENȚIE — Obiceiuri nesănătoase ───────────────────────────

    BODY_IMAGE_NEGATIV(
        "Body image negativ", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "diet", "dietă", "lose weight fast", "slăbire rapidă", "thinspo",
            "skinny goals", "too fat", "prea gras", "body check", "perfect body",
            "corp perfect", "flat stomach", "burtă plată", "thigh gap",
            "#weightloss", "#diet", "calorii", "fasting", "înfometare",
            "body shaming", "fat shaming", "ești gras", "slăbește",
            "trebuie să slăbești", "ideal body", "arată bine înseamnă"
        )
    ),
    TULBURARI_ALIMENTARE(
        "Tulburări alimentare promovate", CategoryType.CONCERN, ConcernLevel.CRITICAL,
        listOf(
            "pro ana", "pro mia", "anorexia", "anorexie", "bulimia", "bulimie",
            "not eating", "nu mănânc", "skip meals", "sar peste mese",
            "starvation diet", "dietă de înfometare", "water fast", "post cu apă",
            "how to stop eating", "cum să nu mănânci", "#thinspiration",
            "#thinspo", "eating disorder", "tulburare alimentară",
            "vărsând", "laxative", "purging", "restrict food"
        )
    ),
    CHALLENGE_PERICULOS(
        "Challenge-uri periculoase", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "dangerous challenge", "challenge periculos", "choking game",
            "blackout challenge", "skull breaker", "tide pod challenge",
            "fire challenge", "24 hour challenge", "overnight challenge",
            "don't breathe challenge", "extreme stunt", "cascadorie periculoasă",
            "viral challenge hurt", "challenge rănit", "încearcă asta acasă nu"
        )
    ),
    PRIVARE_SOMN(
        "Privare de somn glorificată", CategoryType.CONCERN, ConcernLevel.LOW,
        listOf(
            "no sleep", "fără somn", "all nighter", "toată noaptea",
            "sleep is for the weak", "3am vibes", "4am", "stayed up all night",
            "haven't slept in", "nu am dormit de", "#nocturnal",
            "insomnie", "can't sleep challenge", "stau treaz toată noaptea",
            "somnul e pierdere de timp"
        )
    ),

    // ── SEMNALE DE ATENȚIE — Substanțe & Pericole ────────────────────────────

    ALCOOL_DROGURI(
        "Alcool & Droguri", CategoryType.CONCERN, ConcernLevel.CRITICAL,
        listOf(
            "drugs", "droguri", "weed", "marijuana", "cannabis", "cocaine", "cocaină",
            "high", "drogat", "stoned", "drunk", "beat", "alcool", "alcohol",
            "vodka", "beer", "bere", "shot", "get wasted", "#weed", "#high", "#drunk",
            "dealer", "blunt", "joint", "xanax", "pills", "pastile de", "substance",
            "îmbătat", "să ne droghăm", "să ne îmbătăm", "cumpăr droguri",
            "mdma", "ecstasy", "heroin", "heroină", "meth", "crystal"
        )
    ),
    FUMAT_VAPING(
        "Fumat & Vaping", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "vaping", "vape", "e-cigarette", "țigară electronică", "smoking", "fumat",
            "cigarette", "țigară", "nicotine", "nicotină", "tobacco", "tutun",
            "#vaping", "#smoking", "smoke rings", "puff", "hookah", "narghilea",
            "juul", "iqos", "cool to smoke", "trag din", "fumez"
        )
    ),
    ACTIVITATI_ILEGALE(
        "Activități ilegale", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "stealing", "furt", "shoplifting", "furat din magazine", "hack",
            "hacking ilegal", "vandalism", "graffiti ilegal", "illegal", "ilegal",
            "crime", "crimă", "trespassing", "intrare ilegală", "pickpocket",
            "scam", "înșelătorie", "fraud", "fraudă", "#illegal", "breaking in",
            "cum să furi", "cum să intri fără", "evitând securitatea"
        )
    ),
    RISC_GROOMING(
        "Interacțiuni periculoase cu adulți", CategoryType.CONCERN, ConcernLevel.CRITICAL,
        listOf(
            // Solicitare întâlnire
            "older guy", "older man", "bărbat mai în vârstă", "meet up", "întâlnire secretă",
            "meet in person", "vino să ne vedem", "hai să ne întâlnim", "hai să ne vedem",
            "știu un loc", "locul nostru secret", "doar noi doi", "să fim singuri",
            // Solicitare poze
            "give me your address", "dă-mi adresa", "send photos", "trimite poze",
            "trimite-mi o poza", "trimite-mi o poză", "o poză cu tine", "fă-mi o poză",
            "selfie pentru mine", "arată-mi cum ești", "cum ești îmbrăcat",
            // Secret față de părinți
            "don't tell your parents", "nu spune părinților", "nu spune mamei",
            "nu spune tatălui", "nu spune nimănui", "secret între noi", "rămâne între noi",
            "părinții tăi nu ar înțelege", "ei nu ar înțelege", "nu i spune",
            // Manipulare / construire relație falsă
            "where do you live", "unde locuiești", "special friend", "private message me",
            "scrie-mi în privat", "ești atât de matură", "ești atât de matur",
            "you're mature for your age", "ești matură pentru vârsta ta",
            "nimeni nu te înțelege ca mine", "eu te înțeleg cel mai bine",
            "eu am grijă de tine", "vreau să am grijă de tine", "ești specială pentru mine",
            "ești special pentru mine", "nu ai nevoie de ei", "eu sunt singurul",
            "eu sunt singura", "prietenul tău secret", "prietena ta secretă"
        )
    ),
    DATE_PERSONALE(
        "Sharing date personale", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "my address", "adresa mea", "my school", "școala mea", "my phone number",
            "numărul meu de telefon", "my location", "locația mea", "where I live",
            "unde stau", "my real name", "numele meu real", "come to my house",
            "vino la mine acasă", "stau pe strada", "îmi dau numărul"
        )
    ),

    // ── SEMNALE DE ATENȚIE — Social media toxic ───────────────────────────────

    MATERIALISM_FLEXING(
        "Materialism & Flexing", CategoryType.CONCERN, ConcernLevel.LOW,
        listOf(
            "rich flex", "bogat", "flexing", "showing off", "expensive stuff",
            "luxury", "lux", "lamborghini", "ferrari", "diamond", "diamant",
            "bling", "drip", "if you're broke", "dacă ești sărac",
            "money is everything", "banii sunt totul", "#rich", "#luxury",
            "get rich quick", "îmbogățit rapid", "cash gang", "bag secured",
            "fac mai mulți bani decât tine", "sărăcia e o alegere"
        )
    ),
    COMPARATII_FOMO(
        "Comparații sociale & FOMO", CategoryType.CONCERN, ConcernLevel.LOW,
        listOf(
            "everyone has it", "toți au", "why don't I have", "de ce nu am",
            "left out", "exclus", "missing out", "ratez", "fomo", "jealous", "gelos",
            "they have everything", "popular kids", "copiii populari",
            "nobody invited me", "nimeni nu m-a invitat", "trending", "viral goals",
            "influencer lifestyle", "ei au tot ce vreau eu", "nu sunt la fel de bun"
        )
    ),
    GAMBLING(
        "Gambling & Loot boxes", CategoryType.CONCERN, ConcernLevel.HIGH,
        listOf(
            "gambling", "jocuri de noroc", "bet", "pariu", "casino", "cazino",
            "slot", "poker", "ruletă", "loot box", "lootbox", "spin to win",
            "jackpot", "win money", "câștigă bani", "lucky spin",
            "#gambling", "#casino", "fortune", "bookmaker",
            "pariez pe", "câștig garantat", "schemă câștig rapid",
            "csgo gambling", "skin betting", "crypto gambling"
        )
    ),
    FAKE_NEWS_CONSPIRATII(
        "Fake news & Conspirații", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "conspiracy", "conspirație", "illuminati", "new world order",
            "they don't want you to know", "nu vor să știi", "wake up sheeple",
            "trezește-te", "flat earth", "pământ plat", "vaccines cause",
            "vaccinurile cauzează", "government control", "control guvernamental",
            "reptilian", "reptilieni", "chemtrails", "deep state",
            "fake news", "știri false", "matrix", "simulation theory",
            "ei ne controlează", "totul e o minciună"
        )
    ),

    // ── Well-being profile ────────────────────────────────────────────────────

    FERICIRE_POZITIV(
        "Fericire & Energie pozitivă", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "fericit", "fericită", "happy", "bucuros", "bucuroasă", "excited", "entuziasmat",
            "amazing", "super", "minunat", "wonderful", "cel mai bun", "best day",
            "best moment", "iubesc viața", "love life", "grateful", "recunoscător",
            "recunoscătoare", "mulțumit", "mulțumită", "reușit", "mândru", "mândră",
            "proud", "fericire", "happiness", "joy", "bucurie", "fain", "mișto",
            "extraordinar", "perfectă", "perfect day"
        )
    ),
    RELATII_ROMANTICE(
        "Relații romantice", CategoryType.INTEREST, ConcernLevel.NONE,
        listOf(
            "crush", "iubit", "iubită", "boyfriend", "girlfriend", "îndrăgostit",
            "îndrăgostită", "in love", "date", "sărut", "kiss", "couple", "cuplu",
            "relație", "valentines", "aniversare", "anniversary", "iubire", "love",
            "heart", "inimă", "romantic", "romantică", "sweetheart", "darling",
            "dragă", "dragul meu", "draga mea"
        )
    ),
    FURIE_FRUSTRARE(
        "Furie & Frustrare", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "furios", "furioasă", "enervat", "enervată", "nervos", "nervoasă",
            "frustrated", "angry", "rage", "furie", "urăsc totul", "hate everything",
            "sunt sătul", "sunt sătulă", "fed up", "mă enervează", "înnebunesc",
            "explodez", "nu mai suport", "totul e groaznic", "#angry", "#frustrated",
            "pissed off", "am înnebunit", "mă enervez", "suport", "nu pot suporta",
            "dau cu el de pereți", "dau cu ea de pereți"
        )
    ),
    SINGURATATE(
        "Singurătate", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "nu am pe nimeni", "no one cares", "nobody", "toți mă ignoră",
            "nu mă vede nimeni", "mă simt invizibil", "mă simt invizibilă",
            "fără prieteni", "no friends", "nimeni nu-mi scrie", "nimeni nu m-a sunat",
            "mă simt singur", "mă simt singură", "singurătate absolută",
            "nu există nimeni pentru mine", "nu am cu cine", "toți sunt ocupați",
            "sunt mereu singur", "sunt mereu singură", "nimeni nu mă înțelege",
            "mi-e dor de cineva", "nu am prieteni adevărați"
        )
    ),
    STRES_SCOLAR(
        "Stres școlar", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "bacalaureat", "bac", "teze", "teză", "examen greu", "pică examenul",
            "pică bac-ul", "am picat", "notă proastă", "nota proastă", "am luat 4",
            "am luat 3", "am luat 2", "am rămas corigent", "corigent", "corigenți",
            "stres la școală", "nu înțeleg nimic la", "prea greu la",
            "materia asta e imposibilă", "îmi urăsc școala", "hate school",
            "school is killing me", "can't do this anymore school",
            "nu pot cu tezele", "prea multe teme", "nu am dormit de teme"
        )
    ),
    PRESIUNE_GRUP(
        "Presiune de grup", CategoryType.CONCERN, ConcernLevel.MEDIUM,
        listOf(
            "toți fac asta", "everyone does it", "dacă nu faci ești",
            "if you don't you're lame", "fraier dacă nu", "ești fricos",
            "ești fricosă", "challenged", "dare", "ce frică îți e", "nu ți-e frică",
            "fii cool", "be cool", "toată lumea a încercat", "dovedește că poți",
            "prove it", "nu ești bărbat dacă nu", "nu ești femeie dacă nu",
            "hai că nu moare nimeni", "nu fi prost", "nu fi proastă",
            "fă și tu ca ceilalți", "toți ceilalți au făcut", "peer pressure"
        )
    );
}
