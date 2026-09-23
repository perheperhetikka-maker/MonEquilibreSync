package com.tuuli.monequilibresync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF98A45F),
                    background = Color(0xFFFBF7F5),
                    onBackground = Color(0xFF3C3537),
                    surface = Color(0xFFFBF7F5),
                    onSurface = Color(0xFF3C3537),
                )
            ) {
                Surface(color = Color(0xFFFBF7F5), modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp)) {
                        Text(
                            "Confidentialité — Mon équilibre Sync",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF3C3537),
                        )
                        Text(
                            "\nCette version lit uniquement vos pas, vos séances d’exercice et leur distance depuis Health Connect. " +
                                "Lorsque vous appuyez sur « Synchroniser maintenant », ces données sont envoyées vers votre propre compte Mon équilibre. " +
                                "Elles sont protégées par les règles d’accès de votre compte et ne sont pas partagées entre utilisateurs. " +
                                "Vous pouvez retirer les autorisations Health Connect à tout moment.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF3C3537),
                        )
                    }
                }
            }
        }
    }
}
