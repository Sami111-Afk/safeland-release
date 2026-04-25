import random

# Subtipuri și exemple de bază pentru diversificare
subtipuri = {
    1: [ # Adulații excesive / construire relație falsă
        "Ești mult mai matură decât fetele de vârsta ta.",
        "Nimeni nu te înțelege așa cum te simt eu că mă înțelegi.",
        "Mă faci să mă simt atât de bine când vorbim, ești specială.",
        "Ai un suflet atât de frumos, rar am întâlnit pe cineva ca tine.",
        "Ești singura persoană căreia îi pot spune totul.",
        "Simt că avem o conexiune magică, ești incredibilă.",
        "Nu pot să nu mă gândesc la tine toată ziua, ești uimitoare.",
        "Ești micuța mea preferată, mă faci mereu să zâmbesc.",
        "Vreau să te ajut să îți împlinești toate visurile, meriți totul.",
        "Ești genul de persoană care merită să fie răsfățată non-stop."
    ],
    2: [ # Solicitare poze / descrieri fizice
        "Trimite-mi o poză cu tine să văd ce mai faci.",
        "Sunt curios cum ești îmbrăcată acum, îmi arăți?",
        "Fă-ți un selfie rapid, mi-e dor de chipul tău.",
        "Trimite-mi o poză mai specială, doar pentru ochii mei.",
        "Ce haine porți azi la școală? Arăți bine în ele?",
        "Vreau să văd cât de mult ai crescut, trimite-mi o poză întreagă.",
        "Arată-mi ce porți pe sub geacă, sunt curios.",
        "Trimite-mi o poză în oglindă, vreau să te văd acum.",
        "Poți să îmi trimiți o poză mai... relaxată? Știi tu.",
        "Vreau o poză cu tine în pat, înainte de culcare."
    ],
    3: [ # Propunere întâlnire fizică
        "Hai să ne vedem diseară în parc, când nu e multă lume.",
        "Știu eu un loc liniștit unde putem vorbi în voie fără să ne deranjeze nimeni.",
        "Te iau eu cu mașina după ore și mergem să ne plimbăm.",
        "Vreau să îți dau cadoul ăla despre care ți-am zis, ne vedem?",
        "Hai să mergem la un film, doar noi doi.",
        "Te aștept la colțul străzii la ora 8, să nu întârzii.",
        "Putem să ne vedem la mine acasă, părinții mei nu sunt aici.",
        "Hai să facem o plimbare lungă, am multe să îți povestesc față în față.",
        "Dacă vrei, putem merge la o înghețată undeva mai retras.",
        "Abia aștept să te strâng în brațe, când ne vedem?"
    ],
    4: [ # Cerere de secret față de părinți
        "Să nu spui nimănui despre ce vorbim noi aici, e secretul nostru.",
        "Părinții tăi sunt prea severi, nu ar înțelege prietenia noastră.",
        "E mai bine dacă ștergi mesajele astea după ce le citești.",
        "Dacă află cineva, nu o să ne mai lase să vorbim deloc.",
        "Promite-mi că rămâne între noi, e ceva special.",
        "Mama ta ar fi geloasă dacă ar ști cât de bine ne înțelegem.",
        "Nu e treaba nimănui ce facem noi pe internet.",
        "Ascunde telefonul ca să nu vadă nimeni ce scriem.",
        "Când te întreabă cu cine vorbești, zi-le că e o colegă.",
        "Oamenii mari sunt răi și vor să ne strice bucuria, taci."
    ],
    5: [ # Izolare de prieteni/familie
        "Prietenele tale nu te merită, sunt doar invidioase pe tine.",
        "Familia ta nu te prețuiește la adevărata ta valoare, dar eu da.",
        "De ce mai stai cu ei? Eu sunt singurul care te ajută mereu.",
        "Toți ceilalți vor doar să te folosească, eu chiar țin la tine.",
        "Nu ai nevoie de alții când mă ai pe mine lângă tine.",
        "Vezi cum te tratează? Eu nu m-aș purta niciodată așa cu tine.",
        "Ignoră-i pe toți, doar noi doi contăm cu adevărat.",
        "Ești prea bună pentru cercul tău de prieteni de la școală.",
        "O să vezi că în afară de mine, nimeni nu îți vrea binele.",
        "Ei nu înțeleg ce simți tu, doar eu pot să te ascult cu adevărat."
    ]
}

variante = [
    "Vreau să știi că ", "Sincer, ", "Ascultă, ", "Mă gândeam că ", "Hey, ", "Uite ce e, ", "", "", "", ""
]

mesaje_finale = []

for i in range(1, 6):
    baza = subtipuri[i]
    for _ in range(40):
        m = random.choice(baza)
        # Adăugăm mici variații pentru a nu fi identice
        prefix = random.choice(variante)
        suffix = random.choice(["", "...", "!", "?", " :)", " acum", " te rog"])
        mesaj_complet = (prefix + m + suffix).strip()
        # Eliminăm ghilimelele dacă au apărut accidental
        mesaj_complet = mesaj_complet.replace('"', '').replace("'", "")
        mesaje_finale.append(f"{mesaj_complet},GROOMING,4")

random.shuffle(mesaje_finale)

with open("/home/sol/Downloads/grooming_extra.csv", "w", encoding="utf-8") as f:
    for linie in mesaje_finale:
        f.write(linie + "\n")
