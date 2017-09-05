/*
 * Copyright (C) 2015-17 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

/*
 * Read an XML file from Outlaw and produce the packed partition files for use in the
 * Apple II version of MythOS.
 */

package org.badvision

import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Calendar
import java.util.zip.GZIPInputStream
import java.util.LinkedHashMap
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import groovy.json.JsonOutput
import groovy.util.Node

/**
 *
 * @author mhaye
 */
class A2PackPartitions
{
    def TRANSPARENT_COLOR = 15

    def TYPE_CODE        = 1
    def TYPE_2D_MAP      = 2
    def TYPE_3D_MAP      = 3
    def TYPE_TILE_SET    = 4
    def TYPE_TEXTURE_IMG = 5
    def TYPE_SCREEN      = 6
    def TYPE_FONT        = 7
    def TYPE_MODULE      = 8
    def TYPE_BYTECODE    = 9
    def TYPE_FIXUP       = 10
    def TYPE_PORTRAIT    = 11

    static final int FLOPPY_SIZE = 35*8  // good old 140k floppy = 280 blks
    static final int AC_KLUDGE = 2      // minus 1 to work around last-block bug in AppleCommander
    static final int DOS_OVERHEAD = 3 // only 3 blks overhead! ProRWTS is so freaking amazing.
    static final int SAVE_GAME_SIZE = 7 // 6 blocks data, 1 block index
    static final int MAX_DISKS = 20 // for now this should be way more than enough

    def typeNumToName    = [1:  "Code",
                            2:  "2D map",
                            3:  "3D map",
                            4:  "Tile set",
                            5:  "Texture image",
                            6:  "Full screen image",
                            7:  "Font",
                            8:  "Code",
                            9:  "Code",
                            10: "Code",
                            11: "Portrait image"]

    def mapNames  = [:]  // map name (and short name also) to map.2dor3d, map.num
    def sysCode   = [:]  // memory manager
    def code      = [:]  // code name to code.num, code.buf
    def maps2D    = [:]  // map name to map.num, map.buf
    def maps3D    = [:]  // map name to map.num, map.buf
    def tiles     = [:]  // tile id to tile.buf
    def tileSets  = [:]  // tileset name to tileset.num, tileset.buf
    def avatars   = [:]  // avatar tile name to tile num (within the special tileset)
    def compassTiles = [:] // compass direction (north, east, south, west) to tile num (within global tileset)
    def clockTiles = [:] // clock time (e.g. 12:00, 1:30) to tile num (within global tileset)
    def lampTiles = []   // animated series of lamp tiles
    def textures  = [:]  // img name to img.num, img.buf
    def frames    = [:]  // img name to img.num, img.buf
    def portraits = [:]  // img name to img.num, img.buf
    def fonts     = [:]  // font name to font.num, font.buf
    def modules   = [:]  // module name to module.num, module.buf
    def bytecodes = [:]  // module name to bytecode.num, bytecode.buf
    def fixups    = [:]  // module name to fixup.num, fixup.buf
    def gameFlags = [:]  // flag name to number

    def itemNameToFunc = [:]
    def playerNameToFunc = [:]

    def allLootCodes

    def requiredGlobalScripts = ["New Game", "Help",
                                 "Combat win", "Combat intro", "Combat prompt", "Enemy intro", "Death",
                                 "Level XP", "Level SP"]
    def globalScripts = [:]
    def lastSysModule

    def compressor = new Lx47Algorithm()

    def debugCompression = false

    def currentContext = []
    def nWarnings = 0
    def warningBuf = new StringBuilder()

    def binaryStubsOnly = false
    final int CACHE_VERSION = 1  // increment to force full rebuild
    def cache = ["version": CACHE_VERSION]
    def buildDir
    def reportWriter
    def memUsage3D = []
    def chunkSizes = [:]

    def curMapName = null
    def coreSize
    def resourceDeps = [:]
    def allMaps = []
    def allChunks = [:] as LinkedHashMap
    def part1Chunks = [:] as LinkedHashMap

    def stats = [
        "Intelligence": "@S_INTELLIGENCE",
        "Strength":     "@S_STRENGTH",
        "Agility":      "@S_AGILITY",
        "Stamina":      "@S_STAMINA",
        "Spirit":       "@S_SPIRIT",
        "Luck":         "@S_LUCK",
        "Health":       "@S_HEALTH",
        "Max health":   "@S_MAX_HEALTH",
        "Aiming":       "@S_AIMING",
        "Hand to hand": "@S_HAND_TO_HAND",
        "Dodging":      "@S_DODGING",
        "Gold":         "@S_GOLD",
        "XP":           "@S_XP",
        "SP":           "@S_SP"
    ]

    def predefStrings = stats + [
        "Enter": "@S_ENTER",
        "Leave": "@S_LEAVE",
        "Use":   "@S_USE"
    ]

    // 2D map sectioning constants
    def TILES_PER_ROW = 22
    def ROWS_PER_SECTION = 23


    /**
     * Keep track of context within the XML file, so we can spit out more useful
     * error and warning messages.
     */
    def withContext(name, closure)
    {
        def threw = false
        try {
            currentContext << name
            closure()
        }
        catch (Throwable e) {
            threw = true
            throw e
        }
        finally {
            // Preserve context in the case of an exception
            if (!threw)
                currentContext.pop()
        }
    }

    def getContextStr()
    {
        return currentContext.join(" -> ")
    }

    def printWarning(str)
    {
        def msg = "Warning ${currentContext ? "in "+getContextStr() : ""}: $str\n"
        System.out.print(msg)
        warningBuf.append(msg)
        ++nWarnings
    }

    def titleCase(inStr)
    {
        return inStr[0].toUpperCase() + inStr.substring(1)
    }

    def escapeString(inStr)
    {
        // Commonly used strings (e.g. event handler names, attributes)
        if (inStr in predefStrings)
            return predefStrings[inStr]

        def buf = new StringBuilder()
        buf << '\"'
        def prev = '\0'
        def count = 0
        def stop = false
        inStr.eachWithIndex { ch, idx ->
            if (!stop) {
                if (count >= 255) {
                    printWarning("String must be 254 characters or less. " +
                        "Everything after the following will be discarded: '${inStr[0..idx]}'")
                    stop = true
                }
                else if (ch == '^') {
                    if (prev == '^') {
                        buf << ch
                        ch = 'x' // so next char not treated as special
                    }
                }
                else if (ch == '\"')
                    buf << "\\\""
                else if (prev == '^') {
                    def cp = Character.codePointAt(ch.toUpperCase(), 0)
                    // ^A = ctrl-A; ^M = ctrl-M (carriage return); ^Z = ctrl-Z; ^` = space
                    if (cp == 96) // ^`
                        buf << " "
                    else if (cp == 77) // ^M
                        buf << "\\n"
                    else if (cp > 64 && cp < 96)
                        buf << "\\\$" << String.format("%02X", cp - 64)
                    else
                        printWarning("Unrecognized control code '^" + ch + "'")
                }
                else
                    buf << ch
                ++count
                prev = ch
            }
        }
        buf << '\"'
        return buf.toString()
    }

    def parseMap(map, tiles, quick = false)
    {
        // Parse each row of the map
        map.chunk.row.collect
        {
            // The map data is a space-separated string of tile ids. Look up those
            // tiles.
            //
            it.text().split(" ").collect { tileId ->
                (tileId == "_") ? null : quick ? tiles[0] : tiles.find{ it.@id == tileId }
            }
        }
    }

    def calcImagesHash(imgEls)
    {
        def md = MessageDigest.getInstance("MD5")
        imgEls.each { md.update(it.toString().getBytes()) }
        return DatatypeConverter.printHexBinary(md.digest())
    }

    def pixelize(dataEl)
    {
        def nBytes = dataEl.@width as int
        def nLines = dataEl.@height as int
        def hexStr = dataEl.text()
        return (0..<nLines).collect { lineNum ->
            def outRow = []
            def pix = 0
            def pixBits = 0
            for (def byteNum in 0..<nBytes) {
                def pos = (lineNum*nBytes + byteNum) * 2 // two hex chars per byte
                def val = Integer.parseInt(hexStr[pos..pos+1], 16)
                for (def bitNum in 0..6) {
                    if (pixBits == 0) // [ref BigBlue1_40]
                        pix = (val & 0x80) ? 4 : 0 // grab high bit of first byte of pix
                    if (val & (1<<bitNum))
                        pix |= (1<<pixBits)
                    pixBits++
                    if (pixBits == 2) {
                        outRow.add(pix)
                        pixBits = 0
                    }
                }
            }
            return outRow
        }
    }

    def parseTexture(imgEl)
    {
        // Locate the data for the Apple II (as opposed to C64 etc.)
        def data = imgEl.displayData?.find { it.@platform == "AppleII" }
        assert data : "image '${imgEl.@name}' missing AppleII platform data"
        def rows = pixelize(data)

        // Retain only the upper-left 64 lines x 32 pixels
        return rows[0..63].collect { it[0..31] }
    }

    /*
     * Parse raw tile image data and return it as a buffer.
     */
    def parseTileData(imgEl)
    {
        // Locate the data for the Apple II (as opposed to C64 etc.)
        def dataEl = imgEl.displayData?.find { it.@platform == "AppleII" }
        assert dataEl : "image '${imgEl.@name}' missing AppleII platform data"

        // Parse out the hex data on each line and add it to a buffer.
        def hexStr = dataEl.text()
        def outBuf = ByteBuffer.allocate(50000)
        for (def pos = 0; pos < hexStr.size(); pos += 2) {
            def val = Integer.parseInt(hexStr[pos..pos+1], 16)
            outBuf.put((byte)val)
        }

        // All done. Return the buffer.
        return outBuf
    }

    /*
     * Parse raw frame image data, rearrange it in screen order, and return it as a buffer.
     */
    def parseFrameData(imgEl)
    {
        // Locate the data for the Apple II (as opposed to C64 etc.)
        def dataEl = imgEl.displayData?.find { it.@platform == "AppleII" }
        assert dataEl : "image '${imgEl.@name}' missing AppleII platform data"
        //println "Packing frame image '${imgEl.@name}'."

        // Parse out the hex data on each line and add it to a buffer.
        def hexStr = dataEl.text()
        def arr = new byte[7680]
        def srcPos = 0
        def dstPos = 0

        // Process each line
        (0..<192).each { y ->

            // Process all 40 bytes in one line
            (0..<40).each { x ->
                arr[dstPos+x] = Integer.parseInt(hexStr[srcPos..srcPos+1], 16)
                srcPos += 2 // two hex chars = one byte
            }

            dstPos += 40
        }

        // Put the results into the buffer
        def outBuf = ByteBuffer.allocate(dstPos)
        outBuf.put(arr, 0, dstPos)

        // All done. Return the buffer.
        return outBuf
    }

    /*
     * Parse raw frame image data, only 126x128 pixels
     */
    def parse126Data(imgEl)
    {
        // Locate the data for the Apple II (as opposed to C64 etc.)
        def dataEl = imgEl.displayData?.find { it.@platform == "AppleII" }
        assert dataEl : "image '${imgEl.@name}' missing AppleII platform data"

        // Parse out the hex data on each line and add it to a buffer.
        def hexStr = dataEl.text()
        def arr = new byte[128*18]
        def srcPos = 0
        def dstPos = 0

        // Process each line
        (0..<128).each { y ->

            // Process all 18 bytes in one line
            (0..<18).each { x ->
                arr[dstPos+x] = Integer.parseInt(hexStr[srcPos..srcPos+1], 16)
                srcPos += 2
            }
            dstPos += 18

            // Skip unused source data
            srcPos += (40 - 18)*2
        }

        // Put the results into the buffer
        assert dstPos == 128*18
        def outBuf = ByteBuffer.allocate(dstPos)
        outBuf.put(arr)

        // All done. Return the buffer.
        return outBuf
    }

    /**
     * Flood fill from the upper left and upper right corners to determine
     * all transparent areas.
     */
    def calcTransparency(img)
    {
        def height = img.size
        def width  = img[0].size

        // Keep track of which pixels we have traversed
        def marks = img.collect { new boolean[it.size] }

        // Initial positions to check
        def queue = [] as Queue
        queue.add([0, 0, img[0][0]])
        queue.add([width-1, 0, img[0][width-1]])

        // While we have anything left to check...
        while (!queue.isEmpty())
        {
            // Get a coordinate and color to compare to
            def (x, y, c) = queue.poll()

            // If outside the image, skip it
            if (x < 0 || x >= width || y < 0 || y >= height)
                continue

            // Process each pixel only once
            if (marks[y][x])
                continue
            marks[y][x] = true

            // Stop at color changes
            if (img[y][x] != c)
                continue

            // Found a pixel to change to transparent. Mark it, and check in every
            // direction for more of the same color.
            //
            img[y][x] = TRANSPARENT_COLOR
            queue.add([x-1, y,   c])
            queue.add([x,   y-1, c])
            queue.add([x,   y+1, c])
            queue.add([x+1, y,   c])
        }
    }

    class MultiPix
    {
        def colorValues = [:]

        def add(color, value) {
            colorValues[color] = colorValues.get(color,0) + value
        }

        def getHighColor() {
            def kv = colorValues.grep().sort { a, b -> b.value <=> a.value }[0]
            return [kv.key, kv.value]
        }

        def addError(pix, skipColor, frac) {
            pix.colorValues.each { color, value ->
                if (color != skipColor)
                    add(color, value * frac)
            }
        }
    }

    /**
     * Produce a new mip-map level by halving the image's resolution.
     */
    def reduceTexture(imgIn)
    {
        def inWidth = imgIn[0].size()
        def inHeight = imgIn.size()

        def outWidth = inWidth >> 1
        def outHeight = inHeight >> 1

        def pixBuf = new MultiPix[outHeight][outWidth]

        // Distribute the input pixels to the output pixels
        imgIn.eachWithIndex { row, sy ->
            def dy = sy >> 1
            row.eachWithIndex { color, sx ->
                def dx = sx >> 1
                if (!pixBuf[dy][dx])
                    pixBuf[dy][dx] = new MultiPix()
                pixBuf[dy][dx].add(color, 1)
            }
        }

        // Apply error diffusion to form the final result
        def outRows = []
        pixBuf.eachWithIndex { row, dy ->
            def outRow = []
            row.eachWithIndex { pix, dx ->
                def (color, value) = pix.getHighColor()
                outRow.add(color)
                // Distribute the error: 50% to the right, 25% down, 25% down-right
                /* Note: This makes things really look weird, so not doing it any more.
                if (dx+1 < outWidth)
                    pixBuf[dy][dx+1].addError(pix, color, 0.5)
                if (dy+1 < outHeight)
                    pixBuf[dy+1][dx].addError(pix, color, 0.25)
                if (dx+1 < outWidth && dy+1 < outHeight)
                    pixBuf[dy+1][dx+1].addError(pix, color, 0.25)
                */
            }
            outRows.add(outRow)
        }
        return outRows
    }

    def printTexture(rows)
    {
        rows.each { row ->
            print "    "
            row.each { pix -> print pix }
            println ""
        }
    }

    def writeString(buf, str)
    {
        str.each { buf.put((byte)it) }
        buf.put((byte)0);
    }

    def calcMapExtent(rows)
    {
        // Determine the last row that has a majority of tiles filled
        def height = 1
        rows.eachWithIndex { row, y ->
            def filled = 0
            row.each { tile ->
                if (tile) {
                    filled += 1
                }
            }
            if (filled > row.size() / 2)
                height = y+1
        }

        // Determine the last column that has a majority of tiles filled
        def width = 1
        (0..<rows[0].size()).each { x ->
            def filled = 0
            (0..<rows.size()).each { y ->
                if (rows[y][x])
                    filled += 1
            }
            if (filled > rows[0].size() / 2)
                width = x+1
        }

        def nHorzSections = (int) ((width + TILES_PER_ROW - 1) / TILES_PER_ROW)
        def nVertSections = (int) ((height + ROWS_PER_SECTION - 1) / ROWS_PER_SECTION)

        return [width, height, nHorzSections, nVertSections]
    }

    def write2DMap(mapName, mapEl, rows)
    {
        def (width, height, nHorzSections, nVertSections) = calcMapExtent(rows)

        def buffers = new ByteBuffer[nVertSections][nHorzSections]
        def sectionNums = new int[nVertSections][nHorzSections]

        // Allocate a buffer and assign a map number to each section.
        allMaps << [name:mapName, order:parseOrder(mapEl.@order)]
        (0..<nVertSections).each { vsect ->
            (0..<nHorzSections).each { hsect ->
                def buf = ByteBuffer.allocate(512)
                buffers[vsect][hsect] = buf
                def num = maps2D.size() + 1
                sectionNums[vsect][hsect] = num
                def sectName = "$mapName-$hsect-$vsect"
                addResourceDep("map", mapName, "map2D", sectName)
                maps2D[sectName] = [num:num, order:mapEl.@order, buf:buf]
            }
        }

        // Now create each map section
        (0..<nVertSections).each { vsect ->
            (0..<nHorzSections).each { hsect ->

                def hOff = hsect * TILES_PER_ROW
                def vOff = vsect * ROWS_PER_SECTION

                def (tileSetNum, tileMap) = packTileSet(rows, hOff, TILES_PER_ROW, vOff, ROWS_PER_SECTION)

                def xRange = hOff ..< hOff+TILES_PER_ROW
                def yRange = vOff ..< vOff+ROWS_PER_SECTION
                def sectName = "$mapName-$hsect-$vsect"
                def (scriptModule, locationsWithTriggers) =
                    packScripts(mapEl, sectName, width, height, xRange, yRange)

                // Header: first come links to other map sections.
                // The first section is always 0xFF for north and west. So instead, use that
                // space to record the total number of horizontal and vertical sections.
                def buf = buffers[vsect][hsect]
                if (vsect == 0 && hsect == 0)
                    buf.put((byte) nHorzSections);
                else
                    buf.put((byte) ((vsect > 0) ? sectionNums[vsect-1][hsect] : 0xFF))           // north
                buf.put((byte) ((hsect < nHorzSections-1) ? sectionNums[vsect][hsect+1] : 0xFF)) // east
                buf.put((byte) ((vsect < nVertSections-1) ? sectionNums[vsect+1][hsect] : 0xFF)) // south
                if (vsect == 0 && hsect == 0)
                    buf.put((byte) nVertSections);
                else
                    buf.put((byte) ((hsect > 0) ? sectionNums[vsect][hsect-1] : 0xFF))           // west

                // Then links to the tile set and script library
                buf.put((byte) tileSetNum)
                buf.put((byte) scriptModule)

                // After the header comes the raw data
                (0..<ROWS_PER_SECTION).each { rowNum ->
                    def y = vOff + rowNum
                    def row = (y < height) ? rows[y] : null
                    (0..<TILES_PER_ROW).each { colNum ->
                        def x = hOff + colNum
                        def tile = (row && x < width) ? row[x] : null
                        def flags = 0
                        if ([colNum, rowNum] in locationsWithTriggers)
                            flags |= 0x40
                        if (tile?.@obstruction == 'true')
                            flags |= 0x80
                        buf.put((byte)((tile ? tileMap[tile.@id] : 0) | flags))
                    }
                }
            }
        }

        // Compress them all
        (0..<nVertSections).each { vsect ->
            (0..<nHorzSections).each { hsect ->
                def sectName = "$mapName-$hsect-$vsect"
                maps2D[sectName].buf = compress(unwrapByteBuffer(maps2D[sectName].buf))
            }
        }
    }

    def write3DMap(buf, mapName, rows, scriptModule, locationsWithTriggers) // [ref BigBlue1_50]
    {
        def width = rows[0].size() + 2  // Sentinel $FF at start and end of each row
        def height = rows.size() + 2    // Sentinel rows of $FF's at start and end

        // Determine the set of all referenced textures, and assign numbers to them.
        def texMap = [:]
        def texList = []
        def texFlags = []
        def texNames = [] as Set
        rows.each { row ->
            row.each { tile ->
                def id = tile?.@id
                def name = tile?.@name
                if (name != null)
                    name = stripName(name)
                if (!texMap.containsKey(id)) {
                    if (name == null || name =~ /street|blank|null/)
                        texMap[id] = 0
                    else if (name in textures) {
                        def flags = 1
                        if (tile?.@obstruction == 'true')
                            flags |= 2
                        if (tile?.@blocker == 'true')
                            flags |= 4
                        texList.add(textures[name].num)
                        texFlags.add(flags)
                        texMap[id] = texList.size()
                        if (tile?.@sprite == 'true')
                            texMap[id] |= 0x80; // hi-bit flag to mark sprite cells
                        texNames << name
                    }
                    else if (id) {
                        printWarning("can't match tile name '$name' to any image; treating as blank.")
                        texMap[id] = 0
                    }
                }
            }
        }

        // Header: width and height
        buf.put((byte)width)
        buf.put((byte)height)

        // Followed by script module num
        buf.put((byte)scriptModule)

        // Followed by the list of textures
        texList.each { buf.put((byte)it) }
        buf.put((byte)0)

        // Followed by the corresponding list of texture flags
        texFlags.each { buf.put((byte)it) }
        buf.put((byte)0)

        // Sentinel row of $FF at start of map
        (0..<width).each { buf.put((byte)0xFF) }

        // After the header comes the raw data
        rows.eachWithIndex { row,y ->
            buf.put((byte)0xFF) // sentinel at start of row
            row.eachWithIndex { tile,x ->
                // Mark scripted locations with a flag
                def flags = ([x,y] in locationsWithTriggers) ? 0x20 : 0
                buf.put((byte)(texMap[tile?.@id] | flags))
            }
            buf.put((byte)0xFF) // sentinel at end of row
        }

        // Sentinel row of $FF at end of map
        (0..<width).each { buf.put((byte)0xFF) }

        // Document memory usage so user can make intelligent choices about what/when to cut
        def gameloopSize = bytecodes['gameloop'].buf.uncompressedLen
        def mapScriptsSize = bytecodes[makeScriptName(mapName)].buf.uncompressedLen
        def mapTexturesSize = texList.size() * 0x555
        def totalAux = gameloopSize + mapScriptsSize + mapTexturesSize
        def safeLimit = 38 * 1024  // observed by doing a DEBUG_MEM at very start of gameloop.pla
        memUsage3D << String.format("%-20s: %4.1fK of %4.1fK used: %4.1fK scripts, %4.1fK in %2d textures, %4.1fK overhead%s",
            mapName, totalAux/1024.0, safeLimit/1024.0,
            mapScriptsSize/1024.0, mapTexturesSize/1024.0, texList.size(), gameloopSize/1024.0,
            totalAux > safeLimit ? " [WARNING]" : "")
        if (totalAux > safeLimit)
            printWarning "memory will be dangerously full; see pack_report.txt for details."

        // Record texture dependencies
        texNames.each { addMapDep("texture", it) }
    }

    // The renderer wants bits of the two pixels interleaved in a special way.
    // Given input pix1=0000QRST and pix2=0000wxyz, the output will be QwRxSyTz.
    // So the renderer uses mask 10101010 to extract pix1, then shifts left one
    // bit and uses the same mask to get pix2.
    //
    def combine(pix1, pix2) {
        return ((pix2 & 1) << 0) | ((pix1 & 1) << 1) |
               ((pix2 & 2) << 1) | ((pix1 & 2) << 2) |
               ((pix2 & 4) << 2) | ((pix1 & 4) << 3) |
               ((pix2 & 8) << 3) | ((pix1 & 8) << 4);
    }

    def writeTexture(buf, image)
    {
        // Write pixel data for all 5 mip levels plus the orig image
        for (def mipLevel in 0..5)
        {
            // Process double rows
            for (x in 0..<image[0].size()) {
                for (y in (0..<image.size).step(2))
                    buf.put((byte) combine(image[y][x], image[y+1][x]))
            }
            // Generate next mip-map level
            if (mipLevel < 5)
                image = reduceTexture(image)
        }
    }

    def stripName(name)
    {
        return name.toLowerCase().replaceAll(" ", "")
    }

    def grabFromCache(kind, addTo, name, hash)
    {
        def key = kind + ":" + name
        if (cache.containsKey(key) && cache[key].hash == hash) {
            def num = addTo.size() + 1
            addTo[name] = [num:num, buf:cache[key].data]
            return true
        }
        return false
    }

    def addToCache(kind, addTo, name, hash, buf)
    {
        def num = addTo.size() + 1
        addTo[name] = [num:num, buf:buf]

        def key = kind + ":" + name
        cache[key] = [hash:hash, data:buf]
    }

    def updateEngineStamp(name, hash)
    {
        if (!cache.containsKey("engineStamp") || cache["engineStamp"].hash < hash)
            cache["engineStamp"] = [hash:hash]
    }

    def grabEntireFromCache(kind, addTo, hash)
    {
        if (cache.containsKey(kind) && cache[kind].hash == hash) {
            cache[kind].ents.each { ent ->
                def num = addTo.size() + 1
                addTo[ent.name] = [num: ent.num, buf: ent.data]
            }
            return true
        }
        return false
    }

    def addEntireToCache(kind, addTo, hash)
    {
        def ents = []
        addTo.each { name, ent ->
            ents << [name:name, num:ent.num, data:ent.buf]
        }
        cache[kind] = [hash:hash, ents:ents]
    }

    def packTexture(imgEl)
    {
        def (name, animFrameNum, animFlags) = decodeImageName(imgEl.@name ?: "img$num")
        name = stripName(name)
        withContext("texture '$name'") {
            if (!textures.containsKey(name)) {
                def num = textures.size() + 1
                textures[name] = [num:num, anim:new AnimBuf()]
            }
            def pixels = parseTexture(imgEl)
            calcTransparency(pixels)
            def buf = ByteBuffer.allocate(50000)
            writeTexture(buf, pixels)
            textures[name].anim.addImage(animFrameNum, animFlags, buf)
        }
    }

    def packFrameImage(imgEl)
    {
        def (name, animFrameNum, animFlags) = decodeImageName(imgEl.@name ?: "img$num")
        withContext("full screen image '$name'") {
            if (!frames.containsKey(name)) {
                def num = frames.size() + 1
                frames[name] = [num:num, anim:new AnimBuf()]
            }
            frames[name].anim.addImage(animFrameNum, animFlags, parseFrameData(imgEl))
        }
    }

    def packPortrait(imgEl)
    {
        def (name, animFrameNum, animFlags) = decodeImageName(imgEl.@name ?: "img$num")
        withContext("portrait '$name'") {
            if (!portraits.containsKey(name)) {
                def num = portraits.size() + 1
                portraits[name] = [num:num, anim:new AnimBuf()]
            }
            portraits[name].anim.addImage(animFrameNum, animFlags, parse126Data(imgEl))
        }
    }

    def packTile(imgEl)
    {
        def buf = parseTileData(imgEl)
        tiles[imgEl.@id] = buf
    }

    /** Identify the avatars and other global tiles, and number them */
    def numberGlobalTiles(dataIn)
    {
        def tileNum = 0
        dataIn.tile.sort{(it.@category + it.@name).toLowerCase()}.each { tile ->
            def lname = tile.@name.toLowerCase().trim().replaceAll(/\s*-\s*[23][dD]\s*/, "")
            def cat = tile.@category.toLowerCase().trim()
            if (cat == "avatar")
                avatars[lname] = tileNum++
            else if (cat == "compass") {
                for (def dir : ['north', 'east', 'south', 'west']) {
                    if (lname.contains(dir)) {
                        compassTiles[dir] = tileNum++
                        break
                    }
                }
            }
            else if (cat == "clock") {
                for (def time : ['12:00', '1:30', '3:00', '4:30', '6:00', '7:30', '9:00', '10:30']) {
                    if (lname.contains(time)) {
                        clockTiles[time] = tileNum++
                        break
                    }
                }
            }
            else if (cat == "lamp") {
                lampTiles << tileNum++
            }
        }
        assert avatars.size() >= 1 : "Need at least one tile in 'Avatar' category."
    }

    /** Pack the global tiles, like the player avatar, into their own tile set. */
    def packGlobalTileSet(dataIn)
    {
        def setNum = tileSets.size() + 1
        assert setNum == 1 : "Special tile set must be first."
        def setName = "tileSet_special"
        def tileIds = [] as Set
        def tileMap = [:]
        def buf = ByteBuffer.allocate(50000)

        // Add each special tile to the set
        dataIn.tile.sort{(it.@category + it.@name).toLowerCase()}.each { tile ->
            def name = tile.@name
            def id = tile.@id
            def data = tiles[id]
            def cat = tile.@category.toLowerCase().trim()
            if (cat == "avatar" || cat == "compass" || cat == "clock" || cat == "lamp") {
                def num = tileMap.size()
                tileIds.add(id)
                tileMap[id] = num
                data.flip() // crazy stuff to append one buffer to another
                buf.put(data)
            }
        }

        tileSets[setName] = [num:setNum, buf:buf, tileMap:tileMap, tileIds:tileIds]
        return [setNum, tileMap]
    }

    /** Pack tile images referenced by map rows into a tile set */
    def packTileSet(rows, xOff, width, yOff, height)
    {
        // First, determine the set of unique tile IDs for this map section
        def tileIds = [] as Set
        (yOff ..< yOff+height).each { y ->
            def row = (y < rows.size) ? rows[y] : null
            (xOff ..< xOff+height).each { x ->
                def tile = (row && x < row.size) ? row[x] : null
                tileIds.add(tile?.@id)
            }
        }

        assert tileIds.size() > 0

        // See if there's a good existing tile set we can use/add to.
        def tileSet = null
        def bestCommon = 0
        tileSets.values().each {
            // Can't combine with the special tileset
            if (it.num > 1)
            {
                // See if the set we're considering has room for all our tiles
                def inCommon = it.tileIds.intersect(tileIds)
                def together = it.tileIds + tileIds
                if (together.size() < 64 && inCommon.size() > bestCommon) {
                    tileSet = it
                    bestCommon = inCommon.size()
                }
            }
        }

        // If adding to an existing set, update it.
        def setNum
        if (tileSet) {
            setNum = tileSet.num
            //print "Adding to tileSet $setNum; had ${tileSet.tileIds.size()} tiles"
            tileSet.tileIds.addAll(tileIds)
            //println ", now ${tileSet.tileIds.size()}."
        }
        // If we can't add to an existing set, make a new one
        else {
            setNum = tileSets.size() + 1
            //println "Creating new tileSet $setNum."
            tileSet = [num:setNum, buf:ByteBuffer.allocate(50000), tileMap:[:], tileIds:tileIds]
            tileSets["tileSet${setNum}"] = tileSet
        }

        addMapDep("tileSet", "tileSet${setNum}")
        addMapDep("tileSet", "tileSet_special")  // each map requires the global tileset as well

        // Start by assuming we'll create a new tileset
        def tileMap = tileSet.tileMap
        def buf = tileSet.buf

        // Then add each non-null tile to the set
        (yOff ..< yOff+height).each { y ->
            def row = (y < rows.size) ? rows[y] : null
            (xOff ..< xOff+height).each { x ->
                def tile = (row && x < row.size) ? row[x] : null
                def id = tile?.@id
                if (tile && !tileMap.containsKey(id)) {
                    def num = tileMap.size()+1
                    assert num < 64 : "Error: Only 63 kinds of tiles are allowed on any given map."
                    tileMap[id] = num
                    tiles[id].flip() // crazy stuff to append one buffer to another
                    buf.put(tiles[id])
                }
            }
        }
        assert tileMap.size() > 0
        assert buf.position() > 0

        return [setNum, tileMap]
    }

    def pack2DMap(mapEl, tileEls)
    {
        def name = mapEl.@name ?: "map$num"
        def num = mapNames[name][1]
        //println "Packing 2D map #$num named '$name': num=$num."
        withContext("map '$name'") {
            def rows = parseMap(mapEl, tileEls)
            write2DMap(name, mapEl, rows)
        }
    }

    def pack3DMap(mapEl, tileEls)
    {
        def name = mapEl.@name ?: "map$num"
        def num = mapNames[name][1]
        //println "Packing 3D map #$num named '$name': num=$num."
        withContext("map '$name'") {
            addResourceDep("map", name, "map3D", name)
            def rows = parseMap(mapEl, tileEls)
            def (scriptModule, locationsWithTriggers) = packScripts(mapEl, name, rows[0].size(), rows.size())
            def buf = ByteBuffer.allocate(50000)
            write3DMap(buf, name, rows, scriptModule, locationsWithTriggers)
            maps3D[name] = [num:num, order:mapEl.@order, buf:compress(unwrapByteBuffer(buf))]
            allMaps << [name:name, order:parseOrder(mapEl.@order)]
        }
    }

    def makeScriptName(mapName)
    {
        // Strip "- 2D" etc from the map name
        return humanNameToSymbol(mapName.replaceAll(/\s*-\s*[23][dD]\s*/, ""), false)
    }

    def packScripts(mapEl, mapName, totalWidth, totalHeight, xRange = null, yRange = null)
    {
        def num = modules.size() + 1
        def name = makeScriptName(mapName)
        //println "Packing scripts for map $mapName, to module $num."
        addMapDep("module", name)

        def scriptDir = "build/src/mapScripts/"
        if (!new File(scriptDir).exists())
            new File(scriptDir).mkdirs()
        ScriptModule module = new ScriptModule()
        module.packMapScripts(mapName, new File(new File(scriptDir), name+".pla.new"),
            mapEl.scripts ? mapEl.scripts[0] : [],
            totalWidth, totalHeight, xRange, yRange)
        replaceIfDiff(scriptDir + name + ".pla")
        compileModule(name, "src/mapScripts/", false) // false=not verbose
        return [num, module.locationsWithTriggers]
    }

    def readBinary(path)
    {
        def f = new File(path)
        def len = f.length()
        if (binaryStubsOnly)
            return new byte[1]
        def outBuf = new byte[len]
        new File(path).withInputStream { stream ->
            def got = stream.read(outBuf)
            assert got == len
        }
        return outBuf
    }

    def readModule(name, path)
    {
        def num = modules.size() + 1
        //println "Reading module #$num from '$path'."
        def buf = readBinary(path)
        if (binaryStubsOnly) {
            modules[name] = [num:num, buf:bufObj]
            return
        }

        // Look for the magic header 0xDA7F =~ "DAVE"+1
        assert (buf[3] & 0xFF) == 0xDA
        assert (buf[2] & 0xFF) == 0x7F

        // Determine offsets
        def asmCodeStart = 12
        while (buf[asmCodeStart++] != 0)
            ;
        def byteCodeStart = ((buf[6] & 0xFF) | ((buf[7] & 0xFF) << 8)) - 0x1000
        def initStart = ((buf[10] & 0xFF) | ((buf[11] & 0xFF) << 8)) - 2 - 0x1000
        def fixupStart = ((buf[0] & 0xFF) | ((buf[1] & 0xFF) << 8)) + 2

        // Other header stuff
        def defCount = ((buf[8] & 0xFF) | ((buf[9] & 0xFF) << 8))

        //println String.format("asmCodeStart =%04x", asmCodeStart)
        //println String.format("byteCodeStart=%04x", byteCodeStart)
        //println String.format("initStart    =%04x", initStart)
        //println String.format("fixupStart   =%04x", fixupStart)
        //println               "defCount     =$defCount"

        // Sanity checking on the offsets
        assert asmCodeStart >= 0 && asmCodeStart <= byteCodeStart
        assert byteCodeStart >= asmCodeStart && byteCodeStart <= fixupStart
        assert initStart == 0 || (initStart+2 >= byteCodeStart && initStart+2 <= fixupStart)
        assert fixupStart <= buf.length

        // Split up the parts now that we know their offsets
        def asmCode = buf[asmCodeStart..<byteCodeStart]
        def byteCode = buf[byteCodeStart..<fixupStart]
        def fixup = buf[fixupStart..<buf.length]

        // Extract offsets of the bytecode functions from the fixup table
        def sp = 0
        def defs = [initStart+2 - byteCodeStart]
        def invDefs = [:]
        (1..<defCount).each {
            assert fixup[sp++] == 2 // code table fixup
            def addr = fixup[sp++] & 0xFF
            addr |= (fixup[sp++] & 0xFF) << 8
            invDefs[addr] = it*5
            addr -= 0x1000
            addr -= byteCodeStart
            assert addr >= 0 && addr < byteCode.size
            defs.add(addr)
            assert fixup[sp++] == 0  // not sure what the zero byte is
        }

        // Construct asm stubs for all the bytecode functions that'll be in aux mem
        def dp = 0
        def stubsSize = defCount * 5
        def newAsmCode = new byte[stubsSize + asmCode.size + 2]
        (0..<defCount).each {
            newAsmCode[dp++] = 0x20 // JSR
            newAsmCode[dp++] = 0xDC // Aux mem interp ($3DC)
            newAsmCode[dp++] = 0x03
            newAsmCode[dp++] = defs[it] & 0xFF
            newAsmCode[dp++] = (defs[it] >> 8) & 0xFF
        }

        // Stick the asm code onto the end of the stubs
        (0..<asmCode.size).each {
            newAsmCode[dp++] = asmCode[it]
        }

        // Locate the external/entry symbol area, and parse it.
        def imports = [:]
        def exports = [:]
        def ep = sp
        while (fixup[ep] != 0)
            ep += 4
        ep++
        int esdIndex = 0
        while (fixup[ep] != 0) {
            def nameBuf = new StringBuilder()
            while (true) {
                def c = fixup[ep++] & 0xFF
                if (c >= 128)
                    nameBuf.append((char)(c & 0x7F))
                else {
                    nameBuf.append((char)c)
                    break
                }
            }
            def esdName = nameBuf.toString().toLowerCase()
            def esdFlag = fixup[ep++] & 0xFF
            def esdNum = fixup[ep++] & 0xFF
            esdNum += (fixup[ep++] & 0xFF) << 8
            if (esdFlag == 0x10) {
                assert esdNum == esdIndex
                //println "Import '$esdName' at esdIndex $esdNum."
                imports[esdNum] = esdName
            }
            else if (esdFlag == 0x08) {
                assert name == "gameloop" : "Can only export from gameloop"
                //println "Export '$esdName' at offset $esdNum."
                def target
                if (invDefs.containsKey(esdNum)) {
                    //println "  ...in invDefs."
                    target = invDefs[esdNum]
                }
                else {
                    //println "  ...in asm or data."
                    target = esdNum - 0x1000
                    target -= asmCodeStart
                    target += stubsSize   // account for the stubs we prepended to the asm code
                }
                //println "...final target: $target"
                exports[esdName] = target
            }
            else
                assert false : "Unknown Entry/Symbol flag: $flag"
            esdIndex++
        }

        // If any exports, we assume they're from the game lib. That's the only
        // place we support exports.
        if (exports.size() > 0) {
            assert name == "gameloop" : "Symbol exports only supported on 'gameloop' module"
            cache["globalExports"] = exports
        }

        // Translate offsets in all the fixups
        def newFixup = []
        dp = 0
        while (fixup[sp] != 0) {
            int fixupType = fixup[sp++] & 0xFF
            assert fixupType == 0x81 || fixupType == 0x91 // We can only handle WORD sized INTERN or EXTERN fixups
            int addr = fixup[sp++] & 0xFF
            addr |= (fixup[sp++] & 0xFF) << 8
            esdIndex = fixup[sp++] & 0xFF

            // Fixups can be in the asm section or in the bytecode section. Figure out which this is.
            addr += 2  // apparently offsets don't include the header length
            def inByteCode = (addr >= byteCodeStart)
            //println String.format("Fixup addr=0x%04x, inByteCode=%b", addr, inByteCode)

            // Figure out which buffer to modify, and the offset within it
            def codeBuf = inByteCode ? byteCode : newAsmCode
            addr -= inByteCode ? byteCodeStart : asmCodeStart
            if (!inByteCode)
                addr += stubsSize   // account for the stubs we prepended to the asm code
            //println String.format("...adjusted addr=0x%04x", addr)

            int target = (codeBuf[addr] & 0xFF) | ((codeBuf[addr+1] & 0xFF) << 8)
            //println String.format("...target=0x%04x", target)

            if (fixupType == 0x91) {  // external fixup
                //println "external fixup: esdIndex=$esdIndex target=$target"
                def esdName = imports[esdIndex]
                //println "esdName='$esdName'"
                assert esdName != null : "failed to look up esdIndex $esdIndex"
                def offset = cache["globalExports"][esdName]
                //println "offset=$offset"
                assert offset != null : "failed to find global export for symbol '$esdName'"
                target += offset
            }
            else if (invDefs.containsKey(target)) {
                target = invDefs[target]
                //println String.format("...translated to def offset 0x%04x", target)
                assert target >= 5 && target < newAsmCode.length
                assert esdIndex == 0
            }
            else {
                target -= 0x1000
                target -= asmCodeStart
                target += stubsSize   // account for the stubs we prepended to the asm code
                //println String.format("...adjusted to target offset 0x%04x", target)
                assert target >= 5 && target < newAsmCode.length
                assert esdIndex == 0
            }

            // Put the adjusted target back in the code
            codeBuf[addr] = (byte)(target & 0xFF)
            codeBuf[addr+1] = (byte)((target >> 8) & 0xFF)

            // And record the fixup
            assert addr >= 0 && addr <= 0x3FFF : "code module too big"
            newFixup.add((byte)((addr>>8) & 0x3F) |
                                (inByteCode ? 0x40 : 0) |
                                ((fixupType == 0x91) ? 0x80 : 0))
            newFixup.add((byte)(addr & 0xFF))
        }
        newFixup.add((byte)0xFF)

        return [compress(newAsmCode), compress(wrapByteList(byteCode)), compress(wrapByteList(newFixup))]
    }

    def wrapByteList(list) {
        byte[] arr = new byte[list.size]
        list.eachWithIndex { n, idx -> arr[idx] = (byte)n }
        return arr
    }

    def unwrapByteBuffer(buf) {
        int len = buf.position()
        byte[] out = new byte[len]
        buf.position(0)
        buf.get(out)
        return out
    }

    def readFont(name, path)
    {
        def hash = new File(path).lastModified()
        if (!grabEntireFromCache("fonts", fonts, hash)) {
            def num = fonts.size() + 1
            //println "Reading font #$num from '$path'."
            fonts[name] = [num:num, buf:compress(readBinary(path))]
            addEntireToCache("fonts", fonts, hash)
        }
        fonts.each { k,v ->
            addResourceDep("map", "<root>", "font", k)
        }
    }

    static int uncompTotal = 0
    static int lz4Total = 0
    static int lx47Total = 0
    static int lx47Savings = 0

    def compressionSavings = 0

    def compress(uncompressedData)
    {
        def uncompressedLen = uncompressedData.length

        // Now compress it with LX47
        assert uncompressedLen < 327678 : "data block too big"
        assert uncompressedLen > 0
        def compressedData = compressor.compress(uncompressedData)
        def compressedLen = compressedData.length
        assert compressedLen > 0

        // As a check, verify that decompression works with only a 3-byte underlap
        if (debugCompression && (uncompressedLen - compressedLen) > 0) {
            def underlap = 3
            def checkData = new byte[uncompressedLen+underlap]
            def initialOffset = uncompressedLen - compressedLen + underlap
            System.arraycopy(compressedData, 0, checkData, initialOffset, compressedLen)
            compressor.decompress(checkData, initialOffset, checkData, 0, uncompressedLen)
            def outBlk = Arrays.copyOfRange(checkData, 0, uncompressedLen)
            assert Arrays.equals(uncompressedData, outBlk)
        }

        // If we saved at least 10 bytes, take the compressed version.
        // If we saved at least 10 bytes, take the compressed version.
        if ((uncompressedLen - compressedLen) >= 10) {
            if (debugCompression)
                println String.format("  Compress. rawLen=\$%x compLen=\$%x", uncompressedLen, compressedLen)
            compressionSavings += (uncompressedLen - compressedLen) - 2 // -2 for storing compressed len
            return [data:compressedData, len:compressedLen,
                    compressed:true, uncompressedLen:uncompressedLen]
        }
        else {
            if (debugCompression)
                println String.format("  No compress. rawLen=\$%x compLen=\$%x", uncompressedLen, compressedLen)
            return [data:uncompressedData, len:uncompressedLen, compressed:false]
        }
    }

    /** Determine the amount of space that'll be consumed on disk by the given chunk */
    def calcChunkLen(chunk) {
        return chunk.buf.len + (chunk.buf.compressed ? 6 : 4)
    }

    def findResourceChunk(inKey)
    {
        def typeName = inKey[0]
        def resourceName = inKey[1]

        def key = typeName + ":" + resourceName
        if (!allChunks.containsKey(key))
        {
            // It's ok for an abstract map to not have a chunk... as long as
            // there are deps for it, we know it's real. Likewise, encounter
            // zones and global funcs only have subs.
            if (typeName =~ /^(map|encounterZone|globalFunc)$/ && resourceDeps.containsKey(inKey))
                return null

            println "all keys: ${allChunks.keySet()}"
            assert false : "resource link fail: key=$key"
        }
        def data = allChunks[key]

        def typeNum = typeName=="code" ? TYPE_CODE :
                      typeName=="map2D" ? TYPE_2D_MAP :
                      typeName=="map3D" ? TYPE_3D_MAP :
                      typeName=="tileSet" ? TYPE_TILE_SET :
                      typeName=="texture" ? TYPE_TEXTURE_IMG :
                      typeName=="frame" ? TYPE_SCREEN :
                      typeName=="font" ? TYPE_FONT :
                      typeName=="module" ? TYPE_MODULE :
                      typeName=="bytecode" ? TYPE_BYTECODE :
                      typeName=="fixup" ? TYPE_FIXUP :
                      typeName=="portrait" ? TYPE_PORTRAIT :
                      null
        assert typeNum : "Can't map typeName $typeName"
        return [type:typeNum, num:data.num, name:resourceName, buf:data.buf]
    }

    /**
     * Recursively add all dependent resources for the given one, except any that
     * are in part1Chunks.
     *
     * Returns the amount of space added.
     */
    int rtraceLevel = 0
    static final boolean DEBUG_TRACE_RESOURCES = false
    def traceResources(key, LinkedHashMap chunks)
    {
        if (DEBUG_TRACE_RESOURCES)
            println "${("  " * rtraceLevel)}traceResources: key=$key"

        // If already in the set, don't add twice.
        if (chunks.containsKey(key)) {
            if (DEBUG_TRACE_RESOURCES)
                println "${("  " * rtraceLevel)} already have."
            return 0
        }

        // If already on disk 1, don't duplicate.
        if (part1Chunks.containsKey(key)) {
            if (DEBUG_TRACE_RESOURCES)
                println "${("  " * rtraceLevel)} in part 1 already."
            return 0
        }

        // Okay, we need to add it.
        def spaceAdded = 0
        def chunk = findResourceChunk(key)
        if (chunk) {
            chunks[key] = chunk
            spaceAdded += calcChunkLen(chunk)
            if (DEBUG_TRACE_RESOURCES)
                println "${("  " * rtraceLevel)} adding, id=${chunks[key].num} len=${chunks[key].buf.len}"
        }

        // And add all its dependencies.
        if (resourceDeps.containsKey(key)) {
            ++rtraceLevel
            resourceDeps[key].each { spaceAdded += traceResources(it, chunks) }
            --rtraceLevel
        }

        return spaceAdded
    }

    /**
     * Find all portraits used by the given resource
     */
    def tracePortraits(key, portraits)
    {
        if (key[0] == "portrait")
            portraits << key[1]
        if (resourceDeps.containsKey(key))
            resourceDeps[key].each { tracePortraits(it, portraits) }
    }

    /**
     * Determine the most-often-used portraits and stuff as many as we can
     * onto disk 1.
     */
    def stuffMostUsedPortraits(maps, mapChunks, availBlks, spaceUsed)
    {
        def counts = [:]
        maps.each { mapName ->
            def mapPortraits = [] as Set
            tracePortraits(["map", mapName], mapPortraits)
            mapPortraits.each { portraitName ->
                if (!counts.containsKey(portraitName))
                    counts[portraitName] = 1
                else
                    counts[portraitName]++
            }
        }
        def pairs = counts.collect { k,v -> [name:k, count:v] }
        def portraitSpace = 0
        pairs.sort{-it.count}.each { pair ->
            if (pair.count > 1) {
                def key = ["portrait", pair.name]
                if (!mapChunks.containsKey(key)) {
                    def chunk = findResourceChunk(key)
                    def len = calcChunkLen(chunk)
                    def blks = calcFileBlks(spaceUsed + portraitSpace + len)
                    if (blks <= availBlks) {
                        mapChunks[key] = chunk
                        chunk.buf.partNum = 1
                        portraitSpace += len
                    }
                }
            }
        }
        return portraitSpace
    }

    int calcFileBlks(int spaceUsed)
    {
        int blks = (spaceUsed + 511) / 512
        if (blks > 256)
            blks += 3   // tree file
        else if (blks > 1)
            blks += 1   // sapling file
        return blks
    }

    /** Iterate an array of maps, adding them one at a time until we get to one
     *  that won't fit. The maps list is modified to remove all that were accepted.
     *  Returns [chunks, spaceRemaining]
     */
    def fillDisk(int partNum, int availBlks, ArrayList<String> maps, Set<String> toDupe)
    {
        //println "Filling disk $partNum, availBlks=$availBlks"
        def CHUNK_HEADER_SIZE = 3
        def spaceUsed = CHUNK_HEADER_SIZE

        // On disk 1, reserve enough space for the map and portrait index
        int overhead = 0
        if (partNum == 1) {
            overhead = 6                     // chunk header (6 if compressed)
                     + 1 + maps2D.size()     // number of maps, then 1 byte per
                     + 1 + maps3D.size()     // number of maps, then 1 byte per
                     + 1 + portraits.size()  // number of portraits, then 1 byte per
        }

        if (nWarnings == 0)
        {
            reportWriter.println "\nDisk $partNum:"
            reportWriter.println String.format("  %-22s: %6.1fK",
                                    partNum == 1 ? "LegendOS+overhead" : "overhead",
                                    ((FLOPPY_SIZE-availBlks)*512 + overhead) / 1024.0)
        }

        def outChunks = (partNum==1) ? part1Chunks : ([:] as LinkedHashMap)
        def addedMapNames = []
        def mapsSpaceUsed = 0
        def readd = []
        while (!maps.isEmpty()) {
            def mapName = maps[0]
            def mapChunks = outChunks.clone()
            //println "Trying map $mapName"
            def mapSpace = traceResources(["map", mapName], mapChunks)

            int blks = calcFileBlks(spaceUsed + overhead + mapSpace)
            if (blks > availBlks) {
                //println "stopping: map $mapName would add $mapSpace bytes, totaling $blks blks, too big."
                break
            }

            spaceUsed += mapSpace
            //println "ok: map $mapName adds $mapSpace bytes, totaling $blks blks."
            mapChunks.each { k,v ->
                v.buf.partNum = partNum
                outChunks[k] = v
            }
            // Handle maps that get dupe'd on each data disk
            if (toDupe.contains(mapName))
                readd << mapName
            maps.remove(0) // pop finished map

            // After adding the root map, stuff in the most-used portraits onto disk 1
            if (mapName == "<root>") {
                assert partNum == 1
                if (nWarnings == 0)
                    reportWriter.println String.format("  %-22s: %6.1fK", "base resources", mapSpace / 1024.0)
                def portraitsSpace = stuffMostUsedPortraits(maps, outChunks, availBlks, spaceUsed)
                spaceUsed += portraitsSpace
                blks = calcFileBlks(spaceUsed)
                if (nWarnings == 0)
                    reportWriter.println String.format("  %-22s: %6.1fK", "shared portraits", portraitsSpace / 1024.0)
                //println "stuffed most-used portraits for $portraitsSpace bytes, totaling $blks blks."
            }
            else {
                addedMapNames << mapName
                mapsSpaceUsed += mapSpace
            }
        }
        if (!maps.isEmpty())
            maps.addAll(0, readd) // handle maps that need dupe on each data disk
        if (nWarnings == 0) {
            if (mapsSpaceUsed > 0) {
                reportWriter.println String.format("  %-22s: %6.1fK", "maps & resources", mapsSpaceUsed / 1024.0)
                addedMapNames.each { reportWriter.println "    $it" }
            }
            reportWriter.println String.format("  %-22s: %6.1fK", "unused", (availBlks*512 - spaceUsed) / 1024.0)
            reportWriter.println "Total: 140K"
        }
        return [outChunks, spaceUsed]
    }

    def recordChunks(typeName, nameToData) {
        nameToData.each { resourceName, data ->
            def key = "$typeName:$resourceName"
            allChunks[key] = data
        }
    }

    int parseOrder(orderStr) {
        try {
            return Integer.parseInt(orderStr);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Make a compact representation of a timestamp, useful as a version number
     */
    def timestampToVersionNum(engineStamp, scenarioStamp)
    {
        Calendar cal = Calendar.getInstance()
        cal.setTimeInMillis(engineStamp)
        def year = cal.get(Calendar.YEAR)
        def month = cal.get(Calendar.MONTH)
        def day = cal.get(Calendar.DAY_OF_MONTH)
        def hour = cal.get(Calendar.HOUR_OF_DAY)

        def yearCode = year % 10
        def monthCode = (month < 9) ? (char) (48+month+1) :
                        month == 9 ? 'o' :
                        month == 10 ? 'n' :
                        'd'
        def hourCode = (char) (97 + hour) // 'a'=0, 'b'=1, etc.
        def engineCode = String.format("%d%c%02d%c", yearCode, monthCode, day, hourCode)

        def offset = (int) ((scenarioStamp - engineStamp) / (1000 * 60 * 60))
        return String.format("%s%s%d", engineCode, offset < 0 ? "-" : ".", Math.abs(offset))
    }

    /**
     * Make an index listing the partition number wherein each map and portrait can be found.
     */
    def addResourceIndex(part)
    {
        def tmp = ByteBuffer.allocate(5000)

        // Start with the version number
        def combinedVersion = timestampToVersionNum(cache["engineStamp"].hash, cache["scenarioStamp"].hash)
        tmp.put((byte)(combinedVersion.length()))
        combinedVersion.getBytes().each { b -> tmp.put((byte)b) }

        // Then output 2D maps, 3d maps, and portraits
        tmp.put((byte) maps2D.size())
        maps2D.each { k, v ->
            tmp.put((byte) ((parseOrder(v.order) < 0) ? 255 : v.buf.partNum))
        }

        tmp.put((byte) maps3D.size())
        maps3D.each { k, v ->
            tmp.put((byte) ((parseOrder(v.order) < 0) ? 255 : v.buf.partNum))
        }

        tmp.put((byte) portraits.size())
        portraits.each { k, v -> tmp.put((byte) (v.buf.partNum ? v.buf.partNum : 0)) }

        code["resourceIndex"].buf = compress(unwrapByteBuffer(tmp))
        def chunk = [type:TYPE_CODE, num:code["resourceIndex"].num, 
                     name:"resourceIndex", buf:code["resourceIndex"].buf]
        part.chunks[["code", "resourceIndex"]] = chunk
        part.spaceUsed += calcChunkLen(chunk)

        return combinedVersion
    }

    def fillAllDisks()
    {
        // Place all resource buffers in into one easy-to-find place
        recordChunks("code",     code)
        recordChunks("map2D",    maps2D)
        recordChunks("map3D",    maps3D)
        recordChunks("tileSet",  tileSets)
        recordChunks("texture",  textures)
        recordChunks("frame",    frames)
        recordChunks("portrait", portraits)
        recordChunks("font",     fonts)
        recordChunks("module",   modules)
        recordChunks("bytecode", bytecodes)
        recordChunks("fixup",    fixups)

        // Sort the maps in proper disk order
        allMaps << [name:"<root>", order:-999999]
        Collections.sort(allMaps) { a,b ->
            a.order < b.order ? -1 :
            a.order > b.order ?  1 :
            a.name < b.name ? -1 :
            a.name > b.name ?  1 :
            0
        }

        if (nWarnings == 0)
            reportWriter.println "======================== Floppy disk usage ==========================="

        // Now fill up disk partitions until we run out of maps.
        def mapsToDupe = allMaps.grep{ it.name != "<root>" && it.order < 0 }.collect{ it.name }.toSet()
        def mapsTodo = allMaps.collect { it.name }
        def partChunks = []
        for (int partNum=1; partNum<=MAX_DISKS && !mapsTodo.isEmpty(); partNum++) {
            int availBlks = FLOPPY_SIZE - DOS_OVERHEAD
            availBlks -= AC_KLUDGE  // AppleCommander currently unable to allocate last block
            if (partNum == 1) {
                // Disk 1 adds LEGENDOS.SYSTEM. Figure out its size:
                // round up to nearest whole block, plus index blk
                def coreBlks = calcFileBlks(coreSize)
                //println "LEGENDOS blks=$coreBlks"
                availBlks -= coreBlks
                // Disk 1 also holds the save game file
                availBlks -= SAVE_GAME_SIZE
            }

            def (chunks, spaceUsed) = fillDisk(partNum, availBlks, mapsTodo, mapsToDupe)
            partChunks << [partNum:partNum, chunks:chunks, spaceUsed:spaceUsed]
        }
        assert allMaps.isEmpty : "All data must fit within $MAX_DISKS disks."

        // Add the special resource index to disk 1
        def gameVersion = addResourceIndex(partChunks[0])

        // And write out each disk
        partChunks.each { part ->
            //println "Writing disk ${part.partNum}."
            def partFile = new File("build/root/game.part.${part.partNum}.bin")
            partFile.withOutputStream { stream ->
                writePartition(stream, part.partNum, part.chunks.values())
            }
            def spaceUsed = part.spaceUsed  // use var to avoid gigantic assert fail msg
            assert spaceUsed == partFile.length()
        }

        println "Game version: V$gameVersion"
    }

    def writePartition(stream, partNum, chunks)
    {
        // Generate the header chunk. Leave the first 2 bytes for the # of pages in the hdr
        def hdrBuf = ByteBuffer.allocate(50000)
        hdrBuf.put((byte)0)
        hdrBuf.put((byte)0)

        // Write the four bytes for each resource (6 for compressed resources)
        chunks.each { chunk ->
            hdrBuf.put((byte)chunk.type)
            assert chunk.num >= 1 && chunk.num <= 255
            hdrBuf.put((byte)chunk.num)
            def len = chunk.buf.len
            //println "  chunk: type=${chunk.type}, num=${chunk.num}, len=$len"
            hdrBuf.put((byte)(len & 0xFF))
            hdrBuf.put((byte)((len >> 8) | (chunk.buf.compressed ? 0x80 : 0)))
            if (chunk.buf.compressed) {
                def uclen = chunk.buf.uncompressedLen;
                hdrBuf.put((byte)(uclen & 0xFF))
                hdrBuf.put((byte)(uclen >> 8))
            }

            // Record sizes for reporting purposes
            chunkSizes[[type:chunk.type, name:chunk.name, num:chunk.num]] =
                [clen: len,
                 uclen: chunk.buf.compressed ? chunk.buf.uncompressedLen : len]
        }

        // Terminate the header with a zero type
        hdrBuf.put((byte)0);

        // Fix up the first bytes to contain the length of the header (including the length
        // itself)
        //
        def hdrEnd = hdrBuf.position()
        hdrBuf.position(0)
        hdrBuf.put((byte)(hdrEnd & 0xFF))
        hdrBuf.put((byte)(hdrEnd >> 8))
        hdrBuf.position(hdrEnd)

        // Finally, write out each chunk's data, including the header.
        stream.write(unwrapByteBuffer(hdrBuf))
        chunks.each {
            stream.write(it.buf.data, 0, it.buf.len)
        }
    }

    def runNestedvm(programClass, programName, args, inDir, inFile, outFile)
    {
        def prevStdin = System.in
        def prevStdout = System.out
        def prevStderr = System.err
        def prevUserDir = System.getProperty("user.dir")
        def result
        def errBuf = new ByteArrayOutputStream()
        try
        {
            System.setProperty("user.dir", new File(inDir).getAbsolutePath())
            if (inFile) {
                inFile.withInputStream { inStream ->
                    System.in = inStream
                    outFile.withOutputStream { outStream ->
                        System.out = new PrintStream(outStream)
                        System.err = new PrintStream(errBuf)
                        result = programClass.newInstance().run(args)
                    }
                }
            }
            else {
                result = programClass.newInstance().run(args)
            }
        } finally {
            System.in = prevStdin
            System.out = prevStdout
            System.err = prevStderr
            System.setProperty("user.dir", prevUserDir)
        }
        def errStr = errBuf.toString("UTF-8")
        if (result != 0) {
            if (errStr.length() > 0)
                errStr = "\nError output:\n" + errStr + "-----\n"
            throw new Exception(
                "$programName (cd $inDir && ${args.join(' ')}) < $inFile > $outFile failed with code $result." + errStr)
        }
        else if (errStr.length() > 0)
            println("$programName (cd $inDir && ${args.join(' ')}) < $inFile > $outFile generated warning(s):" + errStr)
    }

    /**
     * Copy a file, either from the local area (during development) or failing that,
     * from the jar file resources (for end-users).
     */
    def jitCopy(dstFile)
    {
        dstFile = dstFile.getCanonicalFile()
        if (!dstFile.path.startsWith(buildDir.path))
            return dstFile

        def partial = dstFile.path.substring(buildDir.path.size()+1)

        // See if it's in the local directory
        def srcFile = new File(partial).getCanonicalFile()
        if (!srcFile.exists())
            srcFile = new File(srcFile.getName()) // try current directory
        if (srcFile.exists()) {
            if (dstFile.exists()) {
                if (srcFile.lastModified() == dstFile.lastModified())
                    return dstFile
                dstFile.delete()
            }
            else
                dstFile.getParentFile().mkdirs()
            if (!(srcFile.equals(dstFile))) {
                Files.copy(srcFile.toPath(), dstFile.toPath())
                dstFile.setLastModified(srcFile.lastModified())
            }
            return dstFile
        }

        // See if it's in the resources of the jar file
        def res = getClass().getResource("/virtual/" + partial.replace("\\", "/"))
        if (!res)
            res = getClass().getResource("/" + partial.replace("\\", "/"))
        if (res) {
            def m = res.toString() =~ /^(jar:file:|bundle:\/\/[^\/]*)(.*)!.*$/
            assert m
            srcFile = new File(java.net.URLDecoder.decode(m.group(2), "UTF-8"))
            if (dstFile.exists()) {
                if (srcFile.lastModified() == dstFile.lastModified())
                    return dstFile
                dstFile.delete()
            }
            else
                dstFile.getParentFile().mkdirs()
            Files.copy(res.openStream(), dstFile.toPath())
            dstFile.setLastModified(srcFile.lastModified())
        }
        return dstFile
    }

    def getCodeDeps(codeFile)
    {
        def baseDir = codeFile.getParentFile()

        // If we've cached deps for this file, just return that.
        def key = "codeDeps:" + codeFile.toString()
        def hash = codeFile.lastModified()
        def deps = []
        if (cache.containsKey(key) && cache[key].hash == hash)
            deps = cache[key].deps
        else {
            codeFile.eachLine { line ->
                def m = line =~ /^\s*include\s+"([^"]+)"/
                if (m) {
                    deps << jitCopy(new File(baseDir, m.group(1)))
                    if (m.group(1) =~ /.*gamelib.plh/ && !(codeFile =~ /.*gameloop.pla/))
                        deps << jitCopy(new File(baseDir, m.group(1).replace("gamelib.plh", "gameloop.pla")))
                }
                m = line =~ /^\s*!(source|convtab) "([^"]+)"/
                if (m) {
                    if (codeFile ==~ /.*\.pla$/) {
                        // Asm includes inside a plasma file have an extra level of ".."
                        // because they end up getting processed within the "build" dir.
                        deps << jitCopy(new File(new File(baseDir, "build"), m.group(2)).getCanonicalFile())
                    }
                    else
                        deps << jitCopy(new File(baseDir, m.group(2)).getCanonicalFile())
                }
            }
            cache[key] = [hash:hash, deps:deps]
        }

        // Recursively calc deps of the deps
        deps.each { dep -> getCodeDeps(dep) }

        // And return everything we found.
        return deps
    }

    def getLastDep(codeFile)
    {
        codeFile = jitCopy(codeFile)
        def time = codeFile.lastModified()
        getCodeDeps(codeFile).each { dep ->
            time = Math.max(time, getLastDep(dep))
        }
        return time
    }

    def assembleCode(codeName, inDir)
    {
        if (binaryStubsOnly)
            return addToCache("code", code, codeName, 1, new byte[1])

        inDir = "build/" + inDir
        def hash = getLastDep(new File(inDir, codeName + ".s"))
        if (grabFromCache("code", code, codeName, hash))
            return

        println "Assembling ${codeName}.s"
        new File(inDir + "build").mkdir()
        String[] args = ["acme", "-f", "plain",
                         "-o", "build/" + codeName + ".b",
                         codeName + ".s"]
        runNestedvm(acme.Acme.class,  "ACME assembler", args, inDir, null, null)
        def uncompData = readBinary(inDir + "build/" + codeName + ".b")

        addToCache("code", code, codeName, hash, compress(uncompData))
        updateEngineStamp(codeName, hash)
    }

    def assembleCore(inDir)
    {
        if (binaryStubsOnly)
            return addToCache("sysCode", sysCode, "core", 1, new byte[1])

        // Read in all the parts of the LegendOS core system and combine them together
        // with block headers.
        inDir = "build/" + inDir
        new File(inDir + "build").mkdirs()
        def outBuf = ByteBuffer.allocate(50000)
        ["loader", "decomp", "PRORWTS", "PLVM02", "mem"].each { name ->
            def hash
            if (name ==~ /PRORWTS|PLVM02/) {
                // Pre-assembled (externally); just read the result.
                def file = jitCopy(
                    new File("build/tools/${name=="PRORWTS" ? "ProRWTS/PRORWTS2" : "PLASMA/src/PLVM02"}#4000"))
                hash = file.lastModified()
                if (!grabFromCache("sysCode", sysCode, name, hash)) {
                    addToCache("sysCode", sysCode, name, hash, compress(readBinary(file.toString())))
                    updateEngineStamp(name, hash)
                }
            }
            else {
                hash = getLastDep(new File(inDir, "${name}.s"))
                if (!grabFromCache("sysCode", sysCode, name, hash)) {
                    println "Assembling ${name}.s"
                    String[] args = ["acme", "-o", "build/$name", "${name}.s"]
                    runNestedvm(acme.Acme.class,  "ACME assembler", args, inDir, null, null)
                    def uncompData = readBinary(inDir + "build/$name")
                    // Don't compress the loader and the decompressor; compress mem mgr.
                    addToCache("sysCode", sysCode, name, hash,
                        (name ==~ /loader|decomp/) ? [data:uncompData, len:uncompData.length, compressed:false]
                                                   : compress(uncompData))
                    updateEngineStamp(name, hash)
                }
            }

            def code = sysCode[name].buf
            if (name != "loader") {
                assert code.compressed || name == "decomp"
                // Uncompressed size first
                def uclen = code.compressed ? code.uncompressedLen : code.len
                outBuf.put((byte) (uclen & 0xFF))
                outBuf.put((byte) (uclen >> 8))
                // Then compressed size
                def clen = code.len
                outBuf.put((byte) (clen & 0xFF))
                outBuf.put((byte) (clen >> 8))
            }
            outBuf.put(code.data)
        }

        // Write out the result
        coreSize = outBuf.position()
        new File("build/src/core/build/LEGENDOS.SYSTEM.sys#2000").withOutputStream { stream ->
            stream.write(unwrapByteBuffer(outBuf))
        }
    }

    def compileModule(moduleName, codeDir, verbose = true)
    {
        addResourceDep("module", moduleName, "bytecode", moduleName)
        addResourceDep("module", moduleName, "fixup", moduleName)

        if (binaryStubsOnly)
            return addToCache("modules", modules, moduleName, 1, new byte[1])

        codeDir = "build/" + codeDir
        def hash = getLastDep(new File(codeDir + moduleName + ".pla"))
        if (grabFromCache("modules", modules, moduleName, hash) &&
            grabFromCache("bytecodes", bytecodes, moduleName, hash) &&
            grabFromCache("fixups", fixups, moduleName, hash))
        {
            return
        }

        if (verbose)
            println "Compiling ${moduleName}.pla"
        System.out.flush()
        String[] args = ["plasm", "-AMOW"]
        new File(codeDir + "build").mkdir()
        runNestedvm(plasma.Plasma.class,  "PLASMA compiler", args, codeDir,
            new File(codeDir + moduleName + ".pla"),
            new File(codeDir + "build/" + moduleName + ".a"))

        args = ["acme", "--setpc", "4096",
                "-o", moduleName + ".b",
                moduleName + ".a"]
        runNestedvm(acme.Acme.class,  "ACME assembler", args, codeDir + "build/", null, null)
        def module, bytecode, fixup
        (module, bytecode, fixup) = readModule(moduleName, codeDir + "build/" + moduleName + ".b")
        addToCache("modules", modules, moduleName, hash, module)
        addToCache("bytecodes", bytecodes, moduleName, hash, bytecode)
        addToCache("fixups", fixups, moduleName, hash, fixup)
        if (!(moduleName ==~ /.*(gs|gen)_.*/ || codeDir ==~ /.*mapScript.*/))
            updateEngineStamp(moduleName, hash)
    }

    def readAllCode()
    {
        // Loader, ProRWTS, PLASMA VM, and memory manager
        assembleCore("src/core/")

        // All other assembly-language code
        assembleCode("render", "src/raycast/")
        assembleCode("expand", "src/raycast/")
        assembleCode("fontEngine", "src/font/")
        assembleCode("tileEngine", "src/tile/")

        code.each { k,v -> addResourceDep("map", "<root>", "code", k) }

        // Special module added after the fact
        code["resourceIndex"] = [num:code.size()+1, buf:null] // filled in later

        compileModule("gameloop", "src/plasma/")
        compileModule("combat", "src/plasma/")
        compileModule("party", "src/plasma/")
        compileModule("store", "src/plasma/")
        compileModule("diskops", "src/plasma/")
        compileModule("godmode", "src/plasma/")
        compileModule("intimate", "src/plasma/")
        compileModule("gen_enemies", "src/plasma/")
        compileModule("gen_items", "src/plasma/")
        compileModule("gen_players", "src/plasma/")
        compileModule("gen_flags", "src/plasma/")
        globalScripts.each { name, nArgs ->
            compileModule("gs_"+name, "src/plasma/")
        }
        lastSysModule = modules.size()

        modules.each { k,v -> addResourceDep("map", "<root>", "module", k) }
    }

    /**
     * Number all the maps and record them with names
     */
    def numberMaps(dataIn)
    {
        def num2D = 0
        def num3D = 0
        dataIn.map.each { map ->
            def name = map?.@name
            def shortName = name.replaceAll(/[\s-]*[23]D$/, '')
            if (map?.@name =~ /\s*2D$/) {
                mapNames[name] = ['2D', num2D+1]
                mapNames[shortName] = ['2D', num2D+1]
                def rows = parseMap(map, dataIn.tile, true) // quick mode
                def (width, height, nHorzSections, nVertSections) = calcMapExtent(rows)
                num2D += (nHorzSections * nVertSections)
            }
            else if (map?.@name =~ /\s*3D$/) {
                mapNames[name] = ['3D', num3D+1]
                mapNames[shortName] = ['3D', num3D+1]
                ++num3D
            }
            else
                printWarning "map name '${map?.@name}' should contain '2D' or '3D'. Skipping."
        }
    }

    /*
    *  Scan all the scripts looking for flags, and make a mapping of flag name to number.
    */
    def numberGameFlags(data)
    {
        String name = data.name().toString()
        if (name == "{outlaw}gameData") {
            gameFlags = [] as Set  // temporary, until we have them all
            data.global.scripts.script.each { numberGameFlags(it) }
            data.map.scripts.script.each { numberGameFlags(it) }

            // Now that we have them all, sort and assign numbers
            def flagSet = gameFlags
            gameFlags = [:]
            flagSet.sort().each { flg -> gameFlags[flg] = gameFlags.size() }
        }
        else if (name == "{outlaw}block" &&
                 (data.@type == "interaction_get_flag" || data.@type == "interaction_set_flag"))
        {
            def els = data.field
            assert els.size() == 1
            def first = els[0]
            assert first.@name == "NAME"
            def flg = first.text().toLowerCase()
            gameFlags << flg
        }
        else {
            data.iterator().each {
                if (it instanceof Node)
                    numberGameFlags(it)
            }
        }
    }

    def readCache()
    {
        File cacheFile = new File("build/world.cache")
        if (cacheFile.exists()) {
            ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(cacheFile));
            def inCache = inStream.readObject();
            if (inCache["version"] == CACHE_VERSION)
                cache = inCache
            inStream.close()
        }
    }

    def writeCache()
    {
        File cacheFile = new File("build/world.cache")
        File newCacheFile = new File("build/world.cache.new")
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(newCacheFile));
        out.writeObject(cache);
        out.close()
        cacheFile.delete() // needed on Windows
        newCacheFile.renameTo(cacheFile)
    }

    def reportSizes()
    {
        reportWriter.println(
            "\n================================= Memory usage of 3D maps =====================================\n")
        memUsage3D.each { reportWriter.println it }
        reportWriter.println ""

        reportWriter.println(
            "================================= Resource Sizes =====================================")
        def data = [:]
        chunkSizes.each { k,v ->
            assert typeNumToName.containsKey(k.type)
            def type = typeNumToName[k.type]
            if (type == "Code" && k.num > lastSysModule)
                type = "Script"
            def name = k.name.replaceAll(/\s*-\s*[23][dD].*/, "")
            def dataKey = [type:type, name:name]
            if (!data.containsKey(dataKey))
                data[dataKey] = v
            else {
                data[dataKey].clen += v.clen
                data[dataKey].uclen += v.uclen
            }
        }

        def prevType
        def cSub = 0, cTot = 0, ucSub = 0, ucTot = 0
        data.keySet().sort { a,b ->
            a.type < b.type ? -1 :
            a.type > b.type ? 1 :
            a.name < b.name ? -1 :
            a.name > b.name ? 1 :
            0
        }.each { k ->
            def v = data[k]
            if (prevType != k.type) {
                if (prevType) {
                    reportWriter.println String.format("%-22s: %6.1fK memory, %6.1fK disk", 
                        "Subtotal", ucSub/1024.0, cSub/1024.0)
                }
                reportWriter.println("\n${k.type} resources:")
                prevType = k.type
                cSub = 0
                ucSub = 0
            }
            reportWriter.println String.format("  %-20s: %6.1fK memory, %6.1fK disk", 
                k.name, v.uclen/1024.0, v.clen/1024.0)
            cSub += v.clen
            cTot += v.clen
            ucSub += v.uclen
            ucTot += v.uclen
        }
        reportWriter.println String.format("%-22s: %6.1fK memory, %6.1fK disk", 
            "Subtotal", ucSub/1024.0, cSub/1024.0)
        reportWriter.println String.format("\n%-22s: %6.1fK memory, %6.1fK disk", 
            "GRAND TOTAL", ucTot/1024.0, cTot/1024.0)
    }

    def countArgs(script) {
        return script.block.mutation.arg.size()
    }

    def recordGlobalScripts(dataIn) {
        dataIn.global.scripts.script.each {
            globalScripts[humanNameToSymbol(it.@name, false)] = countArgs(it)
        }
        requiredGlobalScripts.each { humanName ->
            def name = humanNameToSymbol(humanName, false)
            if (!globalScripts.containsKey(name))
                globalScripts[name] = 0
        }
    }

    def addResourceDep(fromType, fromName, toType, toName)
    {
        assert fromType != null && fromName != null
        assert toType != null && toName != null
        def fromKey = [fromType, fromName]
        if (!resourceDeps.containsKey(fromKey))
            resourceDeps[fromKey] = [] as Set
        resourceDeps[fromKey] << [toType, toName]
    }

    def addMapDep(toType, toName)
    {
        assert curMapName != null
        addResourceDep("map", curMapName, toType, toName)
    }

    def pack(xmlFile, dataIn)
    {
        // Save time by using cache of previous run
        readCache()

        // Record scenario timestamp
        cache["scenarioStamp"] = [hash: xmlFile.lastModified()]

        // Record global script names
        recordGlobalScripts(dataIn)

        // Read in code chunks. For now these are hard coded, but I guess they ought to
        // be configured in a config file somewhere...?
        //
        readAllCode()

        // We have only one font, for now at least.
        jitCopy(new File("build/data/fonts/font.bin"))
        readFont("font", "build/data/fonts/font.bin")

        // Pre-pack the data for each tile
        dataIn.tile.each {
            packTile(it)
        }

        // Pack the global tile set before other tile sets (contains the player avatar, etc.)
        numberGlobalTiles(dataIn)
        packGlobalTileSet(dataIn)

        // Divvy up the images by category
        def titleImgs      = []
        def uiFrameImgs    = []
        def fullscreenImgs = []
        def textureImgs    = []
        def portraitImgs   = []

        dataIn.image.sort{it.@name.toLowerCase()}.each { image ->
            def category = image.@category?.toLowerCase()
            def (name, animFrameNum, animFlags) = decodeImageName(image.@name)
            if (category == "fullscreen" && name.equalsIgnoreCase("title"))
                titleImgs << image
            else if (category == "uiframe")
                uiFrameImgs << image
            else if (category == "fullscreen")
                fullscreenImgs << image
            else if (category == "wall" || category == "sprite")
                textureImgs << image
            else if (category == "portrait")
                portraitImgs << image
            else if (category == "background")
                null; // pass for now
            else
                println "Warning: couldn't classify image named '${name}', category '${category}'."
        }

        assert titleImgs.size() >= 1 : "Couldn't find title image. Should be category='FULLSCREEN', name='title'"
        assert uiFrameImgs.size() == 2 : "Need exactly 2 UI frames, found ${uiFramesImgs.size()} instead."

        // Pack each image, which has the side-effect of filling in the image name map.
        def hash = calcImagesHash(titleImgs + uiFrameImgs + fullscreenImgs)
        if (!grabEntireFromCache("frames", frames, hash)) {
            println "Packing frame images."
            titleImgs.each { image -> packFrameImage(image) }
            uiFrameImgs.each { image -> packFrameImage(image) }
            fullscreenImgs.each { image -> packFrameImage(image) }
            frames.each { name, frame ->
                frame.buf = compress(frame.anim.pack())
                frame.remove('anim')
            }
            addEntireToCache("frames", frames, hash)
        }
        frames.each { k,v ->
            addResourceDep("map", "<root>", "frame", k)
        }

        hash = calcImagesHash(textureImgs)
        if (!grabEntireFromCache("textures", textures, hash)) {
            println "Packing textures."
            textureImgs.each { image -> packTexture(image) }
            textures.each { name, texture ->
                texture.buf = compress(texture.anim.pack())
                texture.anim = null
            }
            addEntireToCache("textures", textures, hash)
        }

        hash = calcImagesHash(portraitImgs)
        if (!grabEntireFromCache("portraits", portraits, hash)) {
            println "Packing portraits."
            portraitImgs.each { image -> packPortrait(image) }
            portraits.each { name, portrait ->
                portrait.buf = compress(portrait.anim.pack())
                portrait.anim = null
            }
            addEntireToCache("portraits", portraits, hash)
        }

        // Ensure we can find the system-required portraits, and make them root dependencies
        def combatWinFound = false
        def deathFound = false
        portraits.each { name, portrait ->
            if (name.toLowerCase() == "combatwin") {
                addResourceDep("map", "<root>", "portrait", name)
                combatWinFound = true
            }
            if (name.toLowerCase() == "death") {
                addResourceDep("map", "<root>", "portrait", name)
                deathFound = true
            }
        }
        if (!combatWinFound)
            throw new Exception("Error: combatWin portrait not found")
        if (!deathFound)
            throw new Exception("Error: death portrait not found")

        // Number all the maps and record them with names
        numberMaps(dataIn)

        // Assign a number to each game flag
        numberGameFlags(dataIn)

        // Form the translation from item name to function name (and ditto
        // for players)
        allItemFuncs(dataIn.global.sheets.sheet)
        allPlayerFuncs(dataIn.global.sheets.sheet)

        // Pack each map. This uses the image and tile maps filled earlier.
        println "Packing maps and scripts."
        dataIn.map.each { map ->
            curMapName = map.@name
            if (map?.@name =~ /2D/)
                pack2DMap(map, dataIn.tile)
            else if (map?.@name =~ /3D/)
                pack3DMap(map, dataIn.tile)
            else
                printWarning "map name '${map?.@name}' should contain '2D' or '3D'. Skipping."
            curMapName = null
        }

        // Now that the tileSets are complete, compress them.
        tileSets.each { name, tileSet ->
            tileSet.buf = compress(unwrapByteBuffer(tileSet.buf))
        }

        //println("rdeps - phase 2:" + JsonOutput.prettyPrint(JsonOutput.toJson(resourceDeps)))

        // Ready to write the output file.
        println "Writing output files."
        new File("build/root").mkdir()

        fillAllDisks()

        // Print stats (unless there's a warning, in which case focus the user on that)
        if (nWarnings == 0)
            reportSizes()

        if (debugCompression)
            println "Compression savings: $compressionSavings"

        // And save the cache for next time.
        writeCache()
    }

    def isAlnum(ch)
    {
        if (Character.isAlphabetic(ch.charAt(0) as int))
            return true
        if (Character.isDigit(ch.charAt(0) as int))
            return true
        return false
    }

    def humanNameToSymbol(str, allUpper)
    {
        def buf = new StringBuilder()
        def inParen = false
        def inSlash = false
        def needSeparator = false
        str.eachWithIndex { ch, idx ->
            if (ch == '(') {
                inParen = true
                ch = 0
            }
            else if (ch == ')') {
                inParen = false
                ch = 0
            }
            else if (ch == '/') {
                inSlash = true
                ch = 0
            }
            else if (!isAlnum(ch))
                inSlash = false

            if (ch && !inParen && !inSlash) {
                if ((ch >= 'A' && ch <= 'Z') || ch == ' ' || ch == '_')
                    needSeparator = (idx > 0)
                if (isAlnum(ch)) {
                    if (allUpper) {
                        if (needSeparator)
                            buf.append('_')
                        buf.append(ch.toUpperCase())
                    }
                    else {
                        // Camel case
                        buf.append(needSeparator ? ch.toUpperCase() : ch.toLowerCase())
                    }
                    needSeparator = false
                }
            }
        }
        def result = buf.toString()
        if (result.length() > 16)
        {
            // PLASMA's compiler has a silent limit on the number of significant
            // characters in a symbol. To make the symbol short enough but still
            // significant, calculate a digest and replace the excess characters
            // with part of it.
            def bigHash = Integer.toString(result.hashCode(), 36)
            result = result.substring(0, 10) + "_" + bigHash.substring(bigHash.length() - 4)
        }
        return result
    }

    def parseDice(str)
    {
        // Handle single value
        if (str =~ /^\d+$/) {
            def n = str.toInteger()
            if (n < 0 || n > 255) {
                printWarning("Number $n forced to valid range 0..255")
                return "0"
            }
            return str
        }

        // Otherwise parse things like "2d6+1"
        def m = (str =~ /^(\d+)[dD](\d+)([-+]\d+)?$/)
        if (!m)
            throw new Exception("Cannot parse dice string '$str'")
        def nDice = m[0][1].toInteger()
        if (nDice < 1 || nDice > 15) {
            printWarning("Number of dice $nDice forced to valid range 1..15")
            nDice = Math.min(15, Math.max(1, nDice))
        }
        def dieSize = m[0][2].toInteger()
        if (dieSize < 1 || dieSize > 15) {
            printWarning("Die size $dieSize forced to valid range 1..15")
            dieSize = Math.min(15, Math.max(1, dieSize))
        }
        def add = m[0][3] ? m[0][3].toInteger() : 0
        if (add < 0 || add > 255) {
            printWarning("Add-to-dice $add forced to valid range 0..255")
            add = Math.min(255, Math.max(0, add))
        }
        return String.format("\$%X", ((nDice << 12) | (dieSize << 8) | add))
    }

    def validateLootCode(code)
    {
        if (!code || code == "0")
            return "NULL"
        if (!allLootCodes.contains(code.toLowerCase())) {
            printWarning("Unknown loot-code '$code'")
            return "NULL"
        }
        return escapeString(code)
    }

    def genEnemy(out, row)
    {
        def name = row.@name
        withContext(name)
        {
            out.println("def _NEn_${humanNameToSymbol(name, false)}()")

            def image1 = row.@image1
            if (!portraits.containsKey(image1))
                throw new Exception("Image '$image1' not found")

            def image2 = row.@image2
            if (image2.size() > 0 && !portraits.containsKey(image2))
                throw new Exception("Image '$image2' not found")

            def hitPoints = row.@"hit-points"; assert hitPoints

            def attackType = row.@"attack-type"; assert attackType
            def attackTypeCode = attackType.toLowerCase() == "melee" ? 1 :
                                 attackType.toLowerCase() == "projectile" ? 2 :
                                 0
            if (!attackTypeCode) throw new Exception("Can't parse attack type '$attackType'")

            def attackText = row.@"attack-text";    assert attackText
            def range = row.@range;                 assert range
            def chanceToHit = row.@"chance-to-hit"; assert chanceToHit
            def damage = row.@damage;               assert damage
            def experience = row.@experience;       assert experience
            def mapCode = row.@"map-code";          assert mapCode
            def groupSize = row.@"group-size";      assert groupSize
            def lootChance = row.@"loot-chance";    // optional, defaults to 10%
            def lootCode = row.@"loot-code"         // optional
            def goldLoot = row.@"gold-loot";        assert goldLoot

            out.println("  return makeEnemy_pt2(makeEnemy_pt1(" +
                        "\"$name\", " +
                        "${parseDice(hitPoints)}, " +
                        "PO${humanNameToSymbol(image1, false)}, " +
                        (image2.size() > 0 ? "PO${humanNameToSymbol(image2, false)}, " : "0, ") +
                        "$attackTypeCode, " +
                        "\"$attackText\", " +
                        "${range.replace("'", "").toInteger()}, " +
                        "${chanceToHit.toInteger()}, " +
                        "${parseDice(damage)}, " +
                        "${parseDice(experience)}), " + // end of pt1
                        "${parseDice(groupSize)}, " +
                        "${lootChance ? lootChance.toInteger() : 10}, " +
                        "${validateLootCode(lootCode)}, " +
                        "${parseDice(goldLoot)})")
            out.println("end")

            // Add portrait dependencies based on encounter zone(s)
            def codesString = row.@"map-code"
            if (codesString != null && codesString.length() > 0)
            {
                codesString.replace("\"", "").split(",").collect{it.trim()}.grep{it!=""}.each { code ->
                    code = code.toLowerCase()
                    addResourceDep("encounterZone", code, "portrait", image1)
                    if (image2.size() > 0)
                        addResourceDep("encounterZone", code, "portrait", image2)
                }
            }
        }
    }

    def genAllEnemies(sheet)
    {
        assert sheet : "Missing 'enemies' sheet"

        withContext("enemies sheet")
        {
            new File("build/src/plasma/gen_enemies.plh.new").withWriter { out ->
                out.println("// Generated code - DO NOT MODIFY BY HAND\n")
                out.println("const enemies_forZone = 0")
            }
            replaceIfDiff("build/src/plasma/gen_enemies.plh")
            new File("build/src/plasma/gen_enemies.pla.new").withWriter { out ->
                out.println("// Generated code - DO NOT MODIFY BY HAND")
                out.println()
                out.println("include \"gamelib.plh\"")
                out.println("include \"globalDefs.plh\"")
                out.println("include \"playtype.plh\"")
                out.println("include \"gen_images.plh\"")
                out.println()

                def columns = sheet.columns.column.collect { it.@name }
                assert "name" in columns
                assert "map-code" in columns
                out.println("predef _enemies_forZone(zone)#1")
                out.println("word[] funcTbl = @_enemies_forZone\n")

                // Pre-define all the enemy creation functions
                sheet.rows.row.each { row ->
                    out.println("predef _NEn_${humanNameToSymbol(row.@name, false)}")
                }

                // Figure out the mapping between "map code" and "enemy", and output the table for that
                def codeToFunc = [:]
                sheet.rows.row.each { row ->
                    addCodeToFunc("_NEn_${humanNameToSymbol(row.@name, false)}", row.@"map-code", codeToFunc) 
                }
                outCodeToFuncTbl("mapCode_", codeToFunc, out)

                // Helper function to fill in the Enemy data structure
                out.println("""
def makeEnemy_pt1(name, hDice, img0, img1, attType, attText, attRange, chanceToHit, dmg, xp)
  word p; p = mmgr(HEAP_ALLOC, TYPE_ENEMY)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p=>w_health = rollDice(hDice) // e.g. 4d6
  if !img1 or (rand16() % 2)
    p->b_image = img0
  else
    p->b_image = img1
  fin
  p->b_attackType = attType
  p=>s_attackText = mmgr(HEAP_INTERN, attText)
  p->b_enemyAttackRange = attRange
  p->b_chanceToHit = chanceToHit
  p=>r_enemyDmg = dmg
  p=>r_enemyXP = xp
  return p
end
def makeEnemy_pt2(p, groupSize, lootChance, lootCode, goldLoot)
  p=>r_groupSize = groupSize
  p->b_lootChance = lootChance
  if lootCode; p=>s_lootCode = mmgr(HEAP_INTERN, lootCode); fin
  p=>r_goldLoot = goldLoot
  return p
end
""")

                // Now output a function for each enemy
                sheet.rows.row.each { row ->
                    genEnemy(out, row)
                }
                out.println()

                // And finally, a function to select an enemy given a map code.
                outCodeToFuncMethods("_enemies_forZone", "mapCode_", codeToFunc, out)

                out.println("return @funcTbl")
                out.println("done")
            }
            replaceIfDiff("build/src/plasma/gen_enemies.pla")
        }
    }

    def parseStringAttr(row, attrName)
    {
        def val = row."@$attrName"
        if (val == null)
            val = row."@${titleCase(attrName)}"
        if (val == null)
            val = row."@${attrName.toLowerCase()}"
        if (val == null)
            return ""
        return val.trim()
    }

    def parseByteAttr(row, attrName)
    {
        def val = parseStringAttr(row, attrName)
        if (!val) return 0
        val = val.replace("'", "")  // Change 5' to just 5, e.g. for weapon range
        assert val ==~ /^-?\d*$/ : "\"$attrName\" should be numeric"
        val = val.toInteger()
        assert val >= 0 && val <= 255 : "\"$attrName\" must be 0..255"
        return val
    }

    def parseWordAttr(row, attrName)
    {
        def val = parseStringAttr(row, attrName)
        if (!val) return 0
        assert val ==~ /^-?\d*$/ : "\"$attrName\" should be numeric"
        val = val.toInteger()
        assert val >= -32768 && val <= 32767 : "\"$attrName\" must be -32768..32767"
        return val
    }

    def parseBooleanAttr(row, attrName)
    {
        def val = parseStringAttr(row, attrName)
        return (val.toLowerCase() ==~ /yes|true|1/) ? "TRUE" : "FALSE"
    }

    def parseGenderAttr(row, attrName)
    {
        def val = parseStringAttr(row, attrName)
        if (!val) return 0
        return "'${val.charAt(0).toUpperCase()}'"
    }

    def parseModifier(row, attr1, attr2)
    {
        def bonusValue = parseWordAttr(row, attr1)
        def bonusName  = parseStringAttr(row, attr2)
        if (!bonusValue || !bonusName) return "NULL"
        return "makeModifier(${escapeString(bonusName)}, $bonusValue)"
    }

    def parseDiceAttr(row, attrName)
    {
        def val = parseStringAttr(row, attrName)
        if (!val) return 0
        return parseDice(val)
    }

    def genAllFlags()
    {
        // Make constants
        new File("build/src/plasma/gen_flags.plh.new").withWriter { out ->
            out.println("// Generated code - DO NOT MODIFY BY HAND\n")
            out.println("const flags_nameForNumber = 0")
            out.println("const flags_numberForName = 2")
            out.println()
            gameFlags.each { name, num ->
                out.println("const GF_${humanNameToSymbol(name, true)} = $num")
            }
            out.println("const NUM_GAME_FLAGS = ${gameFlags.size()}")
        }
        replaceIfDiff("build/src/plasma/gen_flags.plh")

        // Generate code
        new File("build/src/plasma/gen_flags.pla.new").withWriter { out ->
            out.println("// Generated code - DO NOT MODIFY BY HAND")
            out.println()
            out.println("include \"gamelib.plh\"")
            out.println("include \"globalDefs.plh\"")
            out.println("include \"gen_flags.plh\"")
            out.println()
            out.println("predef _flags_nameForNumber(num)#1")
            out.println("predef _flags_numberForName(name)#1")
            out.println()
            out.println("word[] funcTbl = @_flags_nameForNumber, @_flags_numberForName")
            out.println()
            gameFlags.each { name, num ->
                out.println("byte[] SF_${humanNameToSymbol(name, true)} = ${escapeString(name.toUpperCase())}")
            }
            out.println()
            gameFlags.each { name, num ->
                if (num == 0)
                    out.print("word[] flagNames = ")
                else
                    out.print("word             = ")
                out.println("@SF_${humanNameToSymbol(name, true)}")
            }
            out.println()
            out.println("def _flags_nameForNumber(num)#1")
            out.println("  if num >= 0 and num < NUM_GAME_FLAGS; return flagNames[num]; fin")
            out.println("  return NULL")
            out.println("end")
            out.println()
            out.println("def _flags_numberForName(name)#1")
            out.println("  word num")
            out.println("  for num = 0 to NUM_GAME_FLAGS-1")
            out.println("    if streqi(flagNames[num], name); return num; fin")
            out.println("  next")
            out.println("  return -1")
            out.println("end")
            out.println()

            // Lastly, the outer module-level code
            out.println("return @funcTbl")
            out.println("done")
        }
        replaceIfDiff("build/src/plasma/gen_flags.pla")
    }

    def genWeapon(func, row, out)
    {
        out.println(
            "  return makeWeapon_pt2(makeWeapon_pt1(" +
            "${escapeString(parseStringAttr(row, "name"))}, " +
            "${escapeString(parseStringAttr(row, "weapon-kind"))}, " +
            "${parseWordAttr(row, "price")}, " +
            "${parseModifier(row, "bonus-value", "bonus-attribute")}, " +
            "${escapeString(parseStringAttr(row, "ammo-kind"))}, " +
            "${parseByteAttr(row, "clip-size")}, " +
            "${parseDiceAttr(row, "melee-damage")}, " +
            "${parseDiceAttr(row, "projectile-damage")}), " +
            "${parseByteAttr(row, "single-shot")}, " +
            "${parseByteAttr(row, "semi-auto-shots")}, " +
            "${parseByteAttr(row, "auto-shots")}, " +
            "${parseByteAttr(row, "range")}, " +
            "${escapeString(parseStringAttr(row, "combat-text"))}, " +
            "${parseBooleanAttr(row, 'single-use')})")
    }

    def genArmor(func, row, out)
    {
        out.println(
            "  return makeArmor(" +
            "${escapeString(parseStringAttr(row, "name"))}, " +
            "${escapeString(parseStringAttr(row, "armor-kind"))}, " +
            "${parseWordAttr(row, "price")}, " +
            "${parseByteAttr(row, "armor-value")}, " +
            "${parseModifier(row, "bonus-value", "bonus-attribute")})")
    }

    def genItem(func, row, out)
    {
        def name = parseStringAttr(row, "name")
        def price = parseWordAttr(row, "price")
        def modifier = parseModifier(row, "bonus-value", "bonus-attribute")
        def kind = parseStringAttr(row, "ammo-kind")
        def count = parseWordAttr(row, "count")
        def storeAmount = parseWordAttr(row, "store-amount")
        def lootAmount = parseDiceAttr(row, "loot-amount")

        if ("$kind, $modifier, $count, $storeAmount, $lootAmount" != ", NULL, 0, 0, 0")
            out.println("  return makeFancyItem(${escapeString(name)}, $price, " +
                "${escapeString(kind)}, $modifier, $count, $storeAmount, $lootAmount)")
        else
            out.println("  return makePlainItem(${escapeString(name)}, $price)")
    }

    def genPlayer(func, row, out)
    {
        out.println("  word p, itemScripts")
        out.println("  itemScripts = mmgr(QUEUE_LOAD, MOD_GEN_ITEMS<<8 | RES_TYPE_MODULE)")
        out.println("  mmgr(FINISH_LOAD, 1) // 1 = keep open")
        out.println(\
            "  p = makePlayer_pt2(makePlayer_pt1(" +
            "${escapeString(parseStringAttr(row, "name"))}, " +
            "${parseByteAttr(row, "intelligence")}, " +
            "${parseByteAttr(row, "strength")}, " +
            "${parseByteAttr(row, "agility")}, " +
            "${parseByteAttr(row, "stamina")}, " +
            "${parseByteAttr(row, "charisma")}, " +
            "${parseByteAttr(row, "spirit")}, " +
            "${parseByteAttr(row, "luck")}), " + // ")" ==> this is the split between pt1 and pt2
            "${parseWordAttr(row, "health")}, " +
            "${Math.max(1, parseByteAttr(row, "level"))}, " +
            "${parseByteAttr(row, "aiming")}, " +
            "${parseByteAttr(row, "hand-to-hand")}, " +
            "${parseByteAttr(row, "dodging")}, " +
            "${parseGenderAttr(row, "gender")})")
        row.attributes().sort().eachWithIndex { name, val, idx ->
            if (name =~ /^skill-(.*)/) {
                out.println("  addToList(@p=>p_skills, " +
                    "makeModifier(${escapeString(titleCase(name.replace("skill-", "")))}, " +
                    "${parseByteAttr(row, name)}))")
            }
            else if (name =~ /^item-/) {
                name = val.trim().toLowerCase()
                def num = 1
                name.find(/^(.*?)#(\d+)$/) { str, p1, p2 ->
                    name = p1
                    num = p2.toInteger()
                }
                def itemFunc = itemNameToFunc[name]
                assert itemFunc : "Can't locate item '$name'"
                if (num > 1)
                    out.println("  addToList(@p=>p_items, setItemCount(itemScripts()=>$itemFunc(), $num))")
                else
                    out.println("  addToList(@p=>p_items, itemScripts()=>$itemFunc())")
            }
        }
        out.println("  girdPlayer(p)")
        out.println("  mmgr(FREE_MEMORY, itemScripts)")
        out.println "  return p"
    }

    def addCodeToFunc(funcName, codesString, addTo)
    {
        if (codesString == null || codesString.trim().length() == 0 || codesString.trim() == "0")
            return

        codesString.replace("\"", "").split(/[,.;]/).collect{it.trim().toLowerCase()}.grep{it!=""}.each { code ->
            if (!addTo.containsKey(code))
                addTo[code] = []
            addTo[code] << funcName
        }
    }

    def outCodeToFuncTbl(prefix, codeToFunc, out)
    {
        codeToFunc.sort { ent -> ent.key.toLowerCase() }.each { code, funcs ->
            funcs.eachWithIndex { func, index ->
                out.println(
                    "${index==0 ? "word[] $prefix${humanNameToSymbol(code, false)} = " : "word         = "}@$func")
            }
            out.println("word         = 0")
            out.println()
        }
    }

    def outCodeToFuncMethods(funcName, prefix, codeToFunc, out)
    {
        out.println("def $funcName(code)#1")
        codeToFunc.sort().each { code, funcs ->
            out.println("  if streqi(code, \"$code\"); return @$prefix${humanNameToSymbol(code, false)}; fin")
        }
        out.println("  puts(code)")
        out.println("  return fatal(\"$funcName\")")
        out.println("end\n")
    }

    def allItemFuncs(sheets)
    {
        def funcs = []
        sheets.find { it?.@name.equalsIgnoreCase("weapons") }.rows.row.findAll{it.@name}.each { row ->
            funcs << ["weapon", "NWp_${humanNameToSymbol(row.@name, false)}", funcs.size, row] }
        sheets.find { it?.@name.equalsIgnoreCase("armor") }.rows.row.findAll{it.@name}.each { row ->
            funcs << ["armor",  "NAr_${humanNameToSymbol(row.@name, false)}", funcs.size, row] }
        sheets.find { it?.@name.equalsIgnoreCase("ammo") }.rows.row.findAll{it.@name}.each { row ->
            funcs << ["ammo",   "NAm_${humanNameToSymbol(row.@name, false)}", funcs.size, row] }
        sheets.find { it?.@name.equalsIgnoreCase("items") }.rows.row.findAll{it.@name}.each { row ->
            funcs << ["item",   "NIt_${humanNameToSymbol(row.@name, false)}", funcs.size, row] }

        // Global mapping of item name to function, so that give/take functions can create items.
        funcs.each { typeName, func, index, row ->
            itemNameToFunc[row.@name.trim().toLowerCase()] = func
        }

        // And return the funcs.
        return funcs
    }

    def genAllItems(sheets)
    {
        // Grab all the raw data
        def funcs = allItemFuncs(sheets)

        // Build up the mappings from loot codes and store codes to creation functions
        def lootCodeToFuncs = [:]
        def storeCodeToFuncs = [:]
        funcs.each { typeName, func, index, row ->
            addCodeToFunc("_$func", row.@"loot-code", lootCodeToFuncs)
            addCodeToFunc("_$func", row.@"store-code", storeCodeToFuncs)
        }
        allLootCodes = (lootCodeToFuncs.keySet().collect{ it.toLowerCase() }) as Set

        // Make constants for the function table
        new File("build/src/plasma/gen_items.plh.new").withWriter { out ->
            out.println("// Generated code - DO NOT MODIFY BY HAND\n")
            out.println("const items_forLootCode = 0")
            out.println("const items_forStoreCode = 2")
            funcs.each { typeName, func, index, row ->
                out.println("const ${func} = ${(index+2)*2}")
            }
            out.println("const NUM_ITEMS = ${funcs.size()}")
        }
        replaceIfDiff("build/src/plasma/gen_items.plh")

        // Generate code
        new File("build/src/plasma/gen_items.pla.new").withWriter { out ->
            out.println("// Generated code - DO NOT MODIFY BY HAND")
            out.println()
            out.println("include \"gamelib.plh\"")
            out.println("include \"globalDefs.plh\"")
            out.println("include \"playtype.plh\"")
            out.println("include \"gen_items.plh\"")
            out.println()

            // Pre-define all the creation functions
            out.println("predef _items_forLootCode(code)#1, _items_forStoreCode(code)#1")
            funcs.each { typeName, func, index, row ->
                out.println("predef _$func")
            }
            out.println("")

            // Tables for converting loot codes and store codes to items
            outCodeToFuncTbl("lootCode_", lootCodeToFuncs, out)
            outCodeToFuncTbl("storeCode_", storeCodeToFuncs, out)

            // Next, output the function table
            out.println("word[] funcTbl = @_items_forLootCode, @_items_forStoreCode")
            funcs.each { typeName, func, index, row ->
                out.println("word         = @_$func")
            }
            out.println("")

            // Data structure filling helpers
            out.print("""\n\
def makeArmor(name, kind, price, armorValue, modifier)
  word p; p = mmgr(HEAP_ALLOC, TYPE_ARMOR)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p=>s_itemKind = mmgr(HEAP_INTERN, kind)
  p=>w_price = price
  p->b_armorValue = armorValue
  p=>p_modifiers = modifier
  return p
end

def makeWeapon_pt1(name, kind, price, modifier, ammoKind, clipSize, meleeDmg, projectileDmg)
  word p; p = mmgr(HEAP_ALLOC, TYPE_WEAPON)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p=>s_itemKind = mmgr(HEAP_INTERN, kind)
  p=>w_price = price
  p=>p_modifiers = modifier
  p=>s_ammoKind = mmgr(HEAP_INTERN, ammoKind)
  p->b_clipSize = clipSize
  p->b_clipCurrent = clipSize
  if !p->b_clipSize and projectileDmg
    p->b_clipCurrent = 1 // auto-reloading, e.g. bows
  fin
  p=>r_meleeDmg = meleeDmg
  p=>r_projectileDmg = projectileDmg
  return p
end

def makeWeapon_pt2(p, attack0, attack1, attack2, weaponRange, combatText, singleUse)
  p->ba_attacks[0] = attack0
  p->ba_attacks[1] = attack1
  p->ba_attacks[2] = attack2
  p->b_weaponRange = weaponRange
  p=>s_combatText = mmgr(HEAP_INTERN, combatText)
  if singleUse
    p->b_flags = WEAPON_FLAG_SINGLE_USE
  fin
  return p
end

def makePlainItem(name, price)
  word p; p = mmgr(HEAP_ALLOC, TYPE_PLAIN_ITEM)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p=>w_price = price
  return p
end

def makeFancyItem(name, price, kind, modifiers, count, storeAmount, lootAmount)
  word p; p = mmgr(HEAP_ALLOC, TYPE_FANCY_ITEM)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p=>w_price = price
  p=>s_itemKind = mmgr(HEAP_INTERN, kind)
  p=>p_modifiers = modifiers
  p=>w_count = count
  p=>w_storeAmount = storeAmount
  p=>r_lootAmount = lootAmount
  return p
end

""")

            // Generate all the functions themselves
            funcs.each { typeName, func, index, row ->
                withContext("$typeName '${row.@name}'")
                {
                    out.println("def _$func()")
                    switch (typeName) {
                        case "weapon": genWeapon(func, row, out); break
                        case "armor":  genArmor(func, row, out);  break
                        case "ammo":   genItem(func, row, out);   break
                        case "item":   genItem(func, row, out);   break
                        default: assert false
                    }
                    out.println("end\n")
                }
            }

            // Code for loot and store generation
            outCodeToFuncMethods("_items_forLootCode", "lootCode_", lootCodeToFuncs, out)
            outCodeToFuncMethods("_items_forStoreCode", "storeCode_", storeCodeToFuncs, out)

            // Lastly, the outer module-level code
            out.println("return @funcTbl")
            out.println("done")
        }
        replaceIfDiff("build/src/plasma/gen_items.pla")
    }

    def allPlayerFuncs(sheets)
    {
        // Grab all the raw data
        def funcs = []
        sheets.find { it?.@name.equalsIgnoreCase("players") }.rows.row.each { row ->
            if (row.@name && row.@"starting-party")
                funcs << ["NPl_${humanNameToSymbol(row.@name, false)}", funcs.size, row]
            // While we're at it, add each skill to the set of stats that can be set/gotten
            row.attributes().sort().each { name, val ->
                if (name =~ /^skill-(.*)/) {
                    name = titleCase(name.replace("skill-", ""))
                    stats[name] = escapeString(name)
                }
            }
        }

        // Global mapping of player name to function, so that add/remove functions can work.
        funcs.each { func, index, row ->
            playerNameToFunc[row.@name.trim().toLowerCase()] = func
        }
    }

    def genAllPlayers(sheets)
    {
        // Grab all the raw data
        def funcs = allPlayerFuncs(sheets)

        // Make constants for the function table
        new File("build/src/plasma/gen_players.plh.new").withWriter { out ->
            out.println("// Generated code - DO NOT MODIFY BY HAND\n")
            out.println("const makeInitialParty = 0")
            funcs.each { func, index, row ->
                out.println("const ${func} = ${(index+1)*2}")
            }
            out.println("const NUM_PLAYERS = ${funcs.size()}")
        }
        replaceIfDiff("build/src/plasma/gen_players.plh")

        // Generate code
        new File("build/src/plasma/gen_players.pla.new").withWriter { out ->
            out.println("// Generated code - DO NOT MODIFY BY HAND")
            out.println()
            out.println("include \"gamelib.plh\"")
            out.println("include \"globalDefs.plh\"")
            out.println("include \"playtype.plh\"")
            out.println("include \"gen_modules.plh\"")
            out.println("include \"gen_items.plh\"")
            out.println("include \"gen_players.plh\"")
            out.println()

            // Pre-define all the creation functions
            out.println("predef _makeInitialParty()#1")
            funcs.each { func, index, row ->
                out.println("predef _$func()#1")
            }
            out.println("")

            // Next, output the function table
            out.println("word[] funcTbl = @_makeInitialParty")
            funcs.each { func, index, row ->
                out.println("word         = @_$func")
            }
            out.println("")

            // Data structure-filling helpers
            out.print("""\n\
def makePlayer_pt1(name, intelligence, strength, agility, stamina, charisma, spirit, luck)#1
  word p
  p = mmgr(HEAP_ALLOC, TYPE_PLAYER)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p->b_intelligence = intelligence
  p->b_strength = strength
  p->b_agility = agility
  p->b_stamina = stamina
  p->b_charisma = charisma
  p->b_spirit = spirit
  p->b_luck = luck
  return p
end

def makePlayer_pt2(p, health, level, aiming, handToHand, dodging, gender)#1
  p=>w_health = health
  p->b_level = level
  p=>w_maxHealth = health
  // Non-first players are NPCs
  if global=>p_players; p->b_playerFlags = PLAYER_FLAG_NPC; fin
  p->b_aiming = aiming
  p->b_handToHand = handToHand
  p->b_dodging = dodging
  p->c_gender = gender
  initPlayerXP(p)
  return p
end

def setItemCount(p, ct)#1
  if p->t_type == TYPE_FANCY_ITEM
    p->w_count = ct
  else
    fatal(\"itemct\")
  fin
  return p // for chaining
end
""")

            // Generate all the functions themselves
            funcs.each { func, index, row ->
                withContext("player '${row.@name}'") {
                    out.println("\ndef _$func()#1")
                    genPlayer(func, row, out)
                    out.println("end\n")
                }
            }

            // Code for initial party creation
            out.println("def _makeInitialParty()#1")
            funcs.each { func, index, row ->
                if (row.@"starting-party".equalsIgnoreCase("yes"))
                    out.println("  addToList(@global=>p_players, _$func())")
            }
            out.println("  return 0")
            out.println("end\n")

            // Lastly, the outer module-level code
            out.println("return @funcTbl")
            out.println("done")
        }
        replaceIfDiff("build/src/plasma/gen_players.pla")
    }

    def genAllGlobalScripts(scripts)
    {
        def found = [] as Set
        scripts.each { script ->
            def gsmod = new ScriptModule()
            def name = humanNameToSymbol(script.@name, false)
            found << name
            gsmod.packGlobalScript(new File("build/src/plasma/gs_${name}.pla.new"), script)
            addResourceDep("globalFunc", name, "module", "gs_" + name)
            replaceIfDiff("build/src/plasma/gs_${name}.pla")
        }

        // For missing global funcs, generate a warning and a stub
        requiredGlobalScripts.each { humanName ->
            def name = humanNameToSymbol(humanName, false)
            if (!found.contains(name)) {
                printWarning("missing global function '$humanName'")
                def gsmod = new ScriptModule()
                gsmod.packStubGlobal(new File("build/src/plasma/gs_${name}.pla.new"), name)
                addResourceDep("globalFunc", name, "module", "gs_" + name)
                replaceIfDiff("build/src/plasma/gs_${name}.pla")
                globalScripts[name] = 0
                scripts << [ name: humanName ]
            }
        }
    }

    def replaceIfDiff(oldFile)
    {
        def newFile = new File(oldFile + ".new")
        oldFile = new File(oldFile)

        def newText = newFile.text
        def oldText = oldFile.exists() ? oldFile.text : ""

        if (newText == oldText) {
            //println "Same text, deleting $newFile"
            newFile.delete()
        }
        else {
            //println "Changed text, renaming $newFile to $oldFile"
            oldFile.delete() // needed on Windows
            newFile.renameTo(oldFile)
        }
    }

    def decodeImageName(rawName)
    {
        def name = rawName
        def animFrameNum = 1
        def animFlags
        def m = (name =~ /^(.*)\*(\d+)(\w*)$/)
        if (m) {
            name = m[0][1]
            animFrameNum = m[0][2].toInteger()
            animFlags = m[0][3].toLowerCase()
        }
        return [name.trim(), animFrameNum, animFlags]
    }

    def dataGen(xmlFile, dataIn)
    {
        // When generating code, we need to use Unix linebreaks since that's what
        // the PLASMA compiler expects to see.
        def oldSep = System.getProperty("line.separator")
        System.setProperty("line.separator", "\n")

        // We have to identify and number the global tiles before generating the image header.
        numberGlobalTiles(dataIn)

        // Translate image names to constants and symbol names
        new File("build/src/plasma").mkdirs()
        new File("build/src/plasma/gen_images.plh.new").withWriter { out ->
            def portraitNum = 0
            dataIn.image.sort{it.@name.toLowerCase()}.each { image ->
                def category = image.@category?.toLowerCase()
                def (name, animFrameNum, animFlags) = decodeImageName(image.@name)
                if (category == "portrait" && animFrameNum == 1) {
                    ++portraitNum
                    out.println "const PO${humanNameToSymbol(name, false)} = $portraitNum"
                    portraits[name] = []  // placeholder during dataGen phase
                }
            }
            out.println "const PO_LAST = $portraitNum\n"

            def frameNum = 3  // 1-3 are title, UI 2d, UI 3d respectively
            dataIn.image.sort{it.@name.toLowerCase()}.each { image ->
                def category = image.@category?.toLowerCase()
                def (name, animFrameNum, animFlags) = decodeImageName(image.@name)
                if (category == "fullscreen" && animFrameNum == 1 && !name.equalsIgnoreCase("title")) {
                    ++frameNum
                    out.println "const PF${humanNameToSymbol(name, false)} = $frameNum"
                    frames[name] = []  // placeholder during dataGen phase
                }
            }
            out.println "const PF_LAST = $frameNum"

            out.println ""
            for (def dir : ["north", "east", "south", "west"])
                out.println "const COMPASS_${dir.toUpperCase()} = ${compassTiles.containsKey(dir) ? compassTiles[dir] : -1}"

            out.println ""
            for (def time : ['12:00', '1:30', '3:00', '4:30', '6:00', '7:30', '9:00', '10:30'])
                out.println "const CLOCK_${time.replace(':', '_')} = ${clockTiles.containsKey(time) ? clockTiles[time] : -1}"

            out.println ""
            out.println "const BASE_AVATAR = ${avatars.values()[0]}"
        }
        replaceIfDiff("build/src/plasma/gen_images.plh")

        // Before we can generate global script code, we need to identify and number all the maps.
        numberMaps(dataIn)

        // Assign a number to each game flag
        numberGameFlags(dataIn)

        // Form the translation from item name to function name (and ditto
        // for players)
        allItemFuncs(dataIn.global.sheets.sheet)
        allPlayerFuncs(dataIn.global.sheets.sheet)

        // Translate global scripts to code. Record their deps as system-level.
        curMapName = "<root>"
        recordGlobalScripts(dataIn)
        genAllGlobalScripts(dataIn.global.scripts.script)
        curMapName = null

        // Generate a mapping of flags, for debugging purposes.
        genAllFlags()

        // Translate enemies, weapons, etc. to code
        genAllItems(dataIn.global.sheets.sheet)
        genAllEnemies(dataIn.global.sheets.sheet.find { it?.@name.equalsIgnoreCase("enemies") })
        genAllPlayers(dataIn.global.sheets.sheet)

        // Produce a list of assembly and PLASMA code segments
        binaryStubsOnly = true
        readAllCode()
        binaryStubsOnly = false
        new File("build/src/plasma/gen_modules.plh.new").withWriter { out ->
            code.each { k, v ->
                out.println "const CODE_${humanNameToSymbol(k, true)} = ${v.num}"
            }
            out.println ""
            modules.each { k, v ->
                if (humanNameToSymbol(k, true).startsWith("GS_"))
                    out.println "const ${humanNameToSymbol(k, true)} = ${v.num}"
                else
                    out.println "const MOD_${humanNameToSymbol(k, true)} = ${v.num}"
            }
        }
        replaceIfDiff("build/src/plasma/gen_modules.plh")

        // Put back the default line separator
        System.setProperty("line.separator", oldSep)
    }

    def copyIfNewer(fromFile, toFile)
    {
        if (!toFile.exists() || fromFile.lastModified() > toFile.lastModified()) {
            toFile.delete()
            Files.copy(fromFile.toPath(), toFile.toPath())
        }
    }

    def copyOrCreateSave(dstDir)
    {
        def prevSave = new File("build/prevGame/game.1.save.bin#0")
        def newSave  = new File("${dstDir}/game.1.save.bin")
        if (prevSave.exists()) {
            copyIfNewer(prevSave, newSave)
        }
        else {
            // Create empty save file
            newSave.withOutputStream { outStream ->
                (0..3071).each {
                    outStream.write( (byte) 0)
                }
            }
        }
    }

    def createHddImage()
    {
        //println "Creating hdd image."

        // Copy the combined core executable to the output directory
        copyIfNewer(new File("build/src/core/build/LEGENDOS.SYSTEM.sys#2000"),
                    new File("build/root/LEGENDOS.SYSTEM.sys#2000"))

        copyIfNewer(jitCopy(new File("build/tools/ProRWTS/PROBOOT#800")),
                    new File("build/root/PROBOOT.sys#800"))

        // If we preserved a previous save game, copy it to the new image.
        copyOrCreateSave("build/root")

        // Decompress the base image.
        // No need to delete old file; that was done by outer-level code.
        def dst = new File("game.2mg")
        Files.copy(new GZIPInputStream(new FileInputStream(
            jitCopy(new File("build/data/disks/base_800k.2mg.gz")))), dst.toPath())

        // Now put the files into the image
        String[] args = ["-put", "game.2mg", "/", "build/root"]
        new a2copy.A2Copy().main(args)
    }

    def createFloppyImages()
    {
        //println "Creating floppy images."

        // We'll be copying stuff from the hdd directory
        def hddDir = new File("build/root")

        // Build a DSK image for each floppy
        for (int i=1; i<=MAX_DISKS; i++) 
        {
            // Skip later partitions if they don't exist
            def partFile = new File("build/root/game.part.${i}.bin")
            if (!partFile.exists())
                continue

            // Copy files.
            def rootDir = new File("build/root$i")
            rootDir.mkdir()
            if (i == 1) {
                copyIfNewer(new File(hddDir, "LEGENDOS.SYSTEM.sys#2000"), new File(rootDir, "LEGENDOS.SYSTEM.sys#2000"))
                copyOrCreateSave("build/root$i")
            }

            copyIfNewer(new File(hddDir, "game.part.${i}.bin"), new File(rootDir, "game.part.${i}.bin"))

            // Unzip a fresh empty disk image
            def dst = new File("game${i}.dsk")
            Files.copy(new GZIPInputStream(new FileInputStream(
                jitCopy(new File("build/data/disks/base_140k.dsk.gz")))), dst.toPath())

            // Now put the files into the image
            String[] args = ["-put", "game${i}.dsk", "/", "build/root${i}"]
            new a2copy.A2Copy().main(args)
        }
    }

    static void packWorld(String xmlPath, Object watcher)
    {
        // If there's an existing error file, remove it first, so user doesn't
        // get confused by an old file.
        def reportFile = new File("pack_report.txt")
        if (reportFile.exists())
            reportFile.delete()
        reportFile.withWriter { reportWriter ->

            def inst1, inst2
            try {
                File xmlFile = new File(xmlPath)

                def flag = false
                try {
                    assert false
                }
                catch (AssertionError e) {
                    flag = true
                }
                if (!flag)
                    throw "Assertions must be enabled"

                // Create the build directory if necessary
                def buildDir = new File("build").getCanonicalFile()
                if (!buildDir.exists())
                    buildDir.mkdirs()

                // Also remove existing game image if any, for the same reason.
                def gameFile = new File("game.2mg")
                if (gameFile.exists())
                {
                    // We want to preserve any existing saved game, so grab files from the old image.
                    String[] args2 = ["-get", "game.2mg", "/", "build/prevGame"]
                    new a2copy.A2Copy().main(args2)

                    // Then delete the old image.
                    gameFile.delete()
                }

                // Also remove existing floppy disks for this game.
                for (int i=1; i<=MAX_DISKS; i++) {
                    def diskFile = new File("game${i}.dsk")
                    if (diskFile.exists())
                        diskFile.delete()
                    def partFile = new File("build/root/game.part.${i}.bin")
                    if (partFile.exists())
                        partFile.delete()
                }

                // Open the XML data file produced by Outlaw Editor
                def dataIn = new XmlParser().parse(xmlPath)

                // Create PLASMA headers
                inst1 = new A2PackPartitions()
                inst1.buildDir = buildDir
                inst1.reportWriter = reportWriter
                inst1.dataGen(xmlFile, dataIn)

                // Save the partial resource deps
                def resourceDeps = inst1.resourceDeps

                // Pack everything into a binary file
                inst2 = new A2PackPartitions() // make a new one without stubs
                inst2.warningBuf = inst1.warningBuf
                inst2.nWarnings = inst1.nWarnings
                inst2.resourceDeps = resourceDeps // inject partial deps
                inst2.buildDir = buildDir
                inst2.reportWriter = reportWriter
                inst2.pack(xmlFile, dataIn)

                // And create the final disk images
                inst2.createHddImage()
                inst2.createFloppyImages()
            }
            catch (Throwable t) {
                reportWriter.println "Packing error:\n${t.message}"
                reportWriter.println "       detail:\n$t"
                if (inst2) {
                    reportWriter.println "\nContext:"
                    reportWriter.println "    ${inst2.getContextStr()}"
                }
                else if (inst1) {
                    reportWriter.println "\nContext:"
                    reportWriter.println "    ${inst1.getContextStr()}"
                }
                reportWriter.println "\nGroovy call stack:"
                t.getStackTrace().each {
                    if (!(it.toString() ==~ /.*(groovy|reflect)\..*/))
                        reportWriter.println "    $it"
                }
                reportWriter.flush()
                reportFile.eachLine { println it }
                watcher.error(t.message, inst2 ? inst2.getContextStr() : inst1 ? inst1.getContextStr() : 'outer')
            }

            if (inst2.nWarnings > 0) {
                reportWriter.println "Packing warnings:\n"
                reportWriter.println inst2.warningBuf.toString()
                reportWriter.write()
                watcher.warnings(inst2.nWarnings, inst2.warningBuf.toString())
            }
        }
    }

    static class CommandLineWatcher
    {
        def error(msg, context)
        {
            javax.swing.JOptionPane.showMessageDialog(null, \
                "Fatal error encountered in\n" + \
                "$context:\n" + \
                "$msg.\n" + \
                "Details written to file 'pack_report.txt'.", "Fatal packing error",
                javax.swing.JOptionPane.ERROR_MESSAGE)
            System.exit(1)
        }

        def warnings(nWarnings, str)
        {
            javax.swing.JOptionPane.showMessageDialog(null,
                "${nWarnings} warning(s) noted during packing.\nDetails written to 'pack_report.txt'.",
                "Pack warnings",
                javax.swing.JOptionPane.ERROR_MESSAGE)
        }
    }

    static void main(String[] args)
    {
        // Set auto-flushing for stdout
        System.out = new PrintStream(new BufferedOutputStream(System.out), true)

        // Check the arguments
        if (args.size() > 1) {
            println "Usage: java -jar packPartitions.jar [path/to/world.xml]"
            println "If no path supplied, assumes world.xml in current directory."
            System.exit(1);
        }
        def xmlFile = args.size() == 1 ? args[0] : "world.xml"
        packWorld(xmlFile, new CommandLineWatcher())
    }

    class ScriptModule
    {
        PrintWriter out
        def locationsWithTriggers = [] as Set
        def scriptNames = [:]
        def scriptArgs = [:]
        def indent = 0
        def variables = [] as Set

        def getScriptName(script)
        {
            if (script.block.size() == 0) {
                if (script.@name)
                    return script.@name
                return null
            }

            def blk = script.block[0]
            if (blk.field.size() == 0) {
                if (scriptNames.containsKey(script))
                    return scriptNames[script]
                return null
            }

            assert blk.field[0].@name == "NAME"
            return blk.field[0].text()
        }

        def startScriptFile(outFile)
        {
            out = new PrintWriter(new FileWriter(outFile))
            out << "// Generated code - DO NOT MODIFY BY HAND\n\n"
            out << "include \"../plasma/gamelib.plh\"\n"
            out << "include \"../plasma/globalDefs.plh\"\n"
            out << "include \"../plasma/playtype.plh\"\n"
            out << "include \"../plasma/gen_flags.plh\"\n"
            out << "include \"../plasma/gen_images.plh\"\n"
            out << "include \"../plasma/gen_items.plh\"\n"
            out << "include \"../plasma/gen_modules.plh\"\n"
            out << "include \"../plasma/gen_players.plh\"\n\n"
        }

        /**
         * Pack a global script (not associated with any particular map).
         */
        def packGlobalScript(outFile, script)
        {
            startScriptFile(outFile)

            def name = getScriptName(script)
            assert name : "Can't find script name in $script"
            getScriptArgs(script)
            scriptNames[script] = "sc_${humanNameToSymbol(name, false)}"
            packScript(script)

            // Set up the pointer to global vars and finish up the module.
            if (script.block.size() == 0)
                out << "return 0\n"
            else
                out << "return @${scriptNames[script]}\n"
            out << "done\n"
            out.close()
        }

        /**
         * Generate a stub for a missing global function.
         */
        def packStubGlobal(outFile, humanName)
        {
            startScriptFile(outFile)

            def name = humanNameToSymbol(humanName, false)
            out << "def ${name}()\n"
            out << "  displayStr(\"Missing script '" << humanName << "'\\n\")\n"
            out << "  return 0\n"
            out << "end\n\n"
            out << "return @${name}\n"
            out << "done\n"
            out.close()
        }

        /**
         * Pack scripts from a map. Either the whole map, or optionally just an X and Y
         * bounded section of it.
         */
        def packMapScripts(mapName, outFile, inScripts, maxX, maxY, xRange = null, yRange = null)
        {
            startScriptFile(outFile)

            // Determine which scripts are referenced in the specified section of the map.
            def initScript
            def scripts = []
            inScripts.script.eachWithIndex { script, idx ->
                def name = getScriptName(script)
                if (name != null && name.toLowerCase() == "init") {
                    initScript = script
                }
                else if (script.locationTrigger.any { trig ->
                            (!xRange || trig.@x.toInteger() in xRange) &&
                            (!yRange || trig.@y.toInteger() in yRange) })
                {
                    scripts << script
                }
                if (name != null)
                    scriptNames[script] = "sc_${humanNameToSymbol(name, false)}"
            }

            // Generate the table of triggers, and code for each script.
            makeTriggerTbl(scripts, xRange, yRange)
            scripts.each { script ->
                packScript(script)
            }

            // Even if there were no scripts, we still need an init to display
            // the map name.
            makeInit(mapName, initScript, maxX, maxY)

            out.close()
        }

        def outIndented(str) {
            out << ("  " * indent) << str
        }

        def getScriptArgs(script)
        {
            scriptArgs[script] = [] as Set
            if (script.block.size() != 0) {
                def proc = script.block[0]
                if (proc.mutation) {
                    proc.mutation.arg.eachWithIndex { arg, idx ->
                        scriptArgs[script] << "v_" + humanNameToSymbol(arg.@name, false)
                    }
                }
            }
        }

        def packScript(script)
        {
            //println "   Script '$name'"
            withContext(scriptNames[script])
            {
                if (script.block.size() == 0) {
                    printWarning("empty script found; skipping.")
                    return
                }
                def proc = script.block[0]

                // Record the function's name and start its definition
                out << "def ${scriptNames[script]}("

                // If the script takes arguments, mark those and add them to the definition
                scriptArgs[script].eachWithIndex { arg, idx ->
                    if (idx > 0)
                        out << ", "
                    out << arg
                }
                out << ")\n"
                indent = 1

                // Need to queue up the script, to find out what variables need
                // to be declared.
                def outerOutput = out
                def buf = new StringWriter()
                out = new PrintWriter(buf)
                variables = [] as Set

                // Process the code inside it
                assert proc.@type == "procedures_defreturn"
                if (proc.statement.size() > 0) {
                    assert proc.statement.size() == 1
                    def stmt = proc.statement[0]
                    assert stmt.@name == "STACK"
                    stmt.block.each { packBlock(it) }
                }
                else
                    printWarning "empty statement found; skipping."

                // Process the script return value
                outIndented("return ")
                if (proc.value.size() > 0) {
                    assert proc.value[0].@name == "RETURN"
                    assert proc.value[0].block.size() == 1
                    packExpr(proc.value[0].block[0], true)
                }
                else
                    out << "0"
                out << "\n"

                // Define all the variables that were mentioned (except the args)
                out.close()
                out = outerOutput
                (variables - scriptArgs[script]).each { var ->
                    outIndented("word $var\n")
                }
                (variables - scriptArgs[script]).each { var ->
                    outIndented("$var = 0\n")
                }

                // And complete the function
                out << buf.toString() + "end\n\n"
            }
        }

        def packBlock(blk)
        {
            withContext("${blk.@type}") {
                switch (blk.@type)
                {
                    case 'text_print':
                    case 'text_println':
                        packTextPrint(blk); break
                    case 'text_clear_window':
                        packClearWindow(blk); break
                    case 'text_getanykey':
                        packGetAnyKey(blk); break
                    case  'controls_if':
                        packIfStmt(blk); break
                    case 'flow_repeat':
                        packRepeatStmt(blk); break
                    case 'events_set_map':
                        packSetMap(blk); break
                    case 'events_set_sky':
                        packSetSky(blk); break
                    case 'events_set_ground':
                        packSetGround(blk); break
                    case 'events_add_encounter_zone':
                        packAddEncounterZone(blk); break
                    case 'events_clr_encounter_zones':
                        packClrEncounterZones(blk); break
                    case 'events_start_encounter':
                        packStartEncounter(blk); break
                    case 'events_teleport':
                        packTeleport(blk); break
                    case 'events_move_backward':
                        packMoveBackward(blk); break
                    case 'graphics_set_portrait':
                        packSetPortrait(blk); break
                    case 'graphics_clr_portrait':
                    case 'graphics_clr_fullscreen':
                        packClrPortrait(blk); break
                    case 'graphics_set_fullscreen':
                        packSetFullscreen(blk); break
                    case 'graphics_set_avatar':
                        packSetAvatar(blk); break
                    case 'graphics_swap_tile':
                        packSwapTile(blk); break
                    case 'graphics_intimate_mode':
                        packIntimateMode(blk); break
                    case 'variables_set':
                        packVarSet(blk); break
                    case 'interaction_give_item':
                        packGiveItem(blk); break
                    case 'interaction_take_item':
                        packTakeItem(blk); break
                    case 'interaction_add_player':
                        packAddPlayer(blk); break
                    case 'interaction_remove_player':
                        packRemovePlayer(blk); break
                    case 'interaction_bench_player':
                        packBenchPlayer(blk); break
                    case 'interaction_unbench_player':
                        packUnbenchPlayer(blk); break
                    case 'interaction_increase_stat':
                    case 'interaction_decrease_stat':
                        packChangeStat(blk); break
                    case 'interaction_increase_party_stats':
                    case 'interaction_decrease_party_stats':
                        packChangePartyStats(blk); break
                    case 'interaction_set_flag':
                    case 'interaction_clr_flag':
                        packChangeFlag(blk); break
                    case 'interaction_pause':
                        packPause(blk); break
                    case 'interaction_buy_from_store':
                        packBuyFromStore(blk); break
                    case 'interaction_sell_to_store':
                        packSellToStore(blk); break
                    case ~/^Globalignore_.*$/:
                        packGlobalCall(blk); break
                    default:
                        printWarning "don't know how to pack block of type '${blk.@type}'"
                }
            }

            // Strangely, blocks seem to be chained together, but hierarchically. Whatever.
            blk.next.each { it.block.each { packBlock(it) } }
        }

        def getSingle(els, name = null, type = null)
        {
            assert els.size() == 1
            def first = els[0]
            if (name)
                assert first.@name == name
            if (type)
                assert first.@type == type
            return first
        }

        def packTextPrint(blk)
        {
            if (blk.value.size() == 0) {
                printWarning "empty text_print block, skipping."
                return
            }
            def valBlk = getSingle(blk.value, 'VALUE').block
            assert valBlk.size() == 1
            if (valBlk[0].@type == 'text')
            {
                // Break up long strings into shorter chunks for PLASMA.
                // Note: this used to be 253, but still had some random mem overwrites.
                // Decreasing to chunks of 200 seems to fix it.
                def text = getSingle(getSingle(valBlk, null, 'text').field, 'TEXT').text()
                def chunks = text.findAll(/.{200}|.*/).grep(~/.+/)
                if (!text || text == "") // interpret lack of text as a single empty string
                    chunks = [""]
                chunks.eachWithIndex { chunk, idx ->
                    outIndented((idx == chunks.size()-1 && blk.@type == 'text_println') ? \
                        'scriptDisplayStrNL(' : 'scriptDisplayStr(')
                    out << escapeString(chunk) << ")\n"
                    // Workaround for strings filling up the frame stack
                    outIndented("tossStrings()\n")
                }
            }
            else {
                // For general expressions instead of literal strings, we can't do
                // any fancy breaking-up business. Just pack.
                outIndented(blk.@type == 'text_println' ? 'scriptDisplayStrNL(' : 'scriptDisplayStr(')
                packExpr(valBlk[0])
                out << ")\n"
            }
        }

        def packClearWindow(blk)
        {
            assert blk.value.size() == 0
            outIndented("clearWindow()\n")
        }

        def packGetAnyKey(blk)
        {
            assert blk.value.size() == 0
            outIndented("getUpperKey()\n")
        }

        def packVarSet(blk)
        {
            def name = "v_" + humanNameToSymbol(getSingle(blk.field, 'VAR'), false)
            variables << name
            outIndented("$name = ")
            packExpr(getSingle(getSingle(blk.value).block), true) // true to intern any strings
            out << "\n"
        }

        def packGiveItem(blk)
        {
            def name = getSingle(blk.field, 'NAME').text().trim()
            def itemFunc = itemNameToFunc[name.toLowerCase()]
            assert itemFunc : "Can't locate item '$name'"
            outIndented("giveItemToPlayer(global=>p_players, $itemFunc)\n")
        }

        def packTakeItem(blk)
        {
            def name = getSingle(blk.field, 'NAME').text().trim()
            assert itemNameToFunc.containsKey(name.toLowerCase()) : "Can't locate item '$name'"
            outIndented("takeItemFromPlayer(global=>p_players, ${escapeString(name)})\n")
        }

        def packBuyFromStore(blk)
        {
            assert blk.value.size() == 2
            assert blk.value[0].@name == 'CODE'
            assert blk.value[1].@name == 'PROFIT'
            outIndented("buySell(")
            assert blk.value[0].block.size() == 1
            packExpr(blk.value[0].block[0])
            out << (", ")
            assert blk.value[1].block.size() == 1
            packExpr(blk.value[1].block[0])
            out << ")\n"
        }

        def packSellToStore(blk)
        {
            assert blk.value.size() == 1
            assert blk.value[0].@name == 'PROFIT'
            outIndented("buySell(NULL, ")
            assert blk.value[0].block.size() == 1
            packExpr(blk.value[0].block[0])
            out << ")\n"
        }

        def packAddPlayer(blk)
        {
            def name = getSingle(blk.field, 'NAME').text().trim()
            def playerFunc = playerNameToFunc[name.toLowerCase()]
            assert playerFunc : "Can't locate player '$name'"
            outIndented("addPlayerToParty($playerFunc)\n")
        }

        def packRemovePlayer(blk)
        {
            def name = getSingle(blk.field, 'NAME').text().trim()
            assert playerNameToFunc.containsKey(name.toLowerCase()) : "Can't locate player '$name'"
            outIndented("removePlayerFromParty(${escapeString(name)})\n")
        }

        def packBenchPlayer(blk)
        {
            assert blk.field.size() == 0
            outIndented("benchPlayer()\n")
        }

        def packUnbenchPlayer(blk)
        {
            assert blk.field.size() == 0
            outIndented("unbenchPlayer()\n")
        }

        def nameToStat(name) {
            def lcName = name.toLowerCase().trim()
            def match = null
            stats.each { stat, symbol ->
                if (stat.equalsIgnoreCase(name))
                    match = symbol
            }
            assert match : "Unrecognized stat '$name'"
            return match
        }

        def packChangeStat(blk)
        {
            assert blk.field.size() == 2
            assert blk.field[0].@name == 'NAME'
            assert blk.field[1].@name == 'AMOUNT'
            def name = blk.field[0].text()
            def amount = blk.field[1].text().toInteger()
            assert amount > 0 && amount < 32767
            def stat = nameToStat(name)
            outIndented(
                "setStat(global=>p_players, $stat, getStat(global=>p_players, $stat) ${blk.@type == 'interaction_increase_stat' ? '+' : '-'} $amount)\n")
        }

        def packChangePartyStats(blk)
        {
            assert blk.field.size() == 2
            assert blk.field[0].@name == 'NAME'
            assert blk.field[1].@name == 'AMOUNT'
            def name = blk.field[0].text()
            def amount = blk.field[1].text().toInteger()
            assert amount > 0 && amount < 32767
            def stat = nameToStat(name)
            variables << "p_player"
            outIndented("p_player = global=>p_players\n")
            outIndented("while p_player\n")
            ++indent
            outIndented(
                "setStat(p_player, $stat, getStat(p_player, $stat) ${blk.@type == 'interaction_increase_party_stats' ? '+' : '-'} $amount)\n")
            outIndented("p_player = p_player=>p_nextObj\n")
            --indent
            outIndented("loop\n")
        }

        def packPause(blk)
        {
            def num = getSingle(blk.field, 'NUM').text()
            assert num.toFloat() > 0
            def factor = 1200 // approx counts per second
            def time = (int)(num.toFloat() * factor)
            if (time > 32767)
                time = 32767
            outIndented("pause($time)\n")
        }

        def packGlobalCall(blk)
        {
            def m = blk.@type =~ /^Globalignore_(.*)$/
            def humanName = m ? m.group(1) : null
            assert humanName
            def funcName = humanNameToSymbol(humanName, false)

            // Check that the function exists, and that we're passing the right number of args to it
            if (!globalScripts.containsKey(funcName)) {
                println globalScripts.keySet()
                printWarning "Call to unknown script '$humanName'; skipping."
                return
            }
            if (blk.value.size() != globalScripts[funcName]) {
                printWarning "Wrong number of args to script '$humanName'"
                return
            }
            if (blk.value.size() > 3) {
                printWarning "Current limit is max of 3 args to global scripts."
                return
            }

            // Now generate the code. Pad with zeros to make exactly 3 args
            outIndented("callGlobalFunc(GS_${humanNameToSymbol(funcName, true)}")
            (0..<3).each { idx ->
                out << ", "
                if (idx < blk.value.size()) {
                    assert blk.value[idx].block.size() == 1
                    packExpr(blk.value[idx].block[0])
                }
                else
                    out << "0"
            }
            out << ")\n"

            addMapDep("globalFunc", funcName)
        }

        def packGetStat(blk)
        {
            def name = getSingle(blk.field, 'NAME').text()
            def stat = nameToStat(name)
            out << "getStat(global=>p_players, $stat)"
        }

        def packGetFlag(blk)
        {
            def name = getSingle(blk.field, 'NAME').text().trim().toLowerCase()
            assert gameFlags.containsKey(name)
            out << "getGameFlag(GF_${humanNameToSymbol(name, true)})"
        }

        def packChangeFlag(blk)
        {
            def name = getSingle(blk.field, 'NAME').text().trim().toLowerCase()
            assert gameFlags.containsKey(name)
            outIndented("setGameFlag(GF_${humanNameToSymbol(name, true)}, ${blk.@type == 'interaction_set_flag' ? 1 : 0})\n")
        }

        def isStringExpr(blk)
        {
            return blk.@type == "text_getstring" || blk.@type == "text_getcharacter" || blk.@type == "text"
        }

        def packRandomInt(blk)
        {
            assert blk.value[0].@name == 'FROM'
            assert blk.value[1].@name == 'TO'
            def low = getSingle(blk.value[0].block)
            def high = getSingle(blk.value[1].block)
            out << "((rand16() % ("; packExpr(high); out << "-"; packExpr(low); out << "+1))+"; packExpr(low); out << ")"
        }

        def packLogicCompare(blk)
        {
            def op = getSingle(blk.field, "OP").text()
            assert blk.value[0].@name == 'A'
            assert blk.value[1].@name == 'B'
            def val1 = getSingle(blk.value[0].block)
            def val2 = getSingle(blk.value[1].block)
            switch (op) {
                case 'EQ':
                    if (isStringExpr(val1) || isStringExpr(val2)) {
                        out << "streqi("; packExpr(val1); out << ", "; packExpr(val2); out << ")"
                    }
                    else {
                        packExpr(val1); out << " == "; packExpr(val2)
                    }
                    break
                case 'NEQ':
                    if (isStringExpr(val1) || isStringExpr(val2)) {
                        out << "!streqi("; packExpr(val1); out << ", "; packExpr(val2); out << ")"
                    }
                    else {
                        packExpr(val1); out << " <> "; packExpr(val2)
                    }
                    break
                case 'GT':
                    packExpr(val1); out << " > "; packExpr(val2); break
                case 'GTE':
                    packExpr(val1); out << " >= "; packExpr(val2); break
                case 'LT':
                    packExpr(val1); out << " < "; packExpr(val2); break
                case 'LTE':
                    packExpr(val1); out << " <= "; packExpr(val2); break
                default:
                    assert false : "Compare op '$op' not yet implemented."
            }
        }

        def packLogicOperation(blk)
        {
            def op = getSingle(blk.field, "OP").text()
            assert blk.value[0].@name == 'A'
            assert blk.value[1].@name == 'B'
            def val1 = getSingle(blk.value[0].block)
            def val2 = getSingle(blk.value[1].block)
            switch (op) {
                case 'AND':
                    packExpr(val1); out << " and "; packExpr(val2)
                    break
                case 'OR':
                    packExpr(val1); out << " or "; packExpr(val2)
                    break
                default:
                    assert false : "Logic op '$op' not yet implemented."
            }
        }

        def packLogicNegate(blk) {
            out << "!("; packExpr(getSingle(blk.value, "BOOL").block[0]); out << ")"
        }

        def packMathArithmetic(blk)
        {
            def op = getSingle(blk.field, "OP").text()
            assert blk.value[0].@name == 'A'
            assert blk.value[1].@name == 'B'
            packExpr(getSingle(blk.value[0].block))
            switch (op) {
                case 'ADD':
                    out << " + "; break
                case 'MINUS':
                    out << " - "; break
                case 'MULTIPLY':
                    out << " * "; break
                case 'DIVIDE':
                    out << " / "; break
                default:
                    assert false : "Arithmetic op '$op' not yet implemented."
            }
            packExpr(getSingle(blk.value[1].block))
        }

        def packVarGet(blk)
        {
            def name = "v_" + humanNameToSymbol(getSingle(blk.field, "VAR").text(), false)
            variables << name
            out << name
        }

        def packHasItem(blk)
        {
            def name = getSingle(blk.field, "NAME").text().trim()
            assert itemNameToFunc.containsKey(name.toLowerCase()) : "Can't locate item '$name'"
            out << "playerHasItem(${escapeString(name)})"
        }

        def packHasPlayer(blk)
        {
            def name = getSingle(blk.field, "NAME").text().trim()
            assert playerNameToFunc.containsKey(name.toLowerCase()) : "Can't locate player '$name'"
            out << "partyHasPlayer(${escapeString(name)})"
        }

        def packExpr(blk, internString = false)
        {
            switch (blk.@type) {
                case 'math_number':
                    out << getSingle(blk.field, "NUM").text().toInteger()
                    break
                case 'math_random_int':
                    packRandomInt(blk)
                    break
                case 'text_getboolean':
                    out << "getYN()"
                    break
                case 'text_getstring':
                    out << "getStringResponse()"
                    break
                case 'text_getcharacter':
                    out << "getCharResponse()"
                    break
                case 'logic_compare':
                    packLogicCompare(blk)
                    break
                case 'math_arithmetic':
                    packMathArithmetic(blk)
                    break
                case 'logic_operation':
                    packLogicOperation(blk)
                    break
                case 'logic_negate':
                    packLogicNegate(blk)
                    break
                case 'variables_get':
                    packVarGet(blk)
                    break
                case 'text':
                    if (internString)
                        out << "mmgr(HEAP_INTERN, "
                    out << escapeString(getSingle(blk.field, 'TEXT').text())
                    if (internString)
                        out << ")"
                    break
                case 'interaction_has_item':
                    packHasItem(blk)
                    break
                case 'interaction_has_player':
                    packHasPlayer(blk)
                    break
                case 'interaction_get_stat':
                    packGetStat(blk)
                    break
                case 'interaction_get_flag':
                    packGetFlag(blk)
                    break
                default:
                    assert false : "Expression type '${blk.@type}' not yet implemented."
            }
        }

        def packIfStmt(blk)
        {
            if (blk.value.size() == 0) {
                printWarning "missing condition; skipping."
                return
            }
            blk.statement.eachWithIndex { stmt, idx ->
                if (stmt.@name == "ELSE")
                    outIndented("else\n")
                else {
                    assert stmt.@name == "DO$idx"
                    def val = blk.value[idx]
                    assert val.@name == "IF$idx"
                    outIndented("${idx==0 ? 'if' : 'elsif'} ")
                    packExpr(getSingle(val.block))
                    out << "\n"
                }
                ++indent
                packBlock(getSingle(stmt.block))
                --indent
            }
            outIndented("fin\n")
        }

        def packRepeatStmt(blk)
        {
            if (blk.statement.size() == 0) {
                printWarning "missing repeat body; skipping."
                return
            }
            if (blk.value.size() != 1) {
                printWarning "missing repeat condition; skipping."
                return
            }
            outIndented("while 1\n")
            ++indent
            blk.statement.each { packBlock(it.block[0]) }
            outIndented("if ")
            packExpr(blk.value.block[0])
            out << "; break; fin\n"
            --indent
            outIndented("loop\n")
        }

        def packSetMap(blk)
        {
            assert blk.field.size() == 4
            assert blk.field[0].@name == 'NAME'
            assert blk.field[1].@name == 'X'
            assert blk.field[2].@name == 'Y'
            assert blk.field[3].@name == 'FACING'
            def mapName = blk.field[0].text()
            def mapNum = mapNames[mapName]
            if (!mapNum) {
                printWarning "map '$mapName' not found; skipping set_map."
                return
            }
            assert mapNum[0] == '2D' || mapNum[0] == '3D'
            def x = blk.field[1].text().toInteger()
            def y = blk.field[2].text().toInteger()
            def facing = blk.field[3].text().toInteger()
            assert facing >= 0 && facing <= 15

            outIndented("queue_setMap(${mapNum[0] == '2D' ? 0 : 1}, ${mapNum[1]}, $x, $y, $facing)\n")
            outIndented("return 0\n")
        }

        def packSetPortrait(blk)
        {
            def portraitName = getSingle(blk.field, 'NAME').text()
            if (!portraits.containsKey(portraitName)) {
                printWarning "portrait '$portraitName' not found; skipping set_portrait."
                return
            }
            outIndented("setPortrait(PO${humanNameToSymbol(portraitName, false)})\n")
            addMapDep("portrait", portraitName)
        }

        def packSetFullscreen(blk)
        {
            def imgName = getSingle(blk.field, 'NAME').text()
            if (!frames.containsKey(imgName)) {
                printWarning "full screen image '$imgName' not found; skipping set_fullscreen."
                return
            }
            outIndented("loadFrameImg(PF${humanNameToSymbol(imgName, false)})\n")
            addMapDep("frame", imgName)
        }

        def packClrPortrait(blk)
        {
            assert blk.field.size() == 0
            outIndented("clearPortrait()\n")
        }

        def packSetAvatar(blk)
        {
            def tileName = getSingle(blk.field, 'NAME').text().trim()
            if (!avatars.containsKey(tileName.toLowerCase())) {
                println(avatars)
                throw new Exception("Can't find avatar '$tileName'")
            }
            outIndented("scriptSetAvatar(${avatars[tileName.toLowerCase()]})\n")
        }

        def packSwapTile(blk)
        {
            assert blk.field.size() == 4
            assert blk.field[0].@name == 'FROM_X'
            assert blk.field[1].@name == 'FROM_Y'
            assert blk.field[2].@name == 'TO_X'
            assert blk.field[3].@name == 'TO_Y'
            def fromX = blk.field[0].text().toInteger()
            def fromY = blk.field[1].text().toInteger()
            def toX   = blk.field[2].text().toInteger()
            def toY   = blk.field[3].text().toInteger()
            outIndented("scriptSwapTile($fromX, $fromY, $toX, $toY)\n")
        }

        def packIntimateMode(blk)
        {
            def enableFlg = getSingle(blk.field, 'FLAG').text()
            assert enableFlg == "0" || enableFlg == "1"
            outIndented("setIntimateMode($enableFlg)\n")
        }

        def packSetSky(blk)
        {
            def color = getSingle(blk.field, 'COLOR').text().toInteger()
            assert (color >= 0 && color <= 17) || (color == 99)
            outIndented("setSky($color)\n")
        }

        def packSetGround(blk)
        {
            def color = getSingle(blk.field, 'COLOR').text().toInteger()
            assert color >= 0 && color <= 17
            outIndented("setGround($color)\n")
        }

        def packAddEncounterZone(blk)
        {
            assert blk.field.size() == 5
            assert blk.field[0].@name == 'CODE'
            assert blk.field[1].@name == 'X'
            assert blk.field[2].@name == 'Y'
            assert blk.field[3].@name == 'MAXDIST'
            assert blk.field[4].@name == 'CHANCE'
            def code = blk.field[0].text()
            def x = blk.field[1].text().toInteger()
            def y = blk.field[2].text().toInteger()
            def maxDist = blk.field[3].text().toInteger()
            def chance = (int)(blk.field[4].text().toFloat() * 10.0)
            assert chance > 0 && chance <= 1000
            outIndented("addEncounterZone(${escapeString(code)}, $x, $y, $maxDist, $chance)\n")
            addMapDep("encounterZone", code.toLowerCase())
        }

        def packClrEncounterZones(blk)
        {
            outIndented("clearEncounterZones()\n")
        }

        def packStartEncounter(blk)
        {
            assert blk.field.size() == 1
            def code = getSingle(blk.field, 'CODE')
            outIndented("if !scriptCombat(${escapeString(code)})); return 0; fin\n")
        }

        def packTeleport(blk)
        {
            assert blk.field.size() == 3
            assert blk.field[0].@name == 'X'
            assert blk.field[1].@name == 'Y'
            assert blk.field[2].@name == 'FACING'
            def x = blk.field[0].text().toInteger()
            def y = blk.field[1].text().toInteger()
            def facing = blk.field[2].text().toInteger()
            assert facing >= 0 && facing <= 15
            outIndented("queue_teleport($x, $y, $facing)\n")
        }

        def packMoveBackward(blk)
        {
            assert blk.field.size() == 0
            outIndented("moveWayBackward()\n")
        }

        def makeTriggerTbl(scripts, xRange, yRange)
        {
            // Emit a predefinition for each function
            scripts.eachWithIndex { script, idx ->
                getScriptArgs(script)
                out << "predef ${scriptNames[script]}("
                scriptArgs[script].eachWithIndex { name, num ->
                    if (num > 0)
                        out << ", "
                    out << name
                }
                out << ")#1\n"
            }

            // Collate all the matching location triggers into a sorted map.
            TreeMap triggers = [:]
            scripts.eachWithIndex { script, idx ->
                if (scriptNames[script]) {
                    script.locationTrigger.each { trig ->
                        def x = trig.@x.toInteger()
                        def y = trig.@y.toInteger()
                        if ((!xRange || x in xRange) && (!yRange || y in yRange))
                        {
                            if (xRange)
                                x -= xRange[0]
                            if (yRange)
                                y -= yRange[0]
                            if (!triggers[y])
                                triggers[y] = [:] as TreeMap
                            if (!triggers[y][x])
                                triggers[y][x] = []
                            triggers[y][x].add(scriptNames[script])
                        }
                    }
                }
            }

            // Now output code for the table. First comes the X
            // and Y origins.
            out << "word[] triggerTbl = ${xRange ? xRange[0] : 0}, ${yRange ? yRange[0] : 0} // origin X,Y\n"

            // Then the Y tables
            triggers.each { y, xs ->
                def size = 2  // 2 bytes for y+off
                xs.each { x, funcs ->
                    size += funcs.size() * 3  // plus 3 bytes per trigger (x, adrlo, adrhi)
                }
                out << "byte = $y, $size // Y=$y, size=$size\n"
                xs.each { x, funcs ->
                    funcs.each { func ->
                        out << "  byte = $x; word = @$func // X=$x\n"
                    }
                    // Record a list of trigger locations for the caller's reference
                    locationsWithTriggers << [x, y]
                }
            }
            out << "byte = \$FF\n\n"
        }

        def makeInit(mapName, script, maxX, maxY)
        {
            // Emit the code the user has stored for the init script (if any)
            if (script)
                packScript(script)

            // Code to register the  map name, trigger table, and map extent.
            def shortName = mapName.replaceAll(/[\s-]*[23][dD][-0-9]*$/, '').take(16)
            out << "setScriptInfo(\"$shortName\", @triggerTbl, $maxX, $maxY)\n"

            // Call init script if one was defined
            if (script)
                out << "sc_${humanNameToSymbol(getScriptName(script), false)}()\n"

            // All done with the init function.
            out << "done\n"
        }
    }

    class AnimBuf
    {
        def animFlags
        def buffers = []

        def addImage(animFrameNum, animFlags, imgBuf)
        {
            if (animFrameNum == 1)
                this.animFlags = animFlags
            buffers << imgBuf
            assert animFrameNum == buffers.size() : "Missing animation frame"
        }

        def pack()
        {
            def outBuf = ByteBuffer.allocate(50000) // plenty of room

            // If no animation, add a stub to the start of the (only) image and return it
            assert buffers.size() >= 1
            if (buffers.size() == 1) {
                outBuf.put((byte)0)
                outBuf.put((byte)0)
                buffers[0].flip()  // crazy stuff to append one buffer to another
                outBuf.put(buffers[0])
                return unwrapByteBuffer(outBuf)
            }

            // At start of buffer, put offset to animation header, then the first frame
            def offset = buffers[0].position() + 2  // 2 for the offset itself
            outBuf.put((byte)(offset & 0xFF))
            outBuf.put((byte)((offset >> 8) & 0xFF))
            buffers[0].flip()
            outBuf.put(buffers[0])

            // Now comes the full animation header
            def flagByte
            switch (animFlags) {
                case ""  : flagByte = 0; break
                case "f" : flagByte = 1; break
                case "fb": flagByte = 2; break
                case "s":  flagByte = 3; break
                case "r" : flagByte = 4; break
                default  : throw new Exception("Unrecognized animation flags '$animFlags'")
            }
            outBuf.put((byte)flagByte)
            outBuf.put((byte)1) // used to store current anim dir; start with 1=forward
            outBuf.put((byte)(buffers.size()))  // number of frames
            outBuf.put((byte)0) // used to store current anim frame

            // Then each patch
            buffers[1..-1].each { inBuf ->
                makePatch(inBuf, buffers[0], outBuf)
            }

            // All done.
            return unwrapByteBuffer(outBuf)
        }

        def makePatch(ByteBuffer inBuf, ByteBuffer refBuf, ByteBuffer outBuf)
        {
            int len = inBuf.position()
            assert refBuf.position() == len

            // Build up an array of patch hunks
            def patches = []
            (0..<len).each { pos ->
                if (refBuf.get(pos) != inBuf.get(pos))
                    addPatch(patches, pos)
            }

            // Write out size of the entire patch
            def startPos = outBuf.position()
            int totalSize = 3  // 2 for len hdr, 1 for end-of-patch
            patches.each { patch ->
                totalSize += (patch.end - patch.start) + 2
            }
            outBuf.put((byte)(totalSize & 0xFF))
            outBuf.put((byte)((totalSize>>8) & 0xFF))

            // And write out each patch hunk.
            int prev = 0
            patches.each { patch ->
                assert patch.start - prev <= 254 && patch.end - patch.start <= 254
                outBuf.put((byte)(patch.start - prev))
                outBuf.put((byte)(patch.end - patch.start))
                (patch.start ..< patch.end).each { pos ->
                    outBuf.put((byte)(inBuf.get(pos)))
                }
                prev = patch.end
            }
            outBuf.put((byte)0xFF)
            assert outBuf.position() - startPos == totalSize
        }


        def addPatch(patches, int pos)
        {
            // See if we can glom on to the previous patch
            def last = patches.isEmpty() ? [start:0, end:0] : patches[-1]
            if (last.end > 0 && pos < last.end + 3 && pos - last.start < 254) {
                last.end = pos+1
                return
            }

            // Skip to the right position
            while (pos - last.end >= 254) {
                last = [start:last.end+254, end:last.end+254]
                patches << last
            }

            // And add a new patch
            patches << [start:pos, end:pos+1]
        }
    }
}