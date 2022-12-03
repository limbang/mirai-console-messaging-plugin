/*
 * Copyright 2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.utlis

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import okhttp3.internal.ws.RealWebSocket
import java.util.concurrent.TimeUnit


class WebSocketUtils(private val url: String, private val listener: WebSocketListener) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS) // 设置连接超时
        .readTimeout(15, TimeUnit.SECONDS) // 设置读取超时
        .writeTimeout(15, TimeUnit.SECONDS) // 设置写入超时
        .retryOnConnectionFailure(true) // 设置断线重连
        .build()

    private var webSocket: RealWebSocket

    init {
        val request: Request = Request.Builder().url(url).build()
        // 创建 webSocket
        webSocket = okHttpClient.newWebSocket(request, listener) as RealWebSocket
    }

    /**
     * 发送消息
     *
     * @param message
     */
    fun send(message: String): Boolean {
        return webSocket.send(message)
    }

    /**
     * 重新连接
     *
     * @param url
     */
    fun reConnect(url: String) {
        webSocket.cancel()
        val request: Request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, listener as WebSocketListener) as RealWebSocket
    }

    /**
     * 重新连接
     *
     */
    fun reConnect() {
        reConnect(url)
    }


    /**
     * 断开连接
     *
     */
    fun cancel() {
        webSocket.cancel()

    }

}
