package com.tuuli.monequilibresync

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val Background = Color(0xFFFBF7F5)
private val PowderPink = Color(0xFFE8CDD5)
private val MatchaLight = Color(0xFFE1E4B4)
private val Matcha = Color(0xFF98A45F)
private val WarmCream = Color(0xFFF1E6CE)
private val TextDark = Color(0xFF3C3537)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Matcha,
                    onPrimary = Color.White,
                    background = Background,
                    onBackground = TextDark,
                    surface = Background,
                    onSurface = TextDark,
                    secondaryContainer = PowderPink,
                    tertiaryContainer = WarmCream,
                )
            ) {
                Surface(color = Background, modifier = Modifier.fillMaxSize()) {
                    SyncScreen()
                }
            }
        }
    }
}

@Composable
private fun SyncScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = viewModel::onPermissionsResult,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Mon équilibre Sync",
                style = MaterialTheme.typography.headlineMedium,
                color = TextDark,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Samsung Health → Health Connect → Mon équilibre",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (state.initializing) {
            item {
                StatusCard("Initialisation…") {
                    CircularProgressIndicator(color = Matcha)
                }
            }
            return@LazyColumn
        }

        if (!state.signedIn) {
            item {
                LoginCard(
                    busy = state.busy,
                    error = state.error,
                    onSignIn = viewModel::signIn,
                )
            }
            return@LazyColumn
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WarmCream),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Compte Mon équilibre", fontWeight = FontWeight.SemiBold, color = TextDark)
                    Text(state.userEmail ?: "Compte connecté", color = TextDark.copy(alpha = 0.7f))
                    OutlinedButton(
                        onClick = viewModel::signOut,
                        enabled = !state.busy,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Se déconnecter")
                    }
                }
            }
        }

        when (state.healthAvailability) {
            HealthConnectAvailability.Unavailable -> item {
                StatusCard("Health Connect n’est pas disponible sur cet appareil.")
            }

            HealthConnectAvailability.ProviderUpdateRequired -> item {
                StatusCard("Health Connect doit être installé ou mis à jour.") {
                    Button(
                        onClick = {
                            val provider = "com.google.android.apps.healthdata"
                            val uri = Uri.parse(
                                "market://details?id=$provider&url=healthconnect%3A%2F%2Fonboarding"
                            )
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Matcha),
                    ) {
                        Text("Ouvrir Google Play")
                    }
                }
            }

            HealthConnectAvailability.Available -> {
                if (!state.permissionsGranted) {
                    item {
                        StatusCard("Autorisez uniquement : pas, exercices et distance.") {
                            Button(
                                onClick = { permissionLauncher.launch(viewModel.permissions) },
                                colors = ButtonDefaults.buttonColors(containerColor = Matcha),
                            ) {
                                Text("Autoriser Health Connect")
                            }
                        }
                    }
                } else {
                    val snapshot = state.snapshot
                    if (snapshot != null) {
                        item { StepsCard(snapshot.steps) }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Séances aujourd’hui",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextDark,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${snapshot.exercises.size}",
                                    color = Matcha,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        if (snapshot.exercises.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = WarmCream),
                                    shape = RoundedCornerShape(22.dp),
                                ) {
                                    Text(
                                        "Aucune séance enregistrée aujourd’hui dans Health Connect.",
                                        color = TextDark,
                                        modifier = Modifier.padding(18.dp),
                                    )
                                }
                            }
                        } else {
                            items(snapshot.exercises, key = { it.recordId }) { exercise ->
                                ExerciseCard(exercise)
                            }
                        }

                        item {
                            Button(
                                onClick = viewModel::syncNow,
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Matcha),
                            ) {
                                if (state.busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(18.dp),
                                        color = Color.White,
                                    )
                                } else {
                                    Text("Synchroniser maintenant")
                                }
                            }
                            OutlinedButton(
                                onClick = viewModel::refresh,
                                enabled = !state.busy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                Text("Relire Health Connect")
                            }
                        }
                    }
                }
            }

            null -> Unit
        }

        state.message?.let { message ->
            item { StatusCard(message) }
        }
        state.error?.let { error ->
            item { StatusCard("Erreur : $error") }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun LoginCard(
    busy: Boolean,
    error: String?,
    onSignIn: (String, String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PowderPink),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Connexion à Mon équilibre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
            )
            Text(
                "Utilisez le même compte que dans l’application web.",
                color = TextDark.copy(alpha = 0.72f),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("E-mail") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mot de passe") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Button(
                onClick = { onSignIn(email, password) },
                enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Matcha),
            ) {
                Text(if (busy) "Connexion…" else "Se connecter")
            }
            if (error != null) {
                Text(error, color = TextDark, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatusCard(message: String, content: @Composable (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PowderPink),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(message, color = TextDark, style = MaterialTheme.typography.bodyLarge)
            content?.invoke()
        }
    }
}

@Composable
private fun StepsCard(steps: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MatchaLight),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("Pas aujourd’hui", color = TextDark.copy(alpha = 0.72f))
            Text(
                text = String.format(Locale.FRENCH, "%,d", steps).replace(',', ' '),
                style = MaterialTheme.typography.displaySmall,
                color = TextDark,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Total agrégé par Health Connect",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun ExerciseCard(exercise: ExerciseSummary) {
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.FRENCH)
        .withZone(ZoneId.systemDefault())
    val distanceKm = exercise.distanceMeters / 1000.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                exercise.label,
                color = TextDark,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${time.format(exercise.startTime)} – ${time.format(exercise.endTime)} · ${exercise.durationMinutes} min",
                color = TextDark.copy(alpha = 0.72f),
            )
            Text(
                if (exercise.distanceMeters > 0.0) {
                    String.format(Locale.FRENCH, "%.2f km", distanceKm)
                } else {
                    "Distance non fournie"
                },
                color = Matcha,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Source : ${friendlySource(exercise.sourcePackage)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.55f),
            )
        }
    }
}

private fun friendlySource(packageName: String): String = when (packageName) {
    "com.sec.android.app.shealth" -> "Samsung Health"
    "com.google.android.apps.fitness" -> "Google Fit"
    else -> packageName.ifBlank { "Health Connect" }
}
