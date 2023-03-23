/*
 * Copyright (c) 2022-2023 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.utlis

import top.limbang.entity.UserMessage
import kotlin.test.Test

private val regex = "@([0-9]{6,12})\\s".toRegex()


internal class MessageUtilsTest{

    @Test
    fun toMiraiMessage(){
        println(UserMessage("limbang","123").toMiraiMessage())
        println(UserMessage("limbang","@123456 123").toMiraiMessage())
        println(UserMessage("limbang","@123456 123 @123456 666 @123456 999").toMiraiMessage())
    }
}
