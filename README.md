# 🏃 Podómetro Android

Aplicación nativa Android en Kotlin que cuenta pasos usando el acelerómetro del móvil. No requiere internet, Alexa ni hardware adicional.

## 📱 Características

- ✅ Conteo de pasos en tiempo real
- ⏱️ Cronómetro de sesión
- 📏 Distancia recorrida (km)
- 🔥 Calorías quemadas estimadas
- 🔄 Funciona en segundo plano (pantalla bloqueada)
- 📤 Envío de datos a API remota (opcional)

## 🛠️ Tecnologías

- **Kotlin** — Lenguaje principal
- **Jetpack Compose** — UI declarativa
- **SensorManager** — Acelerómetro nativo
- **Foreground Service** — Conteo en background

## 🚀 Compilar con Codemagic (CI/CD)

Este proyecto está configurado para compilarse automáticamente con [Codemagic](https://codemagic.io).

### Requisitos previos

1. Cuenta en [Codemagic](https://codemagic.io/signup) (gratuita).
2. Repositorio en GitHub con este código subido.

### Paso 1: Conectar GitHub con Codemagic

1. Entra a [codemagic.io/apps](https://codemagic.io/apps).
2. Pulsa **"Add application"**.
3. Selecciona **GitHub** y elige tu repositorio `PodometroApp`.
4. Codemagic detectará automáticamente el archivo `codemagic.yaml`.

### Paso 2: Configurar firma (Keystore) — Debug

Para compilar APK de prueba **sin Play Store**, Codemagic usa una firma de debug automática. No necesitas configurar nada extra.

Para compilar APK firmado (release), sigue el paso 3.

### Paso 3: Configurar Keystore para Release (opcional)

Si quieres distribuir el APK firmado:

1. Genera un keystore localmente:
   ```bash
   keytool -genkey -v -keystore podometro.keystore -alias podometro      -keyalg RSA -keysize 2048 -validity 10000
   ```

2. En Codemagic, ve a **Teams → Your team → Code signing identities**.
3. Sube tu `.keystore` como **Android keystore**.
4. En **Environment variables**, crea un grupo llamado `android_credentials` con:
   - `CM_KEYSTORE_PASSWORD`
   - `CM_KEY_ALIAS`
   - `CM_KEY_PASSWORD`

### Paso 4: Ejecutar build

1. Haz `git push` a la rama `main`.
2. Codemagic se activa automáticamente.
3. Ve a la pestaña **Builds** y espera ~3-5 minutos.
4. Descarga el APK desde la sección **Artifacts**.

### Workflows disponibles

| Workflow | Trigger | Salida |
|---|---|---|
| `android-debug` | Push a `main` o PR | `app-debug.apk` |
| `android-release` | Push a `release/*` | `app-release.apk` firmado |

## 🖥️ Compilar localmente (alternativa)

### Requisitos
- Android Studio Hedgehog (2023.1.1) o superior
- SDK mínimo: API 26 (Android 8.0)
- SDK objetivo: API 34

### Pasos

1. Clona el repositorio:
   ```bash
   git clone https://github.com/tuusuario/PodometroApp.git
   cd PodometroApp
   ```

2. Abre el proyecto en Android Studio.

3. Sincroniza Gradle: **File → Sync Project with Gradle Files**.

4. Conecta tu dispositivo Android con modo desarrollador activado.

5. Pulsa **Run** (▶).

### Permisos necesarios
La app solicitará automáticamente:
- `ACTIVITY_RECOGNITION` — detectar movimiento físico
- `POST_NOTIFICATIONS` — notificación en segundo plano (Android 13+)

## 📂 Estructura del proyecto

```
PodometroApp/
├── app/
│   ├── src/main/java/com/tuapp/podometro/
│   │   ├── MainActivity.kt          # UI con Jetpack Compose
│   │   ├── StepCounterService.kt    # Servicio en segundo plano
│   │   └── StepDetector.kt          # Algoritmo de detección de pasos
│   ├── src/main/res/
│   │   └── values/
│   │       ├── strings.xml
│   │       └── themes.xml
│   └── build.gradle.kts
├── build.gradle.kts (project)
├── settings.gradle.kts
├── gradle.properties
├── codemagic.yaml                   # Pipeline CI/CD
└── README.md
```

## 🔗 Conexión con API (opcional)

Para enviar los datos a tu backend PHP (`api.php`), descomenta la función `sendStepsToServer()` en `MainActivity.kt` y configura la URL de tu servidor.

## 📄 Licencia

MIT License — libre uso y modificación.
