package com.sol.dopaminetrap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.OnboardingManager
import com.sol.dopaminetrap.OnboardingManager.DeviceMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

private fun generatePairingCode(): String =
    (100000..999999).random().toString()

// Pasul 1 — alegere mod
@Composable
fun ModeSelectionScreen(onModeSelected: (DeviceMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Safeland",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Cine foloseste acest telefon?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { onModeSelected(DeviceMode.PARENT) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Sunt parinte", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { onModeSelected(DeviceMode.CHILD) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Sunt copil / Configureaza pentru copil", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Pasul 2a — copil: introduce numele, genereaza cod si scrie in Firestore
@Composable
fun ChildSetupScreen(onDone: (familyId: String, childId: String, childName: String) -> Unit) {
    val scope = rememberCoroutineScope()

    var childName by remember { mutableStateOf("") }
    var nameConfirmed by remember { mutableStateOf(false) }

    var pairingCode by remember { mutableStateOf("") }
    var familyId by remember { mutableStateOf("") }
    var childId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun generateCode(name: String) {
        isLoading = true
        error = null
        scope.launch {
            runCatching {
                val fid = UUID.randomUUID().toString()
                val cid = UUID.randomUUID().toString()
                val code = generatePairingCode()
                Firebase.firestore.collection("pairing").document(code).set(
                    mapOf(
                        "familyId" to fid,
                        "childId" to cid,
                        "childName" to name,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                // Creeaza documentul copilului in Firestore
                FirebaseRepository.registerChild(fid, cid, name)
                familyId = fid
                childId = cid
                pairingCode = code
            }.onFailure {
                error = "Eroare la generarea codului. Verifica conexiunea la internet."
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Configureaza pentru copil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (!nameConfirmed) {
            Text(
                "Cum se numeste copilul?",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = childName,
                onValueChange = { childName = it },
                label = { Text("Prenumele copilului") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    nameConfirmed = true
                    generateCode(childName.trim().ifEmpty { "Copil" })
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Continua", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Text(
                "Da codul de mai jos parintelui. El il va introduce in aplicatia lui pentru a se conecta.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(40.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { generateCode(childName.trim().ifEmpty { "Copil" }) }) {
                    Text("Incearca din nou")
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        pairingCode,
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 8.sp
                        ),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = { onDone(familyId, childId, childName.trim().ifEmpty { "Copil" }) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Parintele a introdus codul — Continua")
                }
            }
        }
    }
}

// Pasul 2b — parinte: introduce codul copilului
@Composable
fun ParentSetupScreen(onDone: (familyId: String, childId: String, childName: String) -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Conecteaza-te la copil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Introdu codul de 6 cifre afisat pe telefonul copilului.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
            label = { Text("Cod de 6 cifre") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 28.sp,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (code.length != 6) {
                    error = "Codul trebuie sa aiba 6 cifre."
                    return@Button
                }
                isLoading = true
                error = null
                scope.launch {
                    runCatching {
                        val doc = Firebase.firestore.collection("pairing").document(code).get().await()
                        if (!doc.exists()) throw Exception("Cod invalid sau expirat.")
                        val fid = doc.getString("familyId") ?: throw Exception("Date corupte.")
                        val cid = doc.getString("childId") ?: throw Exception("Date corupte.")
                        val name = doc.getString("childName") ?: "Copil"
                        onDone(fid, cid, name)
                    }.onFailure {
                        error = it.message ?: "Eroare necunoscuta."
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && code.length == 6,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (isLoading) CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            else Text("Conecteaza", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Ecran principal de onboarding — orchestreaza flow-ul
@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf<DeviceMode?>(null) }

    when (selectedMode) {
        null -> ModeSelectionScreen(onModeSelected = { selectedMode = it })

        DeviceMode.CHILD -> ChildSetupScreen(onDone = { familyId, childId, childName ->
            OnboardingManager.completeOnboarding(context, DeviceMode.CHILD, familyId, childId, childName)
            onOnboardingComplete()
        })

        DeviceMode.PARENT -> ParentSetupScreen(onDone = { familyId, childId, childName ->
            OnboardingManager.completeOnboarding(context, DeviceMode.PARENT, familyId, childId, childName)
            onOnboardingComplete()
        })
    }
}
