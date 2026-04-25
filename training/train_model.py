"""
DopamineTrap — Script training model text classification
=========================================================
Rulează în Google Colab (gratuit) sau local cu GPU.

Input:  training_data.csv  (exportat din aplicație)
Output: assets/model.tflite
        assets/vocab.json
        assets/labels.json

Pași:
  1. Urcă training_data.csv în Colab (sau pune calea locală)
  2. Rulează tot scriptul
  3. Descarcă cele 3 fișiere din /assets
  4. Pune-le în app/src/main/assets/ și rebuild

Colab setup:
  !pip install tensorflow pandas scikit-learn
"""

import json
import re
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MultiLabelBinarizer
from collections import Counter

# ── Configurare ───────────────────────────────────────────────────────────────

CSV_PATH    = "training_final.csv"
OUTPUT_DIR  = "assets"
MAX_VOCAB   = 8000
MAX_LEN     = 64
EMBED_DIM   = 64
EPOCHS      = 20
BATCH_SIZE  = 32
THRESHOLD   = 0.45   # trebuie să fie același ca în TFLiteClassifier.kt

import os
os.makedirs(OUTPUT_DIR, exist_ok=True)

# ── 1. Încărcare date ─────────────────────────────────────────────────────────

df = pd.read_csv(CSV_PATH, on_bad_lines='skip', engine='python')
print(f"Total events: {len(df)}")

# Filtrăm rânduri goale
df = df.dropna(subset=["rawText", "categories"])
df = df[df["rawText"].str.strip() != ""]
print(f"Events valide: {len(df)}")

# Parsăm categoriile (pipe-separated: "SPORT_MISCARE|DANS")
df["label_list"] = df["categories"].apply(lambda x: x.split("|"))

# ── 2. Encodare etichete ──────────────────────────────────────────────────────

mlb = MultiLabelBinarizer()
y = mlb.fit_transform(df["label_list"])
labels = list(mlb.classes_)
print(f"Categorii detectate: {len(labels)}")
print(f"  {labels}")

# Salvăm labels.json
with open(f"{OUTPUT_DIR}/labels.json", "w", encoding="utf-8") as f:
    json.dump(labels, f, ensure_ascii=False)
print(f"Salvat: {OUTPUT_DIR}/labels.json")

# ── 3. Tokenizare simplă ──────────────────────────────────────────────────────

def tokenize(text: str) -> list[str]:
    """Același algoritm ca în TFLiteClassifier.kt"""
    return re.split(r'[\s,!?.;:()\"/\\]+', text.lower())

def build_vocab(texts, max_vocab=MAX_VOCAB):
    counter = Counter()
    for text in texts:
        counter.update(tokenize(text))
    vocab = {"<PAD>": 0, "<OOV>": 1}
    for word, _ in counter.most_common(max_vocab - 2):
        vocab[word] = len(vocab)
    return vocab

def encode(text: str, vocab: dict, max_len: int) -> list[int]:
    tokens = tokenize(text)[:max_len]
    ids = [vocab.get(t, vocab["<OOV>"]) for t in tokens]
    ids += [0] * (max_len - len(ids))
    return ids

vocab = build_vocab(df["rawText"])
print(f"Vocabular: {len(vocab)} tokeni")

with open(f"{OUTPUT_DIR}/vocab.json", "w", encoding="utf-8") as f:
    json.dump(vocab, f, ensure_ascii=False)
print(f"Salvat: {OUTPUT_DIR}/vocab.json")

X = np.array([encode(t, vocab, MAX_LEN) for t in df["rawText"]], dtype=np.float32)

# ── 4. Split train/validation ─────────────────────────────────────────────────

X_train, X_val, y_train, y_val = train_test_split(
    X, y, test_size=0.15, random_state=42
)
print(f"Train: {len(X_train)} | Val: {len(X_val)}")

# ── 5. Arhitectura modelului ──────────────────────────────────────────────────

n_labels = len(labels)

model = tf.keras.Sequential([
    tf.keras.layers.Embedding(len(vocab), EMBED_DIM, input_length=MAX_LEN),
    tf.keras.layers.GlobalAveragePooling1D(),
    tf.keras.layers.Dense(128, activation="relu"),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Dense(64, activation="relu"),
    tf.keras.layers.Dense(n_labels, activation="sigmoid"),  # multi-label
])

model.compile(
    optimizer="adam",
    loss="binary_crossentropy",
    metrics=["accuracy", tf.keras.metrics.AUC(name="auc")]
)
model.summary()

# ── 6. Training ───────────────────────────────────────────────────────────────

callbacks = [
    tf.keras.callbacks.EarlyStopping(
        monitor="val_auc", patience=4, restore_best_weights=True, mode="max"
    ),
    tf.keras.callbacks.ReduceLROnPlateau(
        monitor="val_loss", patience=2, factor=0.5
    )
]

history = model.fit(
    X_train, y_train,
    validation_data=(X_val, y_val),
    epochs=EPOCHS,
    batch_size=BATCH_SIZE,
    callbacks=callbacks
)

val_auc = max(history.history["val_auc"])
print(f"\nBest val AUC: {val_auc:.4f}")

# ── 7. Export TFLite cu quantization INT8 ─────────────────────────────────────

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]  # INT8 quantization

# Representative dataset pentru calibrare INT8
def representative_data():
    for i in range(min(200, len(X_train))):
        yield [X_train[i:i+1]]

converter.representative_dataset = representative_data
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type  = tf.float32
converter.inference_output_type = tf.float32

tflite_model = converter.convert()

model_path = f"{OUTPUT_DIR}/model.tflite"
with open(model_path, "wb") as f:
    f.write(tflite_model)

size_kb = len(tflite_model) / 1024
print(f"\nSalvat: {model_path} ({size_kb:.1f} KB)")
print("\n=== Gata! ===")
print(f"Copiază aceste 3 fișiere în app/src/main/assets/ și rebuild:")
print(f"  {OUTPUT_DIR}/model.tflite")
print(f"  {OUTPUT_DIR}/vocab.json")
print(f"  {OUTPUT_DIR}/labels.json")

# ── 8. Verificare rapidă ──────────────────────────────────────────────────────

interpreter = tf.lite.Interpreter(model_path=model_path)
interpreter.allocate_tensors()
inp = interpreter.get_input_details()
out = interpreter.get_output_details()

test_text = "#swimming goals so proud"
test_input = np.array([encode(test_text, vocab, MAX_LEN)], dtype=np.float32)
interpreter.set_tensor(inp[0]["index"], test_input)
interpreter.invoke()
result = interpreter.get_tensor(out[0]["index"])[0]

print(f"\nTest: '{test_text}'")
detected = [(labels[i], float(result[i])) for i in range(len(labels)) if result[i] >= THRESHOLD]
detected.sort(key=lambda x: -x[1])
for label, score in detected:
    print(f"  {label}: {score:.3f}")
