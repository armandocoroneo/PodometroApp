package com.tuapp.podometro

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var stepService: StepCounterService? = null
    private var serviceBound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StepCounterService.LocalBinder
            stepService = binder.getService()
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            stepService = null
            serviceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Se necesitan permisos de actividad física para contar pasos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()
        bindService(
            Intent(this, StepCounterService::class.java),
            connection, Context.BIND_AUTO_CREATE
        )
        startService(Intent(this, StepCounterService::class.java))

        setContent {
            PodometroApp(
                stepService = stepService,
                serviceBound = serviceBound
            )
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (perms.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }) {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) unbindService(connection)
    }
}

@Composable
fun PodometroApp(stepService: StepCounterService?, serviceBound: Boolean) {
    val context = LocalContext.current
    var steps by remember { mutableIntStateOf(0) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var sensorType by remember { mutableStateOf("none") }
    var hasError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val strideLength = 0.76f
    val kcalPerStep = 0.04f

    val km = steps * strideLength / 1000f
    val kcal = steps * kcalPerStep
    val totalSec = (elapsedMs / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    val timeStr = "%02d:%02d".format(min, sec)

    LaunchedEffect(serviceBound) {
        stepService?.setCallback { s, e, type ->
            steps = s
            elapsedMs = e
            isRunning = stepService.isRunning
            sensorType = type

            if (type == "none") {
                hasError = true
                errorMsg = "Este dispositivo no tiene sensor de pasos ni acelerómetro."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏃 Podómetro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
        )

        // Indicador de sensor
        val sensorLabel = when (sensorType) {
            "hardware_step_counter" -> "📱 Sensor de pasos (preciso)"
            "accelerometer" -> "📳 Acelerómetro (modo compatibilidad)"
            "none" -> "❌ Sin sensor disponible"
            else -> "Esperando..."
        }
        Text(
            text = sensorLabel,
            fontSize = 12.sp,
            color = if (sensorType == "none") Color(0xFFD32F2F) else Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (hasError) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = errorMsg,
                    fontSize = 13.sp,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    if (isRunning) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$steps",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isRunning) Color(0xFF2E7D32) else Color.Black
                )
                Text(
                    text = "pasos",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(label = "Tiempo", value = timeStr)
            StatCard(label = "Distancia", value = "%.2f km".format(km))
            StatCard(label = "Calorías", value = "%.0f kcal".format(kcal))
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                stepService?.let {
                    if (it.isRunning) {
                        it.stopCounting()
                        isRunning = false
                    } else {
                        if (it.sensorType == "none") {
                            Toast.makeText(
                                context,
                                "No hay sensor disponible en este dispositivo",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            it.startCounting()
                            isRunning = true
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFFD32F2F) else Color.Black
            ),
            enabled = !hasError
        ) {
            Text(
                text = if (isRunning) "⏸ Pausar" else "▶ Iniciar",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Text(
                text = when (sensorType) {
                    "hardware_step_counter" -> "💡 Modo preciso activado. Usa el sensor de pasos dedicado del móvil. Ideal para caminar y correr."
                    "accelerometer" -> "💡 Modo compatibilidad. Usa el acelerómetro. Evita llevar el móvil en mano mientras vas en vehículo para evitar falsos pasos."
                    else -> "💡 Esta app usa el acelerómetro o sensor de pasos de tu móvil. No necesita internet ni hardware extra."
                },
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(16.dp),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
