# local-repo

Espejo Maven **parcial** usado solo como respaldo de red.

Contiene únicamente:

```
androidx/compose/material/material-icons-extended/1.7.8/…
androidx/compose/material/material-icons-extended-android/1.7.8/…
```

## ¿Por qué existe?

`material-icons-extended` pesa ~35 MB. En redes inestables la descarga desde
`dl.google.com` falla con `Host desconocido (dl.google.com)` y rompe el build en
`processDebugNavigationResources`. Con este repositorio local el artefacto se resuelve sin red.

El AAR fue descargado de Maven de Google y su `sha1`
(`7499b6142a798c77a178b1098319c0b90595ab84`) coincide con el declarado en el
`.module` oficial, así que no está alterado.

## Cómo regenerarlo (si algún día hace falta)

```powershell
$dir = "local-repo/androidx/compose/material/material-icons-extended-android/1.7.8"
New-Item -ItemType Directory -Force $dir | Out-Null
Invoke-WebRequest `
  "https://dl.google.com/dl/android/maven2/androidx/compose/material/material-icons-extended-android/1.7.8/material-icons-extended-android-1.7.8.aar" `
  -OutFile "$dir/material-icons-extended-android-1.7.8.aar"
```

También hacen falta los `.pom` y `.module` de `material-icons-extended` y
`material-icons-extended-android` en la misma estructura.

Se puede borrar esta carpeta cuando la red funcione de forma estable: Gradle volverá a
resolver desde `google()`. Es un parche, no una dependencia vendida para siempre.
