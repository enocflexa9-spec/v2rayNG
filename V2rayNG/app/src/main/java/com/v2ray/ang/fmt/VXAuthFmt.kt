package com.v2ray.ang.fmt

import android.util.Base64
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.Utils
import java.net.URI

object VXAuthFmt : FmtBase() {

    fun parse(str: String): ProfileItem? {
        val config = ProfileItem.create(EConfigType.VXAUTH)
        try {
            val uri = URI(Utils.fixIllegalUrl(str))
            if (uri.host.isNullOrEmpty()) return null
            if (uri.port <= 0) return null

            config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).ifEmpty { "VXAuth" }
            config.server = uri.host
            config.serverPort = uri.port.toString()

            if (uri.userInfo?.isNotEmpty() == true) {
                val decoded = try {
                    String(Base64.decode(uri.userInfo, Base64.DEFAULT))
                } catch (_: Exception) { uri.userInfo }
                val parts = decoded.split(":", limit = 2)
                if (parts.size == 2) {
                    config.username = parts[0]
                    config.password = parts[1]
                }
            }

            val query = uri.query?.split("&")?.associate {
                val kv = it.split("=", limit = 2)
                if (kv.size == 2) kv[0] to Utils.decodeURIComponent(kv[1]) else kv[0] to ""
            } ?: emptyMap()

            config.path = query["path"] ?: "/"
            config.sni = query["sni"] ?: ""
            config.security = query["security"] ?: "tls"
            config.host = query["host"] ?: config.server

        } catch (e: Exception) {
            return null
        }
        return config
    }

    fun toUri(config: ProfileItem): String {
        val creds = Base64.encodeToString(
            "${config.username}:${config.password}".toByteArray(),
            Base64.NO_WRAP
        )
        val sb = StringBuilder()
        sb.append("${config.server}:${config.serverPort}")
        sb.append("?path=${Utils.encodeURIComponent(config.path ?: "/")}")
        if (!config.sni.isNullOrEmpty()) sb.append("&sni=${config.sni}")
        if (!config.security.isNullOrEmpty()) sb.append("&security=${config.security}")
        if (!config.host.isNullOrEmpty()) sb.append("&host=${config.host}")
        sb.append("#${Utils.encodeURIComponent(config.remarks)}")
        return "vxauth://$creds@$sb"
    }

    fun getAuthToken(config: ProfileItem): String {
        return Base64.encodeToString(
            "${config.username}:${config.password}".toByteArray(),
            Base64.NO_WRAP
        )
    }
}
