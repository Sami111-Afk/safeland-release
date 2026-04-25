import random

# Categories by concern level
LEVELS = {
    0: ["SPORT_MISCARE", "MUZICA_ARTE", "DANS", "GAMING", "STIINTA_NATURA", "EDUCATIE_LECTURA", "UMOR_ENTERTAINMENT", "GATIT_ALIMENTATIE", "ANIMALE_PETS", "FASHION_STYLE", "CALATORIE_CULTURA", "DIY_CREATIV", "SOCIAL_PRIETENII"],
    1: ["PRIVARE_SOMN", "MATERIALISM_FLEXING", "COMPARATII_FOMO"],
    2: ["TRISTETE_MELANCOLIE", "ANXIETATE_FRICA", "STIMA_SINE_SCAZUTA", "IZOLARE_SOCIALA", "HORROR_EXTREM", "BODY_IMAGE_NEGATIV", "FAKE_NEWS_CONSPIRATII"],
    3: ["BULLYING_VICTIMA", "BULLYING_AGRESOR", "URA_DISCRIMINARE", "CONTINUT_ARME", "CONTINUT_OCULT", "CHALLENGE_PERICULOS", "FUMAT_VAPING", "ACTIVITATI_ILEGALE", "DATE_PERSONALE", "GAMBLING"],
    4: ["CONTINUT_SUICIDAR", "AUTOMUTILARE", "CONTINUT_ADULT", "VIOLENTA_EXTREMA", "TULBURARI_ALIMENTARE", "ALCOOL_DROGURI", "RISC_GROOMING"]
}

# Example templates/phrases for each category
TEMPLATES = {
    "SPORT_MISCARE": {
        "EN": ["Incredible goal in the championship! #soccer #goal #win", "Morning workout routine. #fitness #gym #motivation", "Skateboarding session with the squad. #skate #shred"],
        "RO": ["Gol incredibil în campionat! #fotbal #gol #victorie", "Antrenament de dimineață. #fitness #sală #motivatie", "Sesiune de skate cu echipa. #skate #pasiune"]
    },
    "MUZICA_ARTE": {
        "EN": ["Check out my new guitar cover! #music #guitar #cover", "Painting process. #art #watercolor #artist", "Listening to my favorite album. #music #vibes"],
        "RO": ["Ascultă noul meu cover la chitară! #muzica #chitara #cover", "Procesul meu de pictură. #arta #acuarela #artist", "Ascult pe albumul preferat. #muzica #vibes"]
    },
    "DANS": {
        "EN": ["New dance challenge alert! #dance #challenge #trending", "Coreography practice. #choreography #dancer #energy", "Hip hop vibes today. #hiphop #dance"],
        "RO": ["Alertă de provocare de dans nouă! #dans #challenge #trending", "Practică de coregrafie. #coregrafie #dansator #energie", "Vibe-uri hip hop azi. #hiphop #dans"]
    },
    "GAMING": {
        "EN": ["Finally hit level 100 in Minecraft! #minecraft #gaming #level", "Roblox gameplay highlights. #roblox #gamer #fun", "New skin unlocked! #gaming #skin #achievement"],
        "RO": ["În sfârșit am ajuns la nivelul 100 în Minecraft! #minecraft #gaming #level", "Momente importante din jocul Roblox. #roblox #gamer #distractie", "Skin nou deblocat! #gaming #skin #realizare"]
    },
    "STIINTA_NATURA": {
        "EN": ["Check out this science experiment! #science #experiment #mindblown", "Nature walk today. #nature #animals #wildlife", "Space facts you didn't know. #space #science #universe"],
        "RO": ["Vezi acest experiment științific! #stiinta #experiment #uimitor", "Plimbare în natură azi. #natura #animale #salbatic", "Fapte despre spațiu pe care nu le știai. #spatiu #stiinta #univers"]
    },
    "EDUCATIE_LECTURA": {
        "EN": ["Highly recommend this book! #reading #books #library", "How to solve this math problem. #math #education #tutorial", "Did you know these historical facts? #history #education #facts"],
        "RO": ["Recomand cu căldură această carte! #lectura #carti #biblioteca", "Cum să rezolvi această problemă de matematică. #mate #educatie #tutorial", "Știai aceste fapte istorice? #istorie #educatie #curiozitati"]
    },
    "UMOR_ENTERTAINMENT": {
        "EN": ["Try not to laugh challenge! #funny #comedy #fail", "Funny prank on my best friend. #prank #humor #jokes", "Meme of the day. #meme #funny #comedy"],
        "RO": ["Provocare: încearcă să nu râzi! #amuzant #comedie #fail", "Farsă amuzantă făcută celui mai bun prieten. #farsa #umor #glume", "Meme-ul zilei. #meme #amuzant #comedie"]
    },
    "GATIT_ALIMENTATIE": {
        "EN": ["Easy pizza recipe. #cooking #food #pizza", "Making a delicious chocolate cake. #baking #dessert #cake", "Healthy breakfast smoothie. #food #healthy #smoothie"],
        "RO": ["Rețetă ușoară de pizza. #gatit #mancare #pizza", "Fac un tort de ciocolată delicios. #copt #desert #tort", "Smoothie sănătos pentru micul dejun. #mancare #sanatos #smoothie"]
    },
    "ANIMALE_PETS": {
        "EN": ["My dog is so cute! #pets #dog #adorable", "New kitten in the house. #cat #pets #kitten", "Funny animal videos to make you smile. #animals #funny #pets"],
        "RO": ["Câinele meu e atât de drăguț! #pets #dog #adorabil", "Pisicuță nouă în casă. #cat #pets #kitten", "Videoclipuri amuzante cu animale care să te facă să zâmbești. #animale #amuzant #pets"]
    },
    "FASHION_STYLE": {
        "EN": ["New outfit of the day. #fashion #style #ootd", "Summer haul! #shopping #fashion #clothes", "Accessorizing my look. #style #fashion #accessories"],
        "RO": ["Ținuta zilei. #fashion #stil #ootd", "Haul de vară! #cumparaturi #fashion #haine", "Accesorizarea look-ului meu. #stil #fashion #accesorii"]
    },
    "CALATORIE_CULTURA": {
        "EN": ["Exploring the mountains today. #travel #vacation #mountains", "Best holiday destination. #travel #vacation #beach", "Traditional festival vibes. #culture #travel #tradition"],
        "RO": ["Explorăm munții azi. #calatorie #vacanta #munti", "Cea mai bună destinație de vacanță. #calatorie #vacanta #plaja", "Vibe-uri de festival tradițional. #cultura #calatorie #traditie"]
    },
    "DIY_CREATIV": {
        "EN": ["Building a new LEGO set! #diy #lego #build", "How to make a handmade gift. #craft #diy #handmade", "3D printing process. #diy #creative #3dprint"],
        "RO": ["Construim un nou set LEGO! #diy #lego #construire", "Cum să faci un cadou lucrat manual. #craft #diy #handmade", "Proces de imprimare 3D. #diy #creativ #3dprint"]
    },
    "SOCIAL_PRIETENII": {
        "EN": ["Best friends forever! #friends #friendship #bff", "Party with the team. #party #fun #friends", "Helping our community today. #kindness #help #community"],
        "RO": ["Cei mai buni prieteni pentru totdeauna! #prieteni #prietenie #bff", "Petrecere cu echipa. #party #distractie #prieteni", "Ajutând comunitatea noastră azi. #bunatate #ajutor #comunitate"]
    },
    "PRIVARE_SOMN": {
        "EN": ["Another all nighter. 4am vibes. #nosleep #allnighter #tired", "Sleep is for the weak. #insomnia #nightowl #gaming", "Haven't slept in 24 hours. #nosleep #challenge"],
        "RO": ["Încă o noapte albă. Vibe-uri de ora 4 dimineața. #farasomn #toatanoaptea #obosit", "Somnul e pentru cei slabi. #insomnie #pasareadennoapte #gaming", "Nu am dormit de 24 de ore. #farasomn #challenge"]
    },
    "MATERIALISM_FLEXING": {
        "EN": ["New watch, who dis? #rich #flex #luxury", "Money is everything. #rich #cash #flexing", "Just bought this luxury car. #lamborghini #rich #flex"],
        "RO": ["Ceas nou, cine-i asta? #bogat #flex #lux", "Banii sunt totul. #bogat #cash #flexing", "Tocmai am cumpărat această mașină de lux. #lamborghini #bogat #flex"]
    },
    "COMPARATII_FOMO": {
        "EN": ["Why don't I have what they have? #fomo #jealous #lifestyle", "Everyone is at the party except me. #fomo #leftout", "I wish I was that popular. #popular #jealous #influencer"],
        "RO": ["De ce nu am ce au ei? #fomo #gelos #lifestyle", "Toată lumea e la petrecere, în afară de mine. #fomo #exclus", "Aș vrea să fiu la fel de popular. #popular #gelos #influencer"]
    },
    "TRISTETE_MELANCOLIE": {
        "EN": ["Feeling so alone tonight. Nobody cares. #sad #lonely #depression", "Dark thoughts again. #sadness #broken #mentalhealth", "I just want to disappear. #sad #depressed #lonely"],
        "RO": ["Mă simt atât de singur în seara asta. Nimănui nu-i pasă. #trist #singuratate #depresie", "Gânduri negre din nou. #tristete #distrus #mentalhealth", "Vreau doar să dispar. #trist #deprimat #singur"]
    },
    "ANXIETATE_FRICA": {
        "EN": ["Another panic attack. Can't breathe. #anxiety #panic #mentalhealth", "Always worried about everything. #anxiety #stressed #fear", "The fear of the future is real. #anxiety #scared #fear"],
        "RO": ["Încă un atac de panică. Nu pot respira. #anxietate #panica #mentalhealth", "Mereu îngrijorat de toate. #anxietate #stresat #friea", "Frica de viitor e reală. #anxietate #speriat #friea"]
    },
    "STIMA_SINE_SCAZUTA": {
        "EN": ["I hate my body. I'm so ugly. #selfhate #insecure #ugly", "I'm not good enough for anyone. #insecure #lowselfesteem", "Always comparing myself to others. #insecure #selfhate"],
        "RO": ["Îmi urăsc corpul. Sunt atât de urâtă. #selfhate #nesigura #urata", "Nu sunt suficient de bun pentru nimeni. #nesigur #stimadeinescazuta", "Mereu mă compar cu ceilalți. #nesigur #selfhate"]
    },
    "IZOLARE_SOCIALA": {
        "EN": ["I prefer to be alone. People are toxic. #alone #antisocial #lonely", "Staying inside forever. #stayhome #antisocial", "I don't need friends anymore. #alone #lonely"],
        "RO": ["Prefer să fiu singur. Oamenii sunt toxici. #singur #antisocial #singuratate", "Stau în casă pentru totdeauna. #staiacasa #antisocial", "Nu mai am nevoie de prieteni. #singur #singuratate"]
    },
    "HORROR_EXTREM": {
        "EN": ["Scary story time. Creepypasta. #horror #scary #nightmare", "This cursed image is terrifying. #horror #creepy #cursed", "I can't sleep after watching this. #horror #scary #nightmare"],
        "RO": ["Timpul pentru o poveste de groază. Creepypasta. #horror #infricosator #cosmar", "Această imagine blestemată este terifiantă. #horror #creepy #cursed", "Nu pot dormi după ce am văzut asta. #horror #infricosator #cosmar"]
    },
    "BODY_IMAGE_NEGATIV": {
        "EN": ["Need to lose weight fast. Too fat. #diet #weightloss #thinspo", "Counting every calorie. #diet #skinny #bodygoals", "Body checking again. #bodycheck #diet #thinspo"],
        "RO": ["Trebuie să slăbesc rapid. Prea grasă. #dieta #slabire #thinspo", "Număr fiecare calorie. #dieta #slaba #bodygoals", "Body checking din nou. #bodycheck #dieta #thinspo"]
    },
    "FAKE_NEWS_CONSPIRATII": {
        "EN": ["They are controlling us. Wake up! #conspiracy #illuminati #fake", "The truth about the government. #conspiracy #facts #truth", "Everything is a lie. #conspiracy #fake #simulation"],
        "RO": ["Ei ne controlează. Trezește-te! #conspiratie #illuminati #fake", "Adevărul despre guvern. #conspiratie #fapte #adevar", "Totul e o minciună. #conspiratie #fake #simulare"]
    },
    "BULLYING_VICTIMA": {
        "EN": ["Everyone mocks me at school. #bullying #bullied #sad", "Why do they hate me so much? #bullying #victim #sadness", "I'm always excluded. #bullying #lonely #sad"],
        "RO": ["Toți mă ridiculizează la școală. #bullying #haituit #trist", "De ce mă urăsc atât de mult? #bullying #victima #tristete", "Sunt mereu exclus. #bullying #singur #trist"]
    },
    "BULLYING_AGRESOR": {
        "EN": ["Look at this loser. Roast time! #fight #roast #bullying", "Let's destroy them. #fight #bully #destruction", "Bullying is actually fun. #fight #bullying #fun"],
        "RO": ["Uită-te la ratatul ăsta. E timpul pentru roast! #bataie #roast #bullying", "Să-i distrugem. #bataie #bully #distrugere", "Bullying-ul e chiar distractiv. #bataie #bullying #distractie"]
    },
    "URA_DISCRIMINARE": {
        "EN": ["They don't belong here. Go home. #racism #hate #discrimination", "Inferior people should leave. #hate #racism #discrimination", "No place for them in our country. #hate #racism"],
        "RO": ["Nu au ce căuta aici. Mergeți acasă. #rasism #ura #discriminare", "Oamenii inferiori ar trebui să plece. #ura #rasism #discriminare", "Niciun loc pentru ei în țara noastră. #ura #rasism"]
    },
    "CONTINUT_ARME": {
        "EN": ["My new gun collection. Ready to shoot. #guns #weapons #armed", "Check out this assault rifle. #guns #weapons #assault", "Illegal weapons deal. #guns #weapons #illegal"],
        "RO": ["Noua mea colecție de arme. Gata de tras. #arme #pistol #inarmat", "Vezi această pușcă de asalt. #arme #pistol #asalt", "Tranzacție ilegală de arme. #arme #pistol #ilegal"]
    },
    "CONTINUT_OCULT": {
        "EN": ["Secret ritual tonight. Join our cult. #occult #ritual #darkmagic", "Satanic spells for power. #occult #darkmagic #satanic", "Possessed by a demon. #occult #demon #possessed"],
        "RO": ["Ritual secret în seara asta. Alătură-te sectei noastre. #ocult #ritual #magieneagra", "Vrăji satanice pentru putere. #ocult #magieneagra #satanic", "Posedat de un demon. #ocult #demon #posedat"]
    },
    "CHALLENGE_PERICULOS": {
        "EN": ["Blackout challenge. Try it! #challenge #dangerous #stunt", "Fire challenge at home. #challenge #fire #dangerous", "Skull breaker challenge viral. #challenge #dangerous"],
        "RO": ["Blackout challenge. Încearcă-l! #challenge #periculos #stunt", "Fire challenge acasă. #challenge #fire #periculos", "Skull breaker challenge viral. #challenge #periculos"]
    },
    "FUMAT_VAPING": {
        "EN": ["Cool smoke rings. #vaping #vape #smoking", "Trag din noul meu juul. #vape #vaping #nicotine", "Smoking is a lifestyle. #smoking #cigarette #lifestyle"],
        "RO": ["Inele de fum mișto. #vaping #vape #fumat", "Trag din noul meu juul. #vape #vaping #nicotina", "Fumatul e un stil de viață. #fumat #tigara #lifestyle"]
    },
    "ACTIVITATI_ILEGALE": {
        "EN": ["Shoplifting haul today. #stealing #illegal #crime", "How to hack into accounts. #hack #illegal #crime", "Breaking into abandoned places. #trespassing #illegal #crime"],
        "RO": ["Haul de furat din magazine azi. #furt #ilegal #crima", "Cum să intri în conturi prin hack. #hack #ilegal #crima", "Intrarea ilegală în locuri părăsite. #ilegal #crima"]
    },
    "DATE_PERSONALE": {
        "EN": ["My real address is... Come visit! #address #private #location", "Here is my phone number. Call me. #phone #private #data", "I live on this street. #location #private #home"],
        "RO": ["Adresa mea reală este... Vino în vizită! #adresa #privat #locatie", "Iată numărul meu de telefon. Sună-mă. #telefon #privat #date", "Locuiesc pe strada asta. #locatie #privat #acasa"]
    },
    "GAMBLING": {
        "EN": ["Winning big at the casino! #gambling #bet #casino", "New betting strategy. #bet #gambling #win", "Loot box opening. Jackpot! #gambling #lootbox #jackpot"],
        "RO": ["Câștig mare la cazino! #gambling #pariu #casino", "Nouă strategie de pariere. #pariu #gambling #castig", "Deschidere de loot box. Jackpot! #gambling #lootbox #jackpot"]
    },
    "CONTINUT_SUICIDAR": {
        "EN": ["I want to end my life. Goodbye forever. #suicide #goodbye #broken", "No reason to live anymore. #suicide #death #sadness", "It would be better if I were dead. #suicide #goodbye"],
        "RO": ["Vreau să-mi închei viața. Adio pentru totdeauna. #sinucidere #adio #distrus", "Niciun motiv să mai trăiesc. #sinucidere #moarte #tristete", "Ar fi mai bine dacă aș fi mort. #sinucidere #adio"]
    },
    "AUTOMUTILARE": {
        "EN": ["Cutting again. Physical pain helps. #selfharm #cutting #scars", "Hurt myself tonight. #selfharm #wounds #pain", "New scars on my arm. #selfharm #cutting #pain"],
        "RO": ["Mă tai din nou. Durerea fizică ajută. #automutilare #taiat #cicatrici", "M-am rănit în seara asta. #automutilare #rani #durere", "Cicatrici noi pe braț. #automutilare #taiat #durere"]
    },
    "CONTINUT_ADULT": {
        "EN": ["Check out my explicit content. #nsfw #18plus #sexy", "Provocative video. #sexy #hot #adult", "Link in bio for more. #nsfw #adult #sexy"],
        "RO": ["Vezi conținutul meu explicit. #nsfw #18plus #sexy", "Video provocator. #sexy #hot #adult", "Link în bio pentru mai multe. #nsfw #adult #sexy"]
    },
    "VIOLENTA_EXTREMA": {
        "EN": ["Graphic violence video. Gore. #gore #kill #violence", "Brutal murder footage. #gore #violence #kill", "Extreme blood and death. #gore #brutal #death"],
        "RO": ["Video cu violență grafică. Gore. #gore #omor #violenta", "Imagini cu crime brutale. #gore #violenta #omor", "Sânge și moarte extremă. #gore #brutal #moarte"]
    },
    "TULBURARI_ALIMENTARE": {
        "EN": ["Pro ana for life. Not eating. #proana #anorexia #thinspo", "Skipping meals to stay skinny. #anorexia #bulimia #diet", "How to stop eating forever. #proana #thinspiration #diet"],
        "RO": ["Pro ana pe viață. Nu mănânc. #proana #anorexie #thinspo", "Sar peste mese ca să rămân slabă. #anorexie #bulimie #dieta", "Cum să nu mai mănânci niciodată. #proana #thinspiration #dieta"]
    },
    "ALCOOL_DROGURI": {
        "EN": ["Getting high tonight. Weed and pills. #drugs #high #stoned", "Drunk and wasted. #alcohol #drunk #party", "Buying drugs from my dealer. #drugs #high #illegal"],
        "RO": ["Mă droghez în seara asta. Iarbă și pastile. #droguri #high #stoned", "Beat și praf. #alcool #beat #petrecere", "Cumpăr droguri de la dealer. #droguri #high #ilegal"]
    },
    "RISC_GROOMING": {
        "EN": ["Meet me in secret. Don't tell your parents. #secret #meetup #grooming", "You're so mature for your age. Send photos. #secret #private #photos", "Don't tell anyone about our friendship. #secret #private #grooming"],
        "RO": ["Întâlnește-te cu mine în secret. Nu spune părinților. #secret #intalnire #grooming", "Ești atât de matură pentru vârsta ta. Trimite poze. #secret #privat #poze", "Nu spune nimănui despre prietenia noastră. #secret #privat #grooming"]
    }
}

def generate_row(level):
    categories = LEVELS[level]
    # Pick 1-3 categories
    num_cats = random.randint(1, min(3, len(categories)))
    selected_cats = random.sample(categories, num_cats)
    
    # 60% English, 40% Romanian
    lang = "EN" if random.random() < 0.6 else "RO"
    
    # Pick a template from one of the selected categories
    # Try to pick a category that has templates defined
    valid_cats = [c for c in selected_cats if c in TEMPLATES]
    if not valid_cats:
        # Fallback if no templates (shouldn't happen with the full list)
        text = "Generic text for " + "|".join(selected_cats)
    else:
        primary_cat = random.choice(valid_cats)
        text = random.choice(TEMPLATES[primary_cat][lang])
    
    # Add some randomness to text to avoid duplicates
    if random.random() < 0.3:
        text += " " + "".join(random.choices(["🔥", "✨", "💯", "🙌", "💀", "💔", "⚠️"], k=random.randint(1, 2)))
        
    return f'"{text}","{"|".join(selected_cats)}",{level}'

rows = []
for level in range(5):
    for _ in range(40):
        rows.append(generate_row(level))

# Shuffle all rows
random.shuffle(rows)

for row in rows:
    print(row)
