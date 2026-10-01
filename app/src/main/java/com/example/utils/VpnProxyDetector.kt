package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class VpnProxyStatus(
    val isBlocked: Boolean = false,
    val reason: String = "",
    val isVpn: Boolean = false,
    val isProxy: Boolean = false
)

object VpnProxyDetector {

    fun checkStatus(context: Context): VpnProxyStatus {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null

            // 1. VPN Transport Detection
            val hasVpnTransport = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

            // 2. Tunnel interfaces (tun0, ppp0, etc. used by HTTP Injector and VPN apps)
            val hasTunInterface = try {
                val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                if (interfaces != null) {
                    interfaces.toList().any { nif ->
                        nif.isUp && (nif.name.startsWith("tun") || nif.name.startsWith("ppp") || nif.name.startsWith("p2p") || nif.name.startsWith("tap"))
                    }
                } else false
            } catch (e: Exception) {
                false
            }
            val hasVpn = hasVpnTransport || hasTunInterface

            // 3. System Proxy Detection (including local proxies set by HTTP Injector e.g. 127.0.0.1:8989)
            val httpHost = System.getProperty("http.proxyHost")
            val httpPort = System.getProperty("http.proxyPort")
            val httpsHost = System.getProperty("https.proxyHost")
            val httpsPort = System.getProperty("https.proxyPort")
            val hasProxy = (!httpHost.isNullOrEmpty() && !httpPort.isNullOrEmpty()) ||
                    (!httpsHost.isNullOrEmpty() && !httpsPort.isNullOrEmpty())

            // 4. Anti-Debug Check
            val isDebugging = android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger()

            val isBlocked = hasVpn || hasProxy || isDebugging
            val reason = when {
                isDebugging -> "Herramienta de depuración / análisis detectada"
                hasVpn && hasProxy -> "VPN y Proxy de interceptación detectados (HTTP Injector)"
                hasVpn -> "Conexión VPN o túnel de red activo"
                hasProxy -> "Servidor Proxy detectado"
                else -> ""
            }

            VpnProxyStatus(
                isBlocked = isBlocked,
                reason = reason,
                isVpn = hasVpn,
                isProxy = hasProxy
            )
        } catch (e: Exception) {
            VpnProxyStatus(isBlocked = false)
        }
    }

    fun isVpnOrProxyActive(context: Context): Boolean {
        return checkStatus(context).isBlocked
    }

    fun observeVpnAndProxy(context: Context): Flow<VpnProxyStatus> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(VpnProxyStatus(false))
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(checkStatus(context))
            }

            override fun onLost(network: Network) {
                trySend(checkStatus(context))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(checkStatus(context))
            }
        }

        trySend(checkStatus(context))

        val request = NetworkRequest.Builder().build()
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            trySend(checkStatus(context))
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()
}
