/*
 * Copyright 2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import top.limbang.entity.MessagingServer

object MessagingData : AutoSavePluginData("messaging") {
    @ValueDescription("存放不同群的转发服务器信息")
    var messagingServerList: MutableList<MessagingServer> by value()
}

