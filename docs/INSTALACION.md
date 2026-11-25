# Instalar StudyLock en tu celular

Guía paso a paso para instalar el APK firmado y para resolver los errores más comunes.
Si algo falla, ve directo a la sección **[Errores comunes y su solución](#errores-comunes-y-su-solución)**.

---

## 1. Requisitos

| Requisito | Valor |
|---|---|
| Versión de Android | **8.0 (Oreo, API 26) o superior** |
| Arquitectura | Cualquiera: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` (el APK es universal) |
| Espacio libre | ~14 MB |
| Permisos que pedirá | Uso de apps, superposición, accesibilidad, notificaciones, batería |

> La app **no** está en Google Play. Se instala como APK (instalación manual / *sideload*).

---

## 2. ¿Dónde está el APK?

El repositorio **no incluye el APK**: los archivos `*.apk` están en `.gitignore` para no versionar
binarios. Por eso, si clonas o descargas el código desde GitHub, **no hay nada que instalar**: solo
obtienes el código fuente.

Tienes dos formas de conseguir el APK:

### Opción A — Compilarlo tú mismo (recomendado si tienes el proyecto)

```bash
# APK de release (firmado si existe keystore.properties)
./gradlew :app:assembleRelease
# Resultado:
#   app/build/outputs/apk/release/app-release.apk
```

Si `keystore.properties` no existe, se genera `app-release-unsigned.apk` (sin firmar, no sirve para
instalar hasta firmarlo).

### Opción B — Descargarlo ya compilado

Para las betas, usa siempre este enlace fijo (apunta a la última beta firmada):

`https://github.com/Focusergon/StudyLock/releases/download/beta-latest/StudyLock-beta-latest.apk`

También están todas las versiones en `https://github.com/Focusergon/StudyLock/releases`.
Si descargas un release numerado, el archivo se llama `StudyLock-<versión>.apk`.

> **Copia el archivo al teléfono por cable USB, Google Drive o descarga directa.**
> Evita enviarlo por WhatsApp/Telegram: a veces renombran o truncan el `.apk` y aparece el error
> «No se pudo analizar el paquete».

---

## 3. Instalación paso a paso

1. **Copia** `app-release.apk` al almacenamiento del teléfono (por ejemplo, la carpeta *Descargas*).
2. Abre **Archivos / Mi archivo / Gestor de archivos** y toca el APK.
3. Android pedirá **permitir la instalación de apps de origen desconocido** para esa app
   (Archivos, Chrome, etc.):
   - Toca **Ajustes** en el aviso → activa **«Permitir de esta fuente»**.
4. Toca **Instalar**.
5. Si aparece **Play Protect** («No se pudo instalar» / «App bloqueada»), toca
   **«Instalar de todos modos»** o desactiva temporalmente el análisis:
   Play Store → foto de perfil → **Play Protect** → ⚙️ → desactiva *Analizar apps*.
6. Al terminar, **Abre** la app y completa el onboarding.

---

## 4. Permisos después de instalar

En **Ajustes → Permisos del sistema** (o durante el onboarding) activa:

1. **Acceso al uso de aplicaciones** (`PACKAGE_USAGE_STATS`): Ajustes → *Acceso especial a apps* →
   *Acceso al uso* → StudyLock → *Permitir*.
2. **Superposición sobre otras apps** (`SYSTEM_ALERT_WINDOW`): necesario para mostrar el reto.
3. **Servicio de accesibilidad**: es el que detecta la app en primer plano.
4. **Optimización de batería**: elige *Sin restricciones* para que el cronómetro no se detenga.
5. **Notificaciones** (Android 13+): para el cronómetro y los recordatorios.

> La app **no lee el contenido de otras apps**: el servicio de accesibilidad solo recibe el
> **nombre del paquete** en primer plano (`canRetrieveWindowContent="false"`).

---

## 5. Errores comunes y su solución

### 5.1 «No me deja activar la accesibilidad» / opción en gris (Android 13+)

**Síntoma:** en Ajustes → Accesibilidad, el interruptor de StudyLock está deshabilitado o muestra
*«Configuración restringida: por tu seguridad, esta opción no está disponible»*.

**Causa:** Android 13 (API 33) y superiores aplican **«Restricted settings»** a las apps
instaladas manualmente (fuera de Play Store). Bloquea a propósito el acceso a *Accesibilidad* y a
*Acceso a notificaciones* de apps *sideloaded*.

**Solución:**
1. Ajustes → **Aplicaciones** → **StudyLock**.
2. Toca el menú **⋮** (arriba a la derecha).
3. Elige **«Permitir configuraciones restringidas»** (*Allow restricted settings*).
4. Vuelve a Ajustes → Accesibilidad → activa StudyLock.

> Si no aparece la opción ⋮, primero abre la pantalla de Accesibilidad y toca el interruptor de
> StudyLock; Android mostrará el aviso y luego habilitará el menú.

### 5.2 «App no instalada» / «No se pudo instalar»

| Causa posible | Cómo confirmarla | Solución |
|---|---|---|
| Ya existe una versión con **otra firma** (por ejemplo, un APK *debug* anterior) | Aparece «paquete en conflicto con un paquete existente» | Desinstala la versión anterior y vuelve a instalar |
| **Play Protect** bloquea la app | Aparece un aviso de Play Protect | «Instalar de todos modos» o desactiva el análisis temporalmente |
| **Orígenes desconocidos** desactivados | Aviso «Instalar apps desconocidas» | Activa «Permitir de esta fuente» para el gestor de archivos |
| **Android menor a 8.0** | La app no aparece o dice «no compatible» | Necesitas Android 8.0+ (API 26) |
| **Espacio insuficiente** | — | Libera al menos ~50 MB |

### 5.3 «No se pudo analizar el paquete» (*There was a problem parsing the package*)

**Causa:** el archivo se descargó o transfirió incompleto, o se renombró.
**Solución:** vuelve a descargarlo directamente (navegador/Drive), verifica el tamaño
(**≈13.3 MB**) y que la extensión sea `.apk`. No lo envíes por WhatsApp/Telegram.

### 5.4 La app se instala pero no bloquea nada

1. Confirma que el **servicio de accesibilidad** sigue **conectado** (Ajustes → Accesibilidad →
   StudyLock debe estar *Activado*). Tras reinstalar el APK, el servicio suele quedar desconectado.
2. Verifica que la app esté en la lista **Ajustes → Apps limitadas**.
3. Comprueba el **Acceso al uso** (sin él no hay detección de respaldo).
4. Revisa la **optimización de batería** (pon la app en *Sin restricciones*).

### 5.5 El servicio se detiene solo (Xiaomi, Huawei, Samsung, Oppo, Vivo, Realme)

Estos fabricantes matan servicios en segundo plano. Activa:
- **Inicio automático / Autostart** para StudyLock.
- **Sin restricciones de batería**.
- Bloquea la app en *Recientes* (candado) para que no se cierre.

---

## 6. Desinstalar / reinstalar limpio

```text
Ajustes → Aplicaciones → StudyLock → Desinstalar
```

Si la instalación se queda «a medias», desinstala y reinstala. Para un reinicio total de datos,
usa *Almacenamiento → Borrar datos* o desinstala por completo.

---

## 7. Verificar que el APK está bien firmado (opcional)

Con Android Build Tools:

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Debe imprimir `Verifies` y la huella del certificado (`CN=StudyLock UNJBG`). Si dice
`DOES NOT VERIFY`, el APK no está firmado correctamente y el teléfono lo rechazará.
