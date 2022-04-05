/*
 * Copyright (c) 2022. Reflexão Estudos e Sistemas Informáticos, Lda.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/**
 * https://stackoverflow.com/a/60325567/6397645
 */
@file:JvmName("Log")

package android.util


fun e(tag: String, message: String): Int {
    println("Error: $tag: $message")
    return 0
}

fun d(tag: String, message: String): Int {
    println("Debug: $tag: $message")
    return 0
}

fun i(tag: String, message: String): Int {
    println("Info: $tag: $message")
    return 0
}

fun w(tag: String, message: String): Int {
    println("Warning: $tag: $message")
    return 0
}
