/*
 * Copyright 2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang

import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.PlainText
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import top.limbang.MessagingData.messagingServerList
import top.limbang.entity.ConnectStatus
import top.limbang.entity.MessagingServer
import top.limbang.entity.UserMessage
import top.limbang.utlis.WebSocketUtils
import top.limbang.utlis.toMiraiMessage
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketTimeoutException


object Messaging : KotlinPlugin(
    JvmPluginDescription(
        id = "top.limbang.messaging",
        name = "Messaging",
        version = "0.1.0",
    ) {
        author("limbang")
    }
) {

    override fun onEnable() {
        MessagingData.reload()
        MessagingCompositeCommand.register()

        // 创建事件通道
        val eventChannel = GlobalEventChannel.parentScope(this)

        // 监听群消息
        eventChannel.subscribeGroupMessages {

            startsWith("开始监听") { name ->
                val messagingServer = messagingServerList.find { it.groupId == group.id && it.name == name.trim() }
                // 判断服务器是否没有添加
                if (messagingServer == null) {
                    group.sendMessage("没有找到[${name.trim()}]服务器,请添加后在监听")
                    return@startsWith
                }
                messagingServer.handler(group)
            }

            startsWith("全部监听") {
                val messagingServerList = messagingServerList.filter { it.groupId == group.id}
                messagingServerList.forEach {
                    it.handler(group)
                }
            }

            always { msg ->
                val messagingServer = messagingServerList.find { it.groupId == group.id && msg.trim().startsWith(it.name) } ?: return@always
                // 服务器未连接直接返回
                if (messagingServer.status == ConnectStatus.DISCONNECT) {
                    group.sendMessage("[${messagingServer.name}]服务器未监听")
                    return@always
                }
                val message = UserMessage(sender.nameCardOrNick, msg.trim().substring(messagingServer.name.length))
                // 如果服务器连接了就发送消息
                messagingServer.webSocketUtils?.send(Json.encodeToString(message))
            }

        }
    }

    /**
     * 处理转发服务器到群的消息
     *
     * @param group 群
     */
    private suspend fun MessagingServer.handler(group: Group){

        // 判断是否已经连接
        if (status == ConnectStatus.CONNECTION) {
            group.sendMessage("[$name]已经监听")
            return
        }

        webSocketUtils = WebSocketUtils(url, object : WebSocketListener() {

            // 连接关闭
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                launch { group.sendMessage("[$name]服务器关闭:$code") }
                status = ConnectStatus.DISCONNECT
            }

            // 异常处理
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                launch {
                    when (t) {
                        is ConnectException -> group.sendMessage("[$name]连接失败,请检查服务器后重新监听")
                        is EOFException -> group.sendMessage("[$name]连接异常,请重新监听")
                        is SocketTimeoutException -> group.sendMessage("[$name]连接超时,请检查服务器后重新监听")
                        else -> logger.error(t)
                    }
                }
                status = ConnectStatus.DISCONNECT
            }

            // 服务器发来消息
            override fun onMessage(webSocket: WebSocket, text: String) {
                launch {
                    runCatching {
                        Json.decodeFromString<UserMessage>(text)
                    }.onSuccess {
                        group.sendMessage(PlainText("[$name] <${it.name}> ").plus(it.toMiraiMessage()))
                    }.onFailure {
                        logger.error(it)
                    }
                }
            }

            // 连接服务器成功
            override fun onOpen(webSocket: WebSocket, response: Response) {
                launch { group.sendMessage("连接[$name]服务器成功") }
                status = ConnectStatus.CONNECTION
            }
        })
    }

}