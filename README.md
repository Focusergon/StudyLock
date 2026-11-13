# StudyLock UNJBG

Aplicación Android nativa que convierte el tiempo de ocio en repaso académico. Cuando se
agotan los minutos disponibles, abrir una app limitada (TikTok, Instagram, YouTube, juegos…)
muestra un **reto rápido de 2 preguntas**. Resolverlo otorga minutos de acceso temporales que
se descuentan solo mientras usas esa app.

> Estado: **MVP funcional completo** con contenido académico de demostración (3 cursos,
> 12 temas, 36 preguntas). El conector real del Aula Virtual / Intranet y el generador de
> preguntas con IA están aislados detrás de repositorios para conectarlos sin reescribir la app.

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
- Recordatorio diario con WorkManager (18:30, franja libre sugerida).

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
│   └── util/                    # Tiempo, permisos, carga de apps instaladas
├── data/
│   ├── local/                   # SQLiteOpenHelper, esquema, helpers de Cursor
│   ├── prefs/                   # DataStore (perfil/metas) y AccessStore (tiempo real)
│   ├── repos/                   # Implementaciones de repositorios
│   └── seed/                    # Contenido académico de demostración
├── di/AppGraph.kt               # Contenedor manual (único por proceso)
├── domain/
│   ├── model/                   # Course, Topic, Question, MinuteTx, Dashboard…
│   ├── repo/                    # Interfaces (contratos)
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
- **Conector UNJBG**: implementa `CourseRepository`/`StudyRepository` leyendo Aula Virtual y
  escribe en las mismas tablas. La UI no cambia.
- **Generador de preguntas**: llena `questions` y `options` (hoy lo hace `SeedContent`).
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

- Todo el progreso, cursos y minutos viven en SQLite local; no hay backend en esta versión.
- El correo institucional se guarda en DataStore. **No se almacena contraseña** en esta versión:
  el onboarding es una vinculación local y está marcado como punto de integración futura.
- El servicio de accesibilidad solo recibe el nombre del paquete en primer plano; no inspecciona
  nodos ni contenido de otras apps.
- `allowBackup=false` y reglas de extracción excluyen bases de datos y preferencias.
- Al integrar el Aula Virtual real: usar OAuth/SSO institucional o un backend propio con
  cifrado en reposo, nunca credenciales en texto plano en el dispositivo.

### Nota sobre Google Play
El uso de `AccessibilityService` para bloquear apps requiere justificación explícita en Play
Console y puede ser rechazado. Alternativas: usar `UsageStatsManager` + overlay como mecanismo
principal (ya declarado) y dejar accesibilidad como refuerzo, o distribuir el APK fuera de Play
para la comunidad UNJBG. `isAccessibilityTool="false"` está declarado en la configuración.

---

## 6. Limitaciones conocidas del MVP

- El contenido académico es una **semilla de demostración**; el conector del Aula Virtual y la
  generación automática de preguntas son los siguientes hitos.
- No hay autenticación remota ni sincronización multidispositivo.
- La detección depende del servicio de accesibilidad; si el usuario lo desactiva, el bloqueo
  se degrada (el temporizador usa `UsageStatsManager` como respaldo, pero no hay intercepción
  automática sin accesibilidad).
- El servicio de accesibilidad debe estar **conectado** para interceptar; tras una reinstalación
  del APK conviene revisar en Ajustes que siga activo.
- La lista de apps conocidas para preselección es estática; se puede ampliar en
  `InstalledAppsLoader.knownCategories`.
- Hay tests JVM de la lógica pura (`RewardPolicy`, `ReviewScheduler`, `TimeUtils`), pero todavía
  no hay tests instrumentados de UI ni pruebas en dispositivos reales.

---

## 7. Roadmap sugerido

1. **Conector UNJBG** (Aula Virtual/Intranet) con SSO y descarga de sílabos/PDF.
2. **Generador de preguntas** (backend o on-device) + validación docente.
3. Notificaciones inteligentes por horario de clases (requiere horario importado).
4. Historial de minutos y rachas con detalles semanales.
5. Widget/Quick Settings para consultar saldo sin abrir la app.
6. Migración a Room cuando KSP soporte el Kotlin integrado de AGP 9, o mantener SQLite.
7. Tests: unitarios de dominio + Compose UI tests + pruebas en dispositivos reales.

---

Hecho para estudiantes de la **Universidad Nacional Jorge Basadre Grohmann (UNJBG)**.
