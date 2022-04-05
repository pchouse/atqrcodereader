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

package pt.pchouse.atqrcodereader.logic

object VatRate {

    enum class Type{
        ISE,
        RED,
        INT,
        NOR
    }

   fun pt(type: Type): String{
       return when(type){
           Type.ISE -> "0%"
           Type.RED -> "6%"
           Type.INT -> "13%"
           Type.NOR -> "23%"
       }
   }

    fun ptAz(type: Type): String{
        return when(type){
            Type.ISE -> "0%"
            Type.RED -> "4%"
            Type.INT -> "9%"
            Type.NOR -> "18%"
        }
    }

    fun ptAm(type: Type): String{
        return when(type){
            Type.ISE -> "0%"
            Type.RED -> "5%"
            Type.INT -> "12%"
            Type.NOR -> "22%"
        }
    }

}