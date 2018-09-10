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

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.*

object Canary {

    private val saverMap = mutableMapOf<KSerialSaver<*>, ExtInfo>()
    private val loaderMap = mutableMapOf<KSerialLoader<*>, ExtInfo>()

    fun <T> extInfo(saver: KSerialSaver<T>, obj: T): ExtInfo {
        val current = saverMap[saver]
        if (current != null) return current

        val output = CanaryOutput()
        saver.save(output, obj)
        val new = output.extInfo()

        if (new.childInfo.none { it.type == ChildType.UNKNOWN }) {
            saverMap[saver] = new
        }

        return new
    }

    fun <T> extInfo(loader: KSerialLoader<T>): ExtInfo {
        val current = loaderMap[loader]
        if (current != null) return current

        val input = CanaryInput()
        load(input, loader)
        val new = input.extInfo()

        if (new.childInfo.none { it.type == ChildType.UNKNOWN }) {
            loaderMap[loader] = new
        }

        return new

    }

    internal fun <T> load(input: CanaryInput,
                          loader: KSerialLoader<T>) {
        try {
            loader.load(input)
        } catch (e: CanaryInput.SuspendException) {
            if (e.finished) {
                return
            }
        }
        while (true) {
            try {
                loader.load(input)
                throw IllegalStateException("This should not be reachable")
            } catch (e: CanaryInput.SuspendException) {
                if (e.finished) break
            } catch (e: UnknownFieldException) {
                throw IllegalStateException("Could not gather information for loader $loader on field ${input.currentChildIndex} with info: ${input.childInfo[input.currentChildIndex]}", e)
            }

        }
    }

    fun <T> pollInfo(saver: KSerialSaver<T>): ExtInfo? {
        return saverMap[saver]
    }


    fun <T> pollInfo(loader: KSerialLoader<T>): ExtInfo? {
        return loaderMap[loader]
    }

    internal fun childInfoForClassDesc(desc: KSerialClassDesc): Array<ChildInfo> {
        return when (desc.kind) {
            KSerialClassKind.MAP,
            KSerialClassKind.SET,
            KSerialClassKind.LIST
                 -> arrayOf(ChildInfo("count", classAnnotations = emptyList()), ChildInfo("values", classAnnotations = emptyList()))

            else -> Array(desc.associatedFieldsCount) {
                ChildInfo(desc.getElementName(it), desc.getAnnotationsForIndex(it), emptyList())
            }
        }
    }


}