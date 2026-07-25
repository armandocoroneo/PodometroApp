# 🏃 Podómetro Android

Aplicación nativa Android en Kotlin que cuenta pasos usando el acelerómetro del móvil.

## 🚀 Compilar con Codemagic

1. Sube este repo a GitHub.
2. Conecta el repo en [codemagic.io/apps](https://codemagic.io/apps).
3. Selecciona tipo **Android**.
4. Pulsa **"Start your first build"**.
5. Descarga el APK desde **Artifacts**.

## 🖥️ Compilar localmente

Abre en Android Studio y pulsa **Run**. El proyecto incluye los archivos del Gradle Wrapper.

## 📂 Estructura

```
PodometroApp/
├── app/src/main/java/com/tuapp/podometro/
│   ├── MainActivity.kt
│   ├── StepCounterService.kt
│   └── StepDetector.kt
├── codemagic.yaml
├── gradlew / gradlew.bat
└── README.md
```
