/*
 * Copyright 2020 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.util

import android.util.Log
import kotlin.math.min

/**
 * Parse the 'compressed' binary form of Android XML docs
 * such as AndroidManifest.xml in APK
 */
internal class Xml(xml: ByteArray, private val mode: Int = MODE_MARK, attr: Array<String> = emptyArray()){
    companion object {
        /** terminates the process when desired attribute is found */
        const val MODE_FIND = 0
        /** the process still continues after desired attribute is found */
        const val MODE_MARK = 1
        /** get the decompressed xml content */
        const val MODE_CONTENT = 2
        const val endDocTag = 0x00100101
        const val startTag = 0x00100102
        const val endTag = 0x00100103
    }

    /** the content decompressed from xml, available when MODE_ALL */
    val content: StringBuilder = StringBuilder()
    /** the value of the desired attribute */
    var attrAsset: String = ""

    private fun compXmlString(xml: ByteArray, sitOff: Int, stOff: Int, strInd: Int): String? {
        if (strInd < 0) return null
        val strOff = stOff + LEW(xml, sitOff + strInd * 4)
        return compXmlStringAt(xml, strOff)
    }

    private var spaces = "                                             "
    private fun prtIndent(indent: Int, str: String) {
        if (mode != MODE_CONTENT) return
        content.append('\n').append(spaces.substring(0, min(indent * 2, spaces.length)) + str)
    }

    /**
     * compXmlStringAt -- Return the string stored in StringTable format at
     * offset strOff.  This offset points to the 16 bit string length, which
     * is followed by that number of 16 bit (Unicode) chars.
     */
    private fun compXmlStringAt(arr: ByteArray, strOff: Int): String {
        val strLen = arr[strOff + 1].toInt() shl 8 and 0xff00 or (arr[strOff].toInt() and 0xff)
        val chars = ByteArray(strLen)
        for (ii in 0 until strLen) {
            val s = strOff + 2 + ii * 2
            if (s < arr.size) {
                chars[ii] = arr[s]
            }
        }
        return String(chars)  // Hack, just use 8 byte chars
    }


    /**
     * LEW -- Return value of a Little Endian 32 bit word from the byte array
     * at offset off.
     */
    private fun LEW(arr: ByteArray, off: Int): Int {
        return (arr[off + 3].toInt() shl 24 and -0x1000000 or (arr[off + 2].toInt() shl 16 and 0xff0000)
                or (arr[off + 1].toInt() shl 8 and 0xff00) or (arr[off].toInt() and 0xFF))
    }

    init {
        /**
         * Compressed XML file/bytes starts with 24x bytes of data,
         * 9 32 bit words in little endian order (LSB first):
         * 0th word is 03 00 08 00
         * 3rd word SEEMS TO BE:  Offset at then of StringTable
         * 4th word is: Number of strings in string table
         * WARNING: Sometime I indiscriminently display or refer to word in
         * little endian storage format, or in integer format (ie MSB first).
         */
        val numbStrings = LEW(xml, 4 * 4)

        /**
         * StringIndexTable starts at offset 24x, an array of 32 bit LE offsets
         * of the length/string data in the StringTable.
         */
        val sitOff = 0x24  // Offset of start of StringIndexTable

        /**
         * StringTable, each string is represented with a 16 bit little endian
         * character count, followed by that number of 16 bit (LE) (Unicode) chars.
         */
        val stOff = sitOff + numbStrings * 4  // StringTable follows StrIndexTable

        /**
         * XMLTags, The XML tag tree starts after some unknown content after the
         * StringTable.  There is some unknown data after the StringTable, scan
         * forward from this point to the flag for the start of an XML start tag.
         */
        var xmlTagOff = LEW(xml, 3 * 4)  // Start from the offset in the 3rd word.
        // Scan forward until we find the bytes: 0x02011000(x00100102 in normal int)
        run {
            var ii = xmlTagOff
            while (ii < xml.size - 4) {
                if (LEW(xml, ii) == startTag) {
                    xmlTagOff = ii
                    break
                }
                ii += 4
            }
        } // end of hack, scanning for start of first start tag

        /**
         * XML tags and attributes:
         * Every XML start and end tag consists of 6 32 bit words:
         * 0th word: 02011000 for startTag and 03011000 for endTag
         * 1st word: a flag?, like 38000000
         * 2nd word: Line of where this tag appeared in the original source file
         * 3rd word: FFFFFFFF ??
         * 4th word: StringIndex of NameSpace name, or FFFFFFFF for default NS
         * 5th word: StringIndex of Element Name
         * (Note: 01011000 in 0th word means end of XML document, endDocTag)
         *
         * Start tags (not end tags) contain 3 more words:
         * 6th word: 14001400 meaning??
         * 7th word: Number of Attributes that follow this tag(follow word 8th)
         * 8th word: 00000000 meaning??
         *
         * Attributes consist of 5 words:
         * 0th word: StringIndex of Attribute Name's Namespace, or FFFFFFFF
         * 1st word: StringIndex of Attribute Name
         * 2nd word: StringIndex of Attribute Value, or FFFFFFF if ResourceId used
         * 3rd word: Flags?
         * 4th word: str ind of attr value again, or ResourceId of value
         */
        /**
         * TMP, dump string table to tr for debugging
         * tr.addSelect("strings", null);
         * for (int ii=0; ii<numbStrings; ii++) {
         * //Length of string starts at StringTable plus offset in StrIndTable
         * String str = compXmlString(xml, sitOff, stOff, ii);
         * tr.add(String.valueOf(ii), str);
         * }
         * tr.parent();
         */

        // Step through the XML tree element tags and attributes
        var off = xmlTagOff
        var indent = 0
        var startTagLineNo = -2
        var attrDepth = -1
        // indicate that value should be retrieved because the attr depth has reached the end
        var flagAttr = false
        // indicate that attr[0] is found
        var isHeadReached = false
        while (off < xml.size) {
            val tag0 = LEW(xml, off)
            //int tag1 = LEW(xml, off+1*4);
            val lineNo = LEW(xml, off + 2 * 4)
            //int tag3 = LEW(xml, off+3*4);
            val nameNsSi = LEW(xml, off + 4 * 4)
            val nameSi = LEW(xml, off + 5 * 4)

            if (tag0 == startTag) { // XML START TAG
                val tag6 = LEW(xml, off + 6 * 4)  // Expected to be 14001400
                val numbAttrs = LEW(xml, off + 7 * 4)  // Number of Attributes to follow
                //int tag8 = LEW(xml, off+8*4);  // Expected to be 00000000
                off += 9 * 4  // Skip over 6+3 words of startTag data
                val name = compXmlString(xml, sitOff, stOff, nameSi)
                if (mode == MODE_FIND){
                    if (!flagAttr){
                        if (!isHeadReached && attr[0] == name) isHeadReached = true
                        if (isHeadReached) {
                            attrDepth++
                            if (attr[attrDepth] == name && attrDepth == attr.size - 2) {
                                flagAttr = true
                                attrDepth++
                            }
                        }
                    }
                }

                //tr.addSelect(name, null);
                startTagLineNo = lineNo

                // Look for the Attributes
                val sb = StringBuffer()
                for (ii in 0 until numbAttrs) {
                    val attrNameNsSi = LEW(xml, off)  // AttrName Namespace Str Ind, or FFFFFFFF
                    val attrNameSi = LEW(xml, off + 1 * 4)  // AttrName String Index
                    val attrValueSi = LEW(xml, off + 2 * 4) // AttrValue Str Ind, or FFFFFFFF
                    val attrFlags = LEW(xml, off + 3 * 4)
                    val attrResId = LEW(xml, off + 4 * 4)  // AttrValue ResourceId or dup AttrValue StrInd
                    off += 5 * 4  // Skip over the 5 words of an attribute

                    val attrName = compXmlString(xml, sitOff, stOff, attrNameSi)
                    val attrValue = if (attrValueSi != -1)
                        compXmlString(xml, sitOff, stOff, attrValueSi)
                    else
                        "0x" + Integer.toHexString(attrResId)
                    if (mode == MODE_FIND){
                        if (flagAttr && attrName == attr[attrDepth]) {
                            attrAsset = attrResId.toString()
                            attrDepth = attr.size
                            break
                        }
                    }

                    sb.append(" $attrName=\"$attrValue\"")
                    //tr.add(attrName, attrValue);
                }
                prtIndent(indent, "<$name$sb>")
                indent++

            } else if (tag0 == endTag) { // XML END TAG
                indent--
                off += 6 * 4  // Skip over 6 words of endTag data
                val name = compXmlString(xml, sitOff, stOff, nameSi)
                prtIndent(indent, "</$name>  (line $startTagLineNo-$lineNo)")
                //tr.parent();  // Step back up the NobTree
            } else if (tag0 == endDocTag) {
                break
            } else {
                Log.w("Xml", "Unrecognized tag code '${Integer.toHexString(tag0)}' at offset " + off)
                break
            }
            if (mode == MODE_FIND && (flagAttr || attrDepth >= attr.size)) break
        }
        Log.i("Xml", "Operation ends at offset $off")
    }
}