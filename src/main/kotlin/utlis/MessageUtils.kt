/*
 * Copyright 2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.utlis

import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.buildMessageChain
import top.limbang.entity.UserMessage

private val regex = "@([0-9]{6,10})".toRegex()

fun UserMessage.toMiraiMessage() = buildMessageChain {
    +regex.replace(message) {
        +At(it.groupValues[1].toLong())
        ""
    }
}
