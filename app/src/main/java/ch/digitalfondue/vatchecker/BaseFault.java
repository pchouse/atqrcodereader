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
package ch.digitalfondue.vatchecker;

import java.util.function.Function;

abstract class BaseFault<T extends Enum<T>> {

    private final String faultCode;
    private final String fault;
    private final T faultType;

    protected BaseFault(String faultCode, String fault, Function<String, T> converter, T defaultValue) {
        this.faultCode = faultCode;
        this.fault = fault;
        this.faultType = tryParse(fault, converter, defaultValue);
    }

    public String getFault() {
        return fault;
    }

    public String getFaultCode() {
        return faultCode;
    }

    public T getFaultType() {
        return faultType;
    }

    private static <T extends Enum<T>> T tryParse(String fault, Function<String, T> converter, T defaultValue) {
        try {
            return converter.apply(fault);
        } catch (IllegalArgumentException | NullPointerException e) {
            return defaultValue;
        }
    }
}
