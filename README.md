# AutoCare

App Android para llevar el control del mantenimiento, los gastos, los recordatorios y los documentos de tus vehículos.

## Tecnologías

| Capa | Herramienta |
| --- | --- |
| Lenguaje | Kotlin 2.3.21 |
| Interfaz | Jetpack Compose + Material 3 Expressive |
| Arquitectura | MVVM + Repository, inyección manual (`AppContainer`) |
| Datos locales | Room 2.8.4 (KSP), DataStore |
| Nube | Firebase Authentication y Cloud Firestore (BoM 35.0.0) |
| Tareas en segundo plano | WorkManager |
| Pruebas | JUnit 4, kotlinx-coroutines-test, Compose UI Test |

## Requisitos

- Android Studio con JDK 25 para el daemon de Gradle (el JBR incluido sirve); el código compila a Java 11.
- Android SDK 37, `minSdk` 27.
- El archivo `app/google-services.json` del proyecto de Firebase `autocare-90055`.

## Compilar y probar

```bash
./gradlew assembleDebug          # APK de depuración
./gradlew testDebugUnitTest      # pruebas unitarias
./gradlew connectedDebugAndroidTest  # pruebas instrumentadas (emulador encendido)
```

## Firebase

- **Authentication:** proveedor de correo y contraseña. Las cuentas locales anteriores se migran solas al iniciar sesión.
- **Cloud Firestore:** perfil del usuario en `usuarios/{uid}`. Las reglas están en [`firestore.rules`](firestore.rules).

## Ramas y versiones

- `main`: versiones publicadas, cada una con su etiqueta (`v1.0.0`, `v1.1.0`, ...).
- `develop`: integración de las funciones terminadas.
- `feature/<nombre>`: una rama por historia de usuario; se integra a `develop` con un pull request.

Las versiones siguen [SemVer](https://semver.org/lang/es/): `versionName` = MAYOR.MENOR.PARCHE y `versionCode` aumenta en cada entrega.
