package pe.edu.unjbg.studylock.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Estado de red para las dos tareas que consumen datos: sincronización y modelo local. */
class NetworkStatus(context: Context) {
    private val manager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /** Wi-Fi real (no se cuenta móvil ni VPN). Es el valor por defecto de las dos opciones. */
    fun isWifi(): Boolean {
        val network = manager?.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isConnected(): Boolean {
        val network = manager?.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
