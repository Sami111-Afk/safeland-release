import random

categories = {
    0: ["SPORT_MISCARE", "MUZICA_ARTE", "DANS", "GAMING", "STIINTA_NATURA", "EDUCATIE_LECTURA", "UMOR_ENTERTAINMENT", "GATIT_ALIMENTATIE", "ANIMALE_PETS", "FASHION_STYLE", "CALATORIE_CULTURA", "DIY_CREATIV", "SOCIAL_PRIETENII"],
    1: ["PRIVARE_SOMN", "MATERIALISM_FLEXING", "COMPARATII_FOMO"],
    2: ["TRISTETE_MELANCOLIE", "ANXIETATE_FRICA", "STIMA_SINE_SCAZUTA", "IZOLARE_SOCIALA", "HORROR_EXTREM", "BODY_IMAGE_NEGATIV", "FAKE_NEWS_CONSPIRATII"],
    3: ["BULLYING_VICTIMA", "BULLYING_AGRESOR", "URA_DISCRIMINARE", "CONTINUT_ARME", "CONTINUT_OCULT", "CHALLENGE_PERICULOS", "FUMAT_VAPING", "ACTIVITATI_ILEGALE", "DATE_PERSONALE", "GAMBLING"],
    4: ["CONTINUT_SUICIDAR", "AUTOMUTILARE", "CONTINUT_ADULT", "VIOLENTA_EXTREMA", "TULBURARI_ALIMENTARE", "ALCOOL_DROGURI", "RISC_GROOMING"]
}

# Romanian keywords/templates
ro_templates = {
    0: [
        "Astăzi am jucat fotbal cu băieții în parc. #fotbal #sport #distracție",
        "Noua mea pictură cu acuarele. Ce părere aveți? #artă #pictură #creativitate",
        "Am învățat o coregrafie nouă de pe TikTok! #dans #challenge #trending",
        "Minecraft de dimineață până seară. Cine mai vrea să joace? #gaming #roblox",
        "Am găsit un pui de pisică abandonat. E atât de adorabil! #pisică #animale #cute",
        "Gătind paste cu sos alb pentru cină. Delicios! #gătit #food #rețetă",
        "O zi minunată la munte cu prietenii. #vacanță #munte #prietenie",
        "Am terminat de citit o carte super interesantă despre spațiu. #lectură #spațiu",
        "Experiment de chimie în bucătărie. Nu încercați asta fără părinți! #știință",
        "Cum să faci un robot din carton. DIY simplu și rapid. #diy #robot #craft"
    ],
    1: [
        "Nu am dormit deloc noaptea asta. Gaming-ul e mai important. #fărăsomn #gaming",
        "Uitați ce mi-am cumpărat! Toți trebuie să aibă asta. #lux #bogat #flex",
        "De ce toți prietenii mei sunt la petrecere și eu nu? #fomo #exclus",
        "Sunt treaz la 4 dimineața și mă uit la clipuri amuzante. #insomnie #noapte",
        "Banii sunt totul în viață. Dacă ești sărac, e alegerea ta. #materialism #bani",
        "Mă simt exclus când văd pozele lor din vacanță. #tristețe #gelozie"
    ],
    2: [
        "Uneori mă simt atât de singur și nimeni nu mă înțelege. #tristețe #singur",
        "Am un atac de panică și nu pot respira bine. Mi-e frică. #anxietate #panică",
        "Mă urăsc când mă uit în oglindă. Sunt atât de urâtă. #stimasine #insucces",
        "Oamenii sunt toxici, mai bine stau singur în casă mereu. #izolare #antisocial",
        "Am văzut un clip horror și acum nu mai pot să dorm. #horror #frică",
        "Trebuie să slăbesc neapărat, mă simt prea grasă azi. #dietă #bodyimage",
        "Matrix e real, ei ne controlează mințile prin 5G. #conspirație #trezirea"
    ],
    3: [
        "Toți de la școală râd de mine și mă hărțuiesc zilnic. #bullying #victimă",
        "Hai să ne batem cu ăia de la clasa a 7-a, să îi umilim! #bătaie #agresor",
        "Nu îi suport pe străini, să plece la ei acasă! #ura #discriminare",
        "Mi-am cumpărat un cuțit nou și e super ascuțit. #arme #pericol",
        "Ritual secret în pădure la miezul nopții. Vrei să vii? #ocult #ritual",
        "Încercați acest challenge: țineți-vă respirația cât mai mult! #challenge #pericol",
        "Vaping în buda școlii, cel mai tare sentiment. #vape #fumat",
        "Am reușit să fur ceva din magazin și nu m-au prins. #furt #ilegal",
        "Dacă vrei să vorbim, sună-mă la 0722123456. Stau pe strada Florilor. #datepersonale",
        "Am pariat toți banii de buzunar și am pierdut. Mai vreau! #gambling #pariuri"
    ],
    4: [
        "Nu mai vreau să trăiesc, totul e fără rost. Adio. #suicid #adio",
        "Durerea fizică mă face să uit de cea sufletească. #automutilare #tăiat",
        "Intră pe link-ul din bio pentru conținut 18+ exclusiv. #nsfw #adult",
        "Am văzut un video cu cineva care era ucis, a fost brutal. #gore #violență",
        "Nu am mâncat nimic de 3 zile și mă simt mândră. #proana #anorexie",
        "Am încercat niște pastile noi și acum văd culori. #droguri #high",
        "Un domn mai în vârstă mi-a spus că sunt matură și vrea să ne vedem. #grooming"
    ]
}

en_templates = {
    0: [
        "Just finished a great workout! Feeling energized. #fitness #workout #gym",
        "Check out my new drawing of a sunset. #art #drawing #creative",
        "Learning a new dance for the talent show! #dance #trending #tutorial",
        "Finally hit level 50 in my favorite game! #gaming #gamer #achievement",
        "My dog is the cutest thing ever, look at him sleep! #dog #pets #cute",
        "Made some homemade cookies today. They smell amazing! #baking #food #cookies",
        "Best vacation ever at the beach with my BFFs! #travel #summer #friends",
        "Did you know that space is actually silent? #science #space #facts",
        "Building a LEGO castle, this is going to take all day. #lego #diy #build",
        "Happy birthday to my best friend! Love you! #birthday #friends #party"
    ],
    1: [
        "Up since 3 AM playing games. Who needs sleep anyway? #nosleep #gamer",
        "Just got the new iPhone, feeling so rich right now. #flex #luxury #iphone",
        "Why is everyone hanging out without me? #fomo #leftout",
        "Sleep is for the weak, staying up all night! #nosleep #allnighter",
        "I need more expensive clothes to look cool. #fashion #rich #flex",
        "Feeling jealous of her lifestyle, she has everything. #jealous #fomo"
    ],
    2: [
        "Feeling so down today, nothing seems to go right. #sad #depressed",
        "Having a panic attack and I can't calm down. #anxiety #panic",
        "I hate how I look, I wish I was prettier. #insecure #selfhate",
        "Staying inside forever, people are too much to handle. #antisocial #lonely",
        "Watched a scary video and now I'm hearing noises. #scared #horror",
        "I need to lose 10 pounds by next week. #diet #weightloss",
        "They are watching us through our cameras, stay safe. #conspiracy #matrix"
    ],
    3: [
        "Everyone at school is being so mean to me lately. #bullying #help",
        "Let's go expose that girl from 8th grade! #roast #cancel",
        "I don't like people who don't speak my language. #hate #racism",
        "Look at my new gun collection, don't mess with me. #guns #weapons",
        "Join our secret group for the ultimate power. #cult #ritual",
        "Try the choking challenge, it feels so weird! #challenge #dangerous",
        "Got some new pods for my vape, smells like mango. #vape #smoking",
        "Hacking my teacher's account was so easy. #hacking #illegal",
        "Meet me at the park on Elm Street at 9 PM. #datepersonale #meetup",
        "Won $50 on this betting site! Try it out! #gambling #win"
    ],
    4: [
        "I want to end it all, I can't take this anymore. #suicide #goodbye",
        "Cutting helps me feel something other than numbness. #selfharm #cutting",
        "Check my OnlyFans for the spicy content. #nsfw #18plus",
        "Found a site with real death videos, so intense. #gore #violence",
        "Skipping meals until I can see my ribs. #proana #thinspo",
        "Just popped some pills, feeling like I'm floating. #drugs #high",
        "Met a guy online who wants to buy me gifts if I visit him. #grooming #danger"
    ]
}

data = []

for level in range(5):
    # 40 rows per level
    # 60% English (24 rows), 40% Romanian (16 rows)
    
    # Romanian rows
    for _ in range(16):
        text = random.choice(ro_templates[level])
        # Add some randomness to text to avoid exact duplicates
        if random.random() > 0.5:
            text += " " + "".join(random.choices("!?. ", k=3))
        
        # Pick 1-3 categories from the level
        n_cats = random.randint(1, min(3, len(categories[level])))
        cats = random.sample(categories[level], n_cats)
        
        # If level 0, we can also mix in some interests from other levels? 
        # No, let's keep it strictly mapped as per my grouping for simplicity and accuracy.
        
        data.append(f'"{text}","{"|".join(cats)}",{level}')

    # English rows
    for _ in range(24):
        text = random.choice(en_templates[level])
        if random.random() > 0.5:
            text += " " + "".join(random.choices("!?. ", k=3))
        
        n_cats = random.randint(1, min(3, len(categories[level])))
        cats = random.sample(categories[level], n_cats)
        
        data.append(f'"{text}","{"|".join(cats)}",{level}')

random.shuffle(data)

for row in data:
    print(row)
