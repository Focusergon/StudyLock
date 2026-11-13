package pe.edu.unjbg.studylock.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import pe.edu.unjbg.studylock.domain.model.BlockedApp

data class InstalledApp(
    val packageName: String,
    val label: String,
    val category: String,
    val icon: ImageBitmap?,
)

object InstalledAppsLoader {

    private val knownCategories = mapOf(
        "com.zhiliaoapp.musically" to "Redes sociales",
        "com.instagram.android" to "Redes sociales",
        "com.google.android.youtube" to "Videos",
        "com.facebook.katana" to "Redes sociales",
        "com.whatsapp" to "Mensajería",
        "com.twitter.android" to "Redes sociales",
        "com.snapchat.android" to "Redes sociales",
        "com.spotify.music" to "Música",
        "com.netflix.mediaclient" to "Videos",
        "com.discord" to "Comunidad",
        "com.roblox.client" to "Juegos",
        "com.mobile.legends" to "Juegos",
        "com.supercell.clashofclans" to "Juegos",
        "com.tencent.ig" to "Juegos",
    )

    fun load(context: Context, withIcons: Boolean = true): List<InstalledApp> {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved
            .asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || isKnown(it.packageName) }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    category = knownCategories[info.packageName] ?: "Otras",
                    icon = if (withIcons) info.loadIcon(pm).toImageBitmap() else null,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun labelFor(context: Context, packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    fun isKnown(packageName: String): Boolean = knownCategories.containsKey(packageName)

    fun categoryFor(packageName: String): String = knownCategories[packageName] ?: "Otras"

    private fun Drawable.toImageBitmap(): ImageBitmap? = runCatching {
        toBitmap(width = 96, height = 96).asImageBitmap()
    }.getOrNull()

    fun defaultsFor(context: Context, installed: List<InstalledApp>): List<BlockedApp> =
        installed.filter { isKnown(it.packageName) && it.category in setOf("Redes sociales", "Videos", "Juegos") }
            .take(3)
            .map { BlockedApp(it.packageName, it.label, it.category, true) }
}
