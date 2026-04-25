const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

const groqApiKey = defineSecret("GROQ_API_KEY");

initializeApp();
setGlobalOptions({ region: "europe-west1" });

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function getParentFcmToken(familyId) {
  const doc = await getFirestore()
    .collection("families").doc(familyId)
    .collection("config").doc("parent")
    .get();
  return doc.data()?.fcmToken ?? null;
}

async function getChildName(familyId, childId) {
  const doc = await getFirestore()
    .collection("families").doc(familyId)
    .collection("children").doc(childId)
    .get();
  return doc.data()?.name ?? "Copilul";
}

async function callGroq(apiKey, prompt) {
  const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "llama-3.1-8b-instant",
      messages: [{ role: "user", content: prompt }],
      max_tokens: 300,
      temperature: 0.7,
    }),
  });
  if (!response.ok) throw new Error(`Groq HTTP ${response.status}`);
  const data = await response.json();
  return data.choices?.[0]?.message?.content?.trim() ?? "";
}

function parseJson(text) {
  const cleaned = text.replace(/```json\n?/g, "").replace(/```\n?/g, "").trim();
  return JSON.parse(cleaned);
}

// ─── Alerta critica ───────────────────────────────────────────────────────────

exports.sendCriticalAlert = onDocumentCreated(
  { document: "families/{familyId}/children/{childId}/alerts/{alertId}", secrets: [groqApiKey] },
  async (event) => {
    const { familyId, childId } = event.params;
    const alert = event.data.data();
    if (!alert) return;

    const allCategories = alert.allCategories || alert.category || "";
    const sourceApp = alert.sourceApp || "aplicatie";
    const sourceType = alert.sourceType || "content";
    const isMessaging = sourceType === "messaging";

    const parentToken = await getParentFcmToken(familyId);
    if (!parentToken) {
      console.log(`Nu exista token FCM pentru familia ${familyId}`);
      return;
    }

    const childName = await getChildName(familyId, childId);

    // Evaluare AI: merita trimisa alerta sau e repetitie normala?
    let shouldSend = true;
    let alertReason = `${allCategories} detectat in ${sourceApp}`;

    try {
      const context = isMessaging
        ? `Un copil de 6-12 ani a primit sau trimis mesaje cu conținut de tip "${allCategories}" în ${sourceApp}. Acest lucru s-a repetat de ${isMessaging ? 2 : 5} ori în ultimele ${isMessaging ? "5" : "10"} minute.`
        : `Un copil de 6-12 ani a consumat conținut de tip "${allCategories}" în ${sourceApp} de 5 ori în ultimele 10 minute.`;

      const prompt = `${context}

Evaluează dacă această alertă merită trimisă imediat părintelui sau e repetitie/zgomot normal. Răspunde DOAR cu JSON valid:
{
  "send": true,
  "reason": "mesaj empatic de maxim 1-2 propoziții în română pentru părinte, specificând dacă e vorba de mesaje primite/trimise sau conținut consumat"
}`;

      const raw = await callGroq(groqApiKey.value(), prompt);
      const result = parseJson(raw);
      shouldSend = result.send !== false;
      if (result.reason) alertReason = result.reason;
      console.log(`Evaluare alerta AI: send=${shouldSend}, reason="${alertReason}"`);
    } catch (err) {
      console.error("Groq evaluare alerta a esuat, trimit oricum:", err.message);
      // Fallback: trimitem alerta — mai bine prea mult decat sa ratam ceva critic
    }

    if (!shouldSend) {
      console.log(`Alerta ignorata de AI pentru ${familyId}/${childId}: ${alertReason}`);
      return;
    }

    await getMessaging().send({
      token: parentToken,
      notification: {
        title: `Alerta DopamineTrap — ${childName}`,
        body: alertReason,
      },
      data: { type: "alert", familyId, childId, alertId: event.params.alertId },
      android: {
        priority: "high",
        notification: { channelId: "dopamine_alerts", priority: "max", defaultSound: "true" },
      },
    }).catch((err) => console.error("Eroare FCM alerta:", err));

    console.log(`Alerta trimisa parintelui (${familyId}/${childId}): ${alertReason}`);
  }
);

// ─── Raport saptamanal ────────────────────────────────────────────────────────

exports.sendWeeklyReport = onDocumentCreated(
  "families/{familyId}/children/{childId}/reports/{reportId}",
  async (event) => {
    const { familyId, childId } = event.params;
    const report = event.data.data();
    if (!report) return;

    const parentToken = await getParentFcmToken(familyId);
    if (!parentToken) return;

    const childName = await getChildName(familyId, childId);
    const title = report.title || "Raport saptamanal";
    const message = report.message || "";

    await getMessaging().send({
      token: parentToken,
      notification: { title: `${childName}: ${title}`, body: message },
      data: { type: "weekly_report", familyId, childId, reportId: event.params.reportId },
      android: {
        priority: "normal",
        notification: { channelId: "dopamine_reports", priority: "default", defaultSound: "true" },
      },
    }).catch((err) => console.error("Eroare FCM raport:", err));

    console.log(`Raport saptamanal trimis parintelui (${familyId}/${childId})`);
  }
);

// ─── Sugestie AI pentru raport ────────────────────────────────────────────────

exports.generateAiSuggestion = onDocumentCreated(
  { document: "families/{familyId}/children/{childId}/reports/{reportId}", secrets: [groqApiKey] },
  async (event) => {
    const report = event.data.data();
    if (!report) return;

    const interests = report.interests || [];
    const concerns = report.concerns || [];
    const concernLevel = report.concernLevel || "NONE";

    console.log(`generateAiSuggestion: interests=${JSON.stringify(interests)}, concerns=${JSON.stringify(concerns)}, level=${concernLevel}`);

    if (interests.length === 0 && concerns.length === 0) {
      console.log("Ambele liste goale — skip Groq");
      return;
    }

    const interestsStr = interests.length > 0 ? interests.join(", ") : "niciuna detectată";
    const concernsStr = concerns.length > 0 ? concerns.join(", ") : "niciuna";

    const prompt = `Ești un asistent empatic pentru părinți. Copilul lor de 6-12 ani a urmărit conținut digital cu aceste categorii săptămâna aceasta:
- Interese principale: ${interestsStr}
- Îngrijorări detectate: ${concernsStr} (nivel maxim: ${concernLevel})

Generează un raport săptămânal în română. Răspunde DOAR cu JSON valid, fără alte explicații:
{
  "title": "titlu scurt maxim 8 cuvinte",
  "message": "2-3 propoziții empatice despre ce a consumat copilul această săptămână",
  "suggestion": "o sugestie concretă 1-2 propoziții ce poate face părintele"
}`;

    try {
      const raw = await callGroq(groqApiKey.value(), prompt);
      const result = parseJson(raw);

      const update = {};
      if (result.title) update.title = result.title;
      if (result.message) update.message = result.message;
      if (result.suggestion) update.suggestion = result.suggestion;

      if (Object.keys(update).length > 0) {
        await event.data.ref.update(update);
        console.log(`Raport AI salvat pentru ${event.params.familyId}/${event.params.childId}`);
      }
    } catch (err) {
      console.error("Groq raport esuat, raman valorile hardcodate:", err.message);
    }
  }
);
