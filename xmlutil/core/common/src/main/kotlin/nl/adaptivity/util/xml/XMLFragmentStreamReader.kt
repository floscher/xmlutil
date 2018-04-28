/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util.xml


/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper
 * elements, and then ignoring those on reading.
 * Created by pdvrieze on 04/11/15.
 */
expect class XMLFragmentStreamReader : XmlDelegatingReader {

    companion object {
        fun from(fragment: ICompactFragment): XMLFragmentStreamReader
    }


}

//fun CompactFragment.getXmlReader(): XmlReader = XMLFragmentStreamReader.from(this)