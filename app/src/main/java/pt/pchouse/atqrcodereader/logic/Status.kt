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

/**
 * Valid values: <br>
 * Invoice - N, S, A, R, F <br>
 * Movement - N, T, A, F, R <br>
 * Payments - N, A <br>
 * Work - N, A, F <br>
 */
enum class Status {
    @StatusProperties N,
    @StatusProperties A,
    @StatusProperties([Group.Movement]) T,
    @StatusProperties([Group.Invoice, Group.Insurance]) S,
    @StatusProperties([Group.Invoice, Group.Movement, Group.Insurance]) R,
    @StatusProperties([Group.Invoice, Group.Movement, Group.Working, Group.Insurance]) F,
}