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
| API de terceros | vPIC de la NHTSA (catálogo de marcas, modelos y lectura del VIN) |
| Tareas en segundo plano | WorkManager |
| Pruebas | JUnit 4, kotlinx-coroutines-test, org.json, Compose UI Test |

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
- **Cloud Firestore:** perfil del usuario en `usuarios/{uid}` y, dentro, las colecciones `vehiculos`, `mantenimientos`, `gastos`, `recordatorios` y `documentos`. Las reglas están en [`firestore.rules`](firestore.rules).
- **Sincronización:** cada cambio se guarda en Room y se sube a Firestore. Sin conexión queda en la caché de Firestore y se sube al volver la señal. Al iniciar sesión se descargan los datos de la cuenta. Las fotos de comprobantes se quedan en el teléfono.

## API de terceros

[vPIC](https://vpic.nhtsa.dot.gov/api/) de la NHTSA, pública y sin clave. En el registro de vehículos llena marca, modelo y año a partir del número de serie (VIN) y sugiere los modelos de la marca y el año escritos. Código en `data/catalogo/`.

## Seguridad

- Contraseñas validadas por Firebase Authentication; las cuentas locales anteriores usan PBKDF2.
- Reglas de Firestore: cada usuario solo lee y escribe bajo su propio `uid`.
- Sin respaldo automático de Android (`allowBackup="false"` y reglas de extracción que excluyen todo).
- Solo tráfico HTTPS (`usesCleartextTraffic="false"`).
- Reportes PDF compartidos con `FileProvider`.
- R8 en la variante release.

## Ramas y versiones

- `main`: versiones publicadas, cada una con su etiqueta (`v1.0.0`, `v1.1.0`, ...).
- `develop`: integración de las funciones terminadas.
- `feature/<nombre>`: una rama por historia de usuario; se integra a `develop` con un pull request.

Las versiones siguen [SemVer](https://semver.org/lang/es/): `versionName` = MAYOR.MENOR.PARCHE y `versionCode` aumenta en cada entrega.
