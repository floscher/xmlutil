/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.serialization.compat

import kotlinx.serialization.KSerialLoader
import kotlinx.serialization.KSerialSaver

interface SerializationStrategy<in T>: KSerialSaver<T> {
    fun serialize(output: Encoder, obj: T)

    val descriptor: SerialDescriptor
}

interface DeserializationStrategy<out T>: KSerialLoader<@kotlin.UnsafeVariance T> {
    fun deserialize(input: Decoder, oldValue: @kotlin.UnsafeVariance T? = null): T
    val descriptor: SerialDescriptor
}

interface Serializer<T>: SerializationStrategy<T>, DeserializationStrategy<T> {
    override val descriptor: SerialDescriptor
}