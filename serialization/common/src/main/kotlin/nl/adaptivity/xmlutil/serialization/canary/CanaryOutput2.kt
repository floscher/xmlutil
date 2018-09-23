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
import kotlinx.serialization.internal.EnumSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.UnitSerializer
import nl.adaptivity.xmlutil.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.compat.*
import kotlin.reflect.KClass


class CanaryOutput2(private var parentClassDesc: KSerialClassDesc? = null, val isDeep: Boolean = true) : ElementValueOutput() {
    private var index: Int = -1
    var kind: KSerialClassKind? = null
        private set
    var type: ChildType = ChildType.UNKNOWN
        private set
    val serialKind: SerialKind? get() = kind?.asSerialKind(type)

    // If we are not deep the descriptor is going to be incomplete (only used for nullability)
    var complete: Boolean = isDeep
        private set


    private var childInfo: Array<ChildInfo?> = emptyArray()

    var classAnnotations: List<Annotation> = emptyList()
        private set

    var isNullable: Boolean = false
        private set

    private var currentElementIsNullable: Boolean = false

//    private var parentClassDesc: KSerialClassDesc? = null

    override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
        if (index<0)
            throw IndexOutOfBoundsException()
        this.index = index
        return true
    }

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
        parentClassDesc = desc
        kind = desc.kind
        classAnnotations = desc.getAnnotationsForClass()

        childInfo = childInfoForClassDesc(desc)
        return this
    }

    override fun writeEnd(desc: KSerialClassDesc) {
        if (type == ChildType.UNKNOWN) {
            type = ChildType.ELEMENT
        }
    }

    override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
        if (index < 0) throw IllegalStateException()
        if (index <childInfo.size) {
            val poll = Canary.pollDesc(saver)

            if (poll != null) {
                val isNullable = CanaryOutput2(isDeep = false).let { saver.save(it, value); it.isNullable }
                childInfo[index] = ChildInfo(poll, isNullable)
            } else if(isDeep) {
                CanaryOutput2().also {
                    saver.save(it, value)
                    val desc = it.serialDescriptor()
                    if (it.complete) {
                        Canary.registerDesc(saver, desc)
                    } else {
                        complete = false // If children are not complete we are not
                    }
                    childInfo[index] = ChildInfo(desc, currentElementIsNullable)
                }
            }
        }
        index = -1
    }

    private fun setCurrentPrimitiveChildType(type: ChildType) {
        assert(type.isPrimitive)
        if (index < 0) {
            kind = KSerialClassKind.PRIMITIVE
            parentClassDesc = type.primitiveSerializer.serialClassDesc
            this.type = type
        } else if (index < childInfo.size) {
            val desc = ExtSerialDescriptor(type.primitiveSerializer.serialClassDesc, type.serialKind, BooleanArray(0), emptyArray())
            childInfo[index] = ChildInfo(desc, currentElementIsNullable)
        }
        index = -1
        currentElementIsNullable = false
    }

    override fun writeBooleanValue(value: Boolean) {
        setCurrentPrimitiveChildType(ChildType.BOOLEAN)
    }

    override fun writeByteValue(value: Byte) {
        setCurrentPrimitiveChildType(ChildType.BYTE)
    }

    override fun writeCharValue(value: Char) {
        setCurrentPrimitiveChildType(ChildType.CHAR)
    }

    override fun writeDoubleValue(value: Double) {
        setCurrentPrimitiveChildType(ChildType.DOUBLE)
    }

    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) {
        if (index<0) {
            type = ChildType.ENUM
        } else if (index<childInfo.size) {
            val serializer = EnumSerializer(enumClass)
            val desc = ExtSerialDescriptor(serializer.serialClassDesc, UnionKind.ENUM, BooleanArray(0), emptyArray())
            childInfo[index] = ChildInfo(desc, currentElementIsNullable)
            currentElementIsNullable = false
            index = -1
        }
    }

    override fun writeFloatValue(value: Float) {
        setCurrentPrimitiveChildType(ChildType.FLOAT)
    }

    override fun writeIntValue(value: Int) {
        setCurrentPrimitiveChildType(ChildType.INT)
    }

    override fun writeLongValue(value: Long) {
        setCurrentPrimitiveChildType(ChildType.LONG)
    }

    override fun writeNonSerializableValue(value: Any) {
        if (index<0) {
            type = ChildType.UNKNOWN
        } else if (index<childInfo.size) {
            childInfo[index] = null
        }
        currentElementIsNullable = false
        index = -1
    }

    override fun writeNotNullMark() {
        if (index>=0) {
            currentElementIsNullable = true
        } else {
            isNullable = true
        }
    }

    override fun writeNullValue() {
        if (index>=0) {
            currentElementIsNullable = true
            if(index<childInfo.size) {
                childInfo[index] = DUMMYCHILDINFO
                complete = false
            }
        } else {
            isNullable = true
        }
        index = -1
    }

    override fun writeShortValue(value: Short) {
        setCurrentPrimitiveChildType(ChildType.SHORT)
    }

    override fun writeStringValue(value: String) {
        setCurrentPrimitiveChildType(ChildType.STRING)
    }

    override fun writeUnitValue() {
        setCurrentPrimitiveChildType(ChildType.UNIT)
    }

    fun serialDescriptor(): SerialDescriptor {
        return ExtSerialDescriptor(requireNotNull(parentClassDesc, {"parentClassDesc"}),
                                   (kind ?: KSerialClassKind.PRIMITIVE).asSerialKind(type),
                                   BooleanArray(childInfo.size) { childInfo[it]?.isNullable ?: false },
                                   Array(childInfo.size) {
                                       requireNotNull(requireNotNull(childInfo[it],{"childInfo"}).descriptor, {"descriptor"}) })
    }

    private class ChildInfo(var descriptor: SerialDescriptor? = null,
                            var isNullable: Boolean = false)


    private fun childInfoForClassDesc(desc: KSerialClassDesc): Array<ChildInfo?> = when(desc.kind) {

        KSerialClassKind.PRIMITIVE,
        KSerialClassKind.ENUM,
        KSerialClassKind.OBJECT,
        KSerialClassKind.UNIT        -> emptyArray()
        KSerialClassKind.LIST,
        KSerialClassKind.SET,
        KSerialClassKind.MAP         -> arrayOf(null, DUMMYCHILDINFO)
        KSerialClassKind.POLYMORPHIC,
        KSerialClassKind.ENTRY       -> arrayOfNulls(2)
        else -> arrayOfNulls(desc.associatedFieldsCount)
    }

    companion object {

        object DUMMYSERIALDESC: SerialDescriptor {
            override val name: String get() = throw UnsupportedOperationException("Dummy descriptors have no names")
            override val kind: KSerialClassKind get() = throw UnsupportedOperationException("Dummy descriptors have no kind")
            override val extKind: SerialKind get() = throw UnsupportedOperationException("Dummy descriptors have no kind")
            override val elementsCount: Int get() = 0

            override fun getElementName(index: Int) = throw UnsupportedOperationException("No children in dummy")

            override fun getElementIndex(name: String) = throw UnsupportedOperationException("No children in dummy")

            override fun getEntityAnnotations(): List<Annotation> = emptyList()

            override fun getElementAnnotations(index: Int) = throw UnsupportedOperationException("No children in dummy")

            override fun getElementDescriptor(index: Int): SerialDescriptor {
                throw UnsupportedOperationException("No children in dummy")
            }

            override fun isNullable(index: Int) = throw UnsupportedOperationException("No children in dummy")

            override fun isElementOptional(index: Int) =
                    throw UnsupportedOperationException("No children in dummy")
        }

        private val DUMMYCHILDINFO = ChildInfo(DUMMYSERIALDESC)

    }
}

