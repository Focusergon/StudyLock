# StudyLock UNJBG

Aplicación Android nativa que convierte el tiempo de ocio en repaso académico. Cuando se
agotan los minutos disponibles, abrir una app limitada (TikTok, Instagram, YouTube, juegos…)
muestra un **reto rápido de 2 preguntas**. Resolverlo otorga minutos de acceso temporales que
se descuentan solo mientras usas esa app.

> Estado: **versión inicial completa y escalable**. Incluye el bucle de bloqueo, generación de
> preguntas on-device a partir de tu propio material, importación desde el Aula Virtual UNJBG
> (Moodle), horario de clases con recordatorios inteligentes e historial de minutos y rachas.
> El generador puede sustituirse por un modelo de IA más potente sin tocar la UI.

---

## 1. Qué incluye esta versión

### Flujo de interrupción positiva
- `AccessibilityService` detecta la app que pasa a primer plano (solo lee el nombre del paquete).
- `BlockingGate` decide: dejar pasar, iniciar acceso o abrir la pantalla de bloqueo.
- `BlockingActivity` muestra el reto de 2 preguntas y el resultado con los minutos ganados.
- `AccessTimerService` (foreground service) hace la cuenta regresiva y consume minutos reales.
- El temporizador **se pausa solo** cuando la app limitada deja de estar visible: el primer plano
  se reconstruye con los eventos `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` de `UsageStatsManager`
  (fuente principal) y se contrasta con el último paquete observado por accesibilidad. Así no se
  gastan minutos navegando en otra parte y el saldo mostrado coincide con el real.
- Al llegar a cero, se vuelve a bloquear automáticamente.
- **Desbloqueo de emergencia** de 2 minutos con espera de 20 minutos entre usos.
- Estado del temporizador persistido en `SharedPreferences`, sobrevive a la muerte del proceso.
- Tras reiniciar el teléfono no se regalan minutos: `BootReceiver` limpia el acceso activo.

### Estudio y analítica
- Onboarding: bienvenida, vinculación de cuenta UNJBG, activación de permisos con estado en
  vivo, selección de apps instaladas.
- Inicio: saldo de minutos, acceso activo, estadísticas del día, siguiente tema recomendado,
  gráfico semanal, apps limitadas.
- Mis cursos + detalle por curso con dominio y estado por tema
  (**Aprendido / Necesita repaso / Pendiente**).
- Sesión de repaso adaptativo de 5 preguntas con retroalimentación y explicación.
- Progreso: minutos, sesiones, preguntas, aciertos, racha y dominio por curso.
- Ajustes: permisos del sistema, meta diaria, recordatorios, emergencia, privacidad y reinicio
  del onboarding.
- Repetición espaciada tipo SM-2 simplificada (`ReviewScheduler`) y priorización de temas
  débiles (`Topic.priority`).

### Importación de contenido (sin crear tarjetas a mano)
- **Aula Virtual UNJBG (Moodle)**: conector de servicios web (`AulaVirtualClient`) que valida el
  token móvil, lista tus cursos y descarga los documentos de cada sección.
- **Texto pegado** o **archivo `.txt`/`.md`/`.pdf`** (extractor de PDF propio, sin dependencias).
- `QuestionGenerator` detecta temas (encabezados) y crea preguntas de completar y de definición
  con distractores sacados del propio material. Es determinista y se ejecuta en el dispositivo.
- Vista previa antes de guardar e **historial de importaciones** con eliminación.
- Reimportar un curso con el mismo nombre lo **reemplaza** en vez de duplicarlo.

### Horario y notificaciones inteligentes
- `ScheduleScreen`: registra tus clases por día (hora de inicio/fin, aula) y calcula tus
  **franjas libres**.
- `SmartReminder`: solo avisa cuando estás en una franja libre, no llegaste a tu meta diaria y no
  recibiste un aviso reciente (mínimo 90 min). Nunca interrumpe una clase ni los 10 minutos
  previos. Un `Worker` cada 30 minutos aplica la regla.

### Historial y rachas
- `StatsRepository.observeHistory`: minutos, preguntas, aciertos, días activos, mejor racha y
  racha actual de los últimos 14 días, con gráfico y listado por día.

### Reglas de recompensa (MVP)
| Sesión | Preguntas | Minutos |
|---|---|---|
| Repaso | 5 | 5 si ≥80 % de acierto, 3 si ≥50 %, 2 si menos |
| Reto rápido | 2 | 5 con ambos aciertos, 3 en caso contrario |
| Emergencia | — | 2 (cooldown 20 min) |

Los minutos se modelan como un **ledger inmutable** (`minute_tx`): ganar y gastar son
asientos, nunca un contador mutable. Así el saldo siempre es auditable y se puede mostrar el
historial en una versión futura.

---

## 2. Cómo compilar

Requisitos:
- JDK 17+ (probado con JDK 25).
- Android SDK Platform 37 y Build Tools 36.
- Sin `ANDROID_HOME`: crea `local.properties` con
  ```
  sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
  ```

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK generado en `app/build/outputs/apk/debug/app-debug.apk`.

> `local-repo/` es un espejo Maven parcial con `material-icons-extended` (≈35 MB). Existe porque
> en redes inestables esa descarga falla y rompe el build. Se puede borrar cuando la red sea
> estable; Gradle volverá a resolver desde `google()`. Detalles en `local-repo/README.md`.

### Firma de release (APK firmado)

El APK de release se firma con un keystore local cuyas credenciales **no se versionan**
(`keystore.properties` y `*.jks` están en `.gitignore`).

1. Genera el keystore (una sola vez, guárdalo en un lugar seguro):

   ```bash
   keytool -genkeypair -v -keystore studylock-release.jks -alias studylock \
     -keyalg RSA -keysize 4096 -validity 10000 -storetype PKCS12 \
     -storepass <PASSWORD> -keypass <PASSWORD> \
     -dname "CN=StudyLock UNJBG, OU=UNJBG, O=StudyLock, L=Tacna, ST=Tacna, C=PE"
   ```

2. Copia `keystore.properties.example` a `keystore.properties` y rellena `storePassword`/`keyPassword`.

3. Compila y verifica:

   ```bash
   ./gradlew :app:assembleRelease
   $ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

APK firmado en `app/build/outputs/apk/release/app-release.apk`. Si `keystore.properties` no
existe, la build de release sigue funcionando pero genera un APK sin firmar
(`app-release-unsigned.apk`).

> **Importante**: si pierdes el keystore o su contraseña no podrás publicar actualizaciones de
> la app con la misma identidad. Respalda `studylock-release.jks` y la contraseña.

### Stack
- Kotlin + Jetpack Compose (Material 3), `minSdk 26`, `compileSdk/targetSdk 37`.
- AGP **9.5.1** con **Kotlin integrado** (no se aplica `org.jetbrains.kotlin.android`).
- Navigation Compose, DataStore Preferences, WorkManager, `material-icons-extended`.
- **SQLite directo** (`SQLiteOpenHelper`) detrás de interfaces de repositorio. No hay KSP/Room
  porque KSP todavía no es compatible con el Kotlin integrado de AGP 9. Migrar a Room es un
  cambio contenido a `data/` (las features solo ven las interfaces de `domain/repo`).
- **DI manual** (`AppGraph`). Es el punto de entrada para migrar a Hilt sin tocar features.

---

## 3. Estructura

```
app/src/main/java/pe/edu/unjbg/studylock/
├── StudyLockApp.kt              # Application: AppGraph + canales + recordatorio
├── core/
│   ├── designsystem/            # Tokens, tipografía, componentes reutilizables
│   ├── text/                    # Extractor de texto de PDF (Kotlin puro)
│   └── util/                    # Tiempo, permisos, carga de apps instaladas
├── data/
│   ├── aula/                    # Conector del Aula Virtual UNJBG (Moodle Web Services)
│   ├── local/                   # SQLiteOpenHelper, esquema, helpers de Cursor
│   ├── prefs/                   # DataStore (perfil/metas) y AccessStore (tiempo real)
│   ├── repos/                   # Implementaciones de repositorios
│   └── seed/                    # Contenido académico de demostración
├── di/AppGraph.kt               # Contenedor manual (único por proceso)
├── domain/
│   ├── content/                 # Generador de preguntas a partir de texto
│   ├── model/                   # Course, Topic, Question, MinuteTx, ClassBlock, History…
│   ├── repo/                    # Interfaces (contratos)
│   ├── HistoryStats.kt          # Rachas y días activos (puro, testeable)
│   ├── SmartReminder.kt         # Decisión de avisos en franjas libres (puro, testeable)
│   └── RewardPolicy.kt          # SM-2 simplificado + reglas de minutos
├── blocking/
│   ├── BlockingGate.kt          # Decisión central de bloqueo
│   ├── StudyLockAccessibilityService.kt
│   ├── AccessTimerService.kt    # Foreground service con la cuenta regresiva
│   ├── BlockingActivity.kt      # Interceptor + reto
│   ├── BootReceiver.kt
│   ├── ReminderScheduler.kt
│   └── StudyLockNotifications.kt
├── feature/
│   ├── onboarding/ home/ courses/ session/ progress/ settings/
│   ├── import/                  # Importación (Aula Virtual, texto, archivo)
│   ├── schedule/                # Horario de clases y franjas libres
│   ├── access/                  # Pantalla de acceso temporal activo
│   └── blocking/                # UI y ViewModel del reto
├── navigation/                  # Rutas + NavHost raíz
└── ui/                          # MainActivity, LocalAppGraph, helpers de permisos
```

### Flujo de datos
1. La UI observa `Flow`s de repositorios (`observeCourses`, `observeDashboard`, …).
2. Cualquier escritura llama `StudyLockDatabase.notifyChanged()`.
3. Un `MutableSharedFlow` con `replay = 1` reemite y todas las pantallas se actualizan.
4. La capa de bloqueo usa `suspend` + `Dispatchers.IO` y no depende de Compose.

### Puntos de extensión
- **Conector UNJBG**: `AulaVirtualClient` (Moodle Web Services) ya importa cursos y documentos.
  Para SSO institucional, sustituye el token por OAuth manteniendo `ContentRepository`.
- **Generador de preguntas**: `QuestionGenerator` (on-device) llena `questions` y `options`.
  Reemplazarlo por un LLM es cambiar la implementación de `ContentRepository`, sin tocar la UI.
- **Backend**: sincronización opcional de progreso; el ledger local sigue siendo la verdad en
  el dispositivo.
- **Hilt**: reemplazar `AppGraph` por módulos, manteniendo constructores por interfaz.

---

## 4. Permisos que debes activar en el dispositivo

En el onboarding la app muestra el estado en vivo de cada permiso. También puedes abrirlos
desde **Ajustes → Permisos del sistema**.

1. **Acceso al uso de aplicaciones** (`PACKAGE_USAGE_STATS`): detector de respaldo.
2. **Superposición sobre otras apps** (`SYSTEM_ALERT_WINDOW`): permite mostrar el reto.
3. **Servicio de accesibilidad**: detección principal por eventos de ventana.
   La app **no lee contenido**: solo `event.packageName`.
4. **Optimización de batería** (recomendado): evita que el sistema mate el temporizador.
5. **Notificaciones** (Android 13+): necesarias para el cronómetro y recordatorios.

### Cómo probar el bloqueo
1. Instala el APK y completa el onboarding.
2. Elige al menos una app instalada (por defecto se preseleccionan redes sociales, videos y
   juegos conocidos).
3. Abre esa app con saldo 0 → aparece la pantalla de bloqueo.
4. Resuelve el reto → verás “+5 min” → “Abrir app” inicia el temporizador.
5. El cronómetro aparece en la notificación y en **Inicio → Acceso activo**.
6. “Finalizar antes” corta el acceso y devuelve el saldo restante al ledger.

> En algunos fabricantes (Xiaomi, Huawei, Samsung) hay que permitir “inicio automático” y
> desactivar la optimización agresiva para que el servicio de accesibilidad no se detenga.

---

## 5. Privacidad y seguridad

- Todo el progreso, cursos y minutos viven en SQLite local; no hay backend propio.
- El correo institucional se guarda en DataStore. **No se almacena contraseña**: el onboarding es
  una vinculación local y el Aula Virtual usa el **token de servicio web** que generas tú en tu
  perfil de Moodle (Preferencias → Seguridad → Claves), no tu contraseña.
- La app pide `INTERNET` solo para hablar con el Aula Virtual que tú indiques; el material se
  procesa en el dispositivo y no se envía a ningún servidor.
- El servicio de accesibilidad solo recibe el nombre del paquete en primer plano; no inspecciona
  nodos ni contenido de otras apps.
- `allowBackup=false` y reglas de extracción excluyen bases de datos y preferencias.
- Al integrar SSO institucional: usar OAuth/SSO y cifrado en reposo, nunca credenciales en texto
  plano en el dispositivo.

### Nota sobre Google Play
El uso de `AccessibilityService` para bloquear apps requiere justificación explícita en Play
Console y puede ser rechazado. Alternativas: usar `UsageStatsManager` + overlay como mecanismo
principal (ya declarado) y dejar accesibilidad como refuerzo, o distribuir el APK fuera de Play
para la comunidad UNJBG. `isAccessibilityTool="false"` está declarado en la configuración.

---

## 6. Limitaciones conocidas

- La extracción de PDF es **best-effort**: funciona con PDFs de texto (Word/LaTeX), no con
  escaneados ni fuentes CID (requerirían OCR).
- El generador de preguntas es determinista y local; la calidad mejora al sustituirlo por un LLM.
- El Aula Virtual requiere que el administrador de Moodle tenga habilitado el servicio web móvil
  y que tú generes un token; no se usa SSO todavía.
- No hay autenticación remota ni sincronización multidispositivo.
- La detección depende del servicio de accesibilidad; si el usuario lo desactiva, el bloqueo
  se degrada (el temporizador usa `UsageStatsManager` como respaldo, pero no hay intercepción
  automática sin accesibilidad).
- El servicio de accesibilidad debe estar **conectado** para interceptar; tras una reinstalación
  del APK conviene revisar en Ajustes que siga activo.
- La lista de apps conocidas para preselección es estática; se puede ampliar en
  `InstalledAppsLoader.knownCategories`.
- Hay 46 tests JVM de la lógica pura (`RewardPolicy`, `ReviewScheduler`, `TimeUtils`,
  `QuestionGenerator`, `PdfTextExtractor`, `SmartReminder`, `HistoryStats`, franjas libres), pero
  todavía no hay tests instrumentados de UI ni pruebas en dispositivos reales.

---

## 7. Roadmap sugerido

1. **SSO institucional** para el Aula Virtual (hoy se usa el token de servicio web móvil).
2. **Generador con IA** (backend o modelo on-device) para preguntas de mayor calidad, con
   validación docente.
3. **Importar horario** desde el Aula Virtual o un archivo `.ics` (hoy se registra a mano).
4. **Sesiones ultra-cortas proactivas**: sugerir 1–2 preguntas al desbloquear el teléfono.
5. Widget/Quick Settings para consultar saldo sin abrir la app.
6. Migración a Room cuando KSP soporte el Kotlin integrado de AGP 9, o mantener SQLite.
7. Tests: Compose UI tests + pruebas en dispositivos reales (la lógica de dominio ya está cubierta).

---

Hecho para estudiantes de la **Universidad Nacional Jorge Basadre Grohmann (UNJBG)**.
