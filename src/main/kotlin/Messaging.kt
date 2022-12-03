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
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.PlainText
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import top.limbang.MessagingData.messagingServerList
import top.limbang.entity.ConnectStatus
import top.limbang.entity.UserMessage
import top.limbang.utlis.WebSocketUtils
import top.limbang.utlis.toMiraiMessage
import java.io.EOFException
import java.net.ConnectException


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

        // 获取插件作用域
        val scope = this
        // 创建事件通道
        val eventChannel = GlobalEventChannel.parentScope(scope)

        // 监听群消息
        eventChannel.subscribeGroupMessages {
            startsWith("开始监听") { name ->
                val messagingServer = messagingServerList.find { it.groupId == group.id && it.name == name.trim() }
                // 判断服务器是否没有添加
                if (messagingServer == null) {
                    group.sendMessage("没有找到[${name.trim()}]服务器,请添加后在监听")
                    return@startsWith
                }
                // 判断 websocket 是否已经连接
                if (messagingServer.status == ConnectStatus.CONNECTION) {
                    group.sendMessage("[${name.trim()}]已经监听")
                    return@startsWith
                }
                // 创建并添加 websocket
                messagingServer.webSocketUtils = WebSocketUtils(messagingServer.url, object : WebSocketListener() {

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        scope.launch { group.sendMessage("[${messagingServer.name}]服务器关闭:$code") }
                        messagingServer.status = ConnectStatus.DISCONNECT
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        scope.launch {
                            when(t){
                                is ConnectException -> group.sendMessage("[${messagingServer.name}]连接失败,请检查服务器后重新监听")
                                is EOFException -> group.sendMessage("[${messagingServer.name}]连接异常,请重新监听")
                                else -> logger.error(t)
                            }
                        }
                        messagingServer.status = ConnectStatus.DISCONNECT
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        scope.launch {
                            runCatching {
                                Json.decodeFromString<UserMessage>(text)
                            }.onSuccess {
                                group.sendMessage(PlainText("[${messagingServer.name}] <${it.name}> ").plus(it.toMiraiMessage()))
                            }.onFailure {
                                logger.error(it.localizedMessage)
                            }
                        }
                    }

                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        scope.launch { group.sendMessage("连接[${messagingServer.name}]服务器成功") }
                        messagingServer.status = ConnectStatus.CONNECTION
                    }
                })
            }

            always {msg ->
                val messagingServer = messagingServerList.find { it.groupId == group.id && msg.trim().startsWith(it.name) } ?: return@always
                // 服务器未连接直接返回
                if(messagingServer.status == ConnectStatus.DISCONNECT) {
                    group.sendMessage("[${messagingServer.name}]服务器未监听")
                    return@always
                }
                val message = UserMessage(sender.nameCardOrNick, msg.trim().substring(messagingServer.name.length))
                // 如果服务器连接了就发送消息
                messagingServer.webSocketUtils?.send(Json.encodeToString(message))
            }

        }
    }
}