/*
 * Copyright 2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.limbang.utlis.WebSocketUtils

@Serializable
data class MessagingServer(
    val groupId: Long,
    var name: String,
    var url: String,
    @Transient var status: ConnectStatus = ConnectStatus.DISCONNECT,
    @Transient var webSocketUtils: WebSocketUtils? = null
)
