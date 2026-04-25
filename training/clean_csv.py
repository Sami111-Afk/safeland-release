"""
Curăță training_final.csv — păstrează doar rândurile cu categorii valide.
Rulează: python clean_csv.py
Output: training_clean.csv
"""

import pandas as pd
import re

VALID_LABELS = {
    "SPORT_MISCARE", "MUZICA_ARTE", "GAMING", "STIINTA_NATURA",
    "EDUCATIE_LECTURA", "ANIMALE_PETS", "FASHION_STYLE", "SOCIAL_PRIETENII",
    "ALCOOL_DROGURI", "GAMBLING", "CALATORIE_CULTURA", "UMOR_ENTERTAINMENT",
    "DIY_CREATIV", "GATIT_ALIMENTATIE", "DANS", "FUMAT_VAPING",
    "BULLYING_AGRESOR", "BULLYING_VICTIMA", "VIOLENTA_EXTREMA",
    "CONTINUT_SEXUAL", "CONTINUT_OCULT", "SUICID", "AUTOMUTILARE",
    "ANXIETATE_STRES", "TRISTETE_MELANCOLIE", "IZOLARE_RETRAGERE",
    "CONFLICTE_FAMILIALE", "BODY_IMAGE_NEGATIV", "TULBURARI_ALIMENTARE",
    "RUSINEA_CORP", "STIMA_SINE_SCAZUTA", "HORROR_EXTREM",
    "COMPARATII_FOMO", "MATERIALISM_FLEXING", "URA_DISCRIMINARE",
    "ACTIVITATI_ILEGALE", "GROOMING"
}

def is_valid_categories(cat_str):
    if not isinstance(cat_str, str) or not cat_str.strip():
        return False
    labels = [l.strip() for l in cat_str.split("|") if l.strip()]
    if not labels:
        return False
    return all(l in VALID_LABELS for l in labels)

df = pd.read_csv("training_final.csv", on_bad_lines='skip', engine='python')
print(f"Total rânduri: {len(df)}")

df = df.dropna(subset=["rawText", "categories"])
df = df[df["rawText"].str.strip() != ""]
df = df[df["categories"].apply(is_valid_categories)]

print(f"Rânduri valide: {len(df)}")

# Statistici per categorie
from collections import Counter
counter = Counter()
for cats in df["categories"]:
    for c in cats.split("|"):
        counter[c.strip()] += 1

print("\nDistributie categorii:")
for label, count in sorted(counter.items(), key=lambda x: -x[1]):
    print(f"  {label}: {count}")

df.to_csv("training_clean.csv", index=False)
print(f"\nSalvat: training_clean.csv")
