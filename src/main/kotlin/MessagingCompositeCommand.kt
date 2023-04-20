/*
 * Copyright (c) 2022-2023 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang


import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.broadcast
import top.limbang.Messaging.isLoadGeneralPluginInterface
import top.limbang.MessagingData.messagingServerList
import top.limbang.entity.MessagingServer
import top.limbang.mirai.event.GroupRenameEvent

object MessagingCompositeCommand : CompositeCommand(Messaging, "msg") {

    @OptIn(ConsoleExperimentalApi::class)
    @SubCommand
    @Description("添加转发服务器")
    suspend fun UserCommandSender.add(
        @Name("名称") name: String,
        @Name("地址") address: String,
        @Name("端口") port: Int,
        @Name("启用安全连接") enableSecure: Boolean = false,
    ) {
        if (isNotGroup()) return
        val url = if (enableSecure) "wss://$address${if (port == 443) "" else ":$port"}"
        else "ws://$address${if (port == 80) "" else ":$port"}"
        val messagingServer = messagingServerList.find { it.name == name }
        if (messagingServer == null)
            messagingServerList.add(MessagingServer(groupId = subject.id, name = name, url = url))
        else
            messagingServer.url = url
        sendMessage("添加[$name]服务器:成功")
    }

    @SubCommand
    @Description("改名服务器")
    suspend fun UserCommandSender.rename(@Name("名称") name: String, @Name("新名称") newName: String) {
        if (isNotGroup()) return
        if (renameInstance(name, newName, subject.id, false)) sendMessage("原[$name]改为[$newName]成功")
        else sendMessage("未找到[$name]服务器")
    }

    internal suspend fun renameInstance(name: String, newName: String, groupID: Long, isEvent: Boolean): Boolean {
        val messagingServer = messagingServerList.find { it.groupId == groupID && it.name == name }
        return if (messagingServer != null) {
            messagingServer.name = newName

            if (isLoadGeneralPluginInterface) {
                // 发布改名广播
                // 不是事件就发布改名广播
                if (!isEvent) GroupRenameEvent(groupID, Messaging.id, name, newName).broadcast()
            }
            true
        } else {
            false
        }
    }

    @SubCommand
    @Description("删除转发服务器")
    suspend fun UserCommandSender.delete(@Name("名称") name: String) {
        if (isNotGroup()) return
        val messagingServer = messagingServerList.find { it.groupId == subject.id && it.name == name }
        val result = messagingServerList.remove(messagingServer)
        sendMessage("删除[$name]服务器: ${if (result) "成功" else "失败"}")
    }

    @SubCommand
    @Description("查看列表")
    suspend fun UserCommandSender.list() {
        if (isNotGroup()) return
        var msg = "转发服务器列表如下:"
        val length = msg.length
        messagingServerList.filter { it.groupId == subject.id }.forEach { msg += "\n[${it.name}] ${it.url}" }
        if (msg.length == length) sendMessage("本群还未添加转发服务器...") else sendMessage(msg)
    }


    private suspend fun UserCommandSender.isNotGroup() = (subject !is Group).also {
        if (it) sendMessage("请在群内发送命令")
    }
}

