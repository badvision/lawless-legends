/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
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
import net.jpountz.lz4.LZ4Factory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

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

    def mapNames  = [:]  // map name (and short name also) to map.2dor3d, map.num
    def sysCode   = [:]  // memory manager
    def code      = [:]  // code name to code.num, code.buf    
    def maps2D    = [:]  // map name to map.num, map.buf
    def maps3D    = [:]  // map name to map.num, map.buf
    def tiles     = [:]  // tile id to tile.buf
    def tileSets  = [:]  // tileset name to tileset.num, tileset.buf
    def avatars   = [:]  // avatar tile name to tile num (within the special tileset)
    def textures  = [:]  // img name to img.num, img.buf
    def frames    = [:]  // img name to img.num, img.buf
    def portraits = [:]  // img name to img.num, img.buf
    def fonts     = [:]  // font name to font.num, font.buf
    def modules   = [:]  // module name to module.num, module.buf
    def bytecodes = [:]  // module name to bytecode.num, bytecode.buf
    def fixups    = [:]  // module name to fixup.num, fixup.buf
    
    def itemNameToFunc = [:]
    
    def lastSysModule
    
    def compressor = LZ4Factory.fastestInstance().highCompressor()
    
    def ADD_COMP_CHECKSUMS = false
    
    def debugCompression = false
    
    def currentContext = []    
    def nWarnings = 0
    def warningBuf = new StringBuilder()
    
    def binaryStubsOnly = false
    def cache = [:]
    def buildDir
    
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
        def msg = String.format("Warning in ${getContextStr()}: %s\n", str)
        System.out.print(msg)
        warningBuf.append(msg)
        ++nWarnings
    }
    
    def escapeString(inStr)
    {
        def buf = new StringBuilder()
        buf << '\"'
        def prev = '\0'
        def count = 0
        def stop = false
        inStr.eachWithIndex { ch, idx ->
            if (!stop) {
                if (count >= 255) {
                    printWarning("String must be 254 characters or less. Everything after the following will be discarded: '${inStr[0..idx]}'")
                    stop = true
                }
                else if (ch == '^') {
                    if (prev == '^')
                        buf << ch
                }
                else if (ch == '\"') {
                    buf << "\\\""
                    ++count  // account for extra backslash
                }
                else if (prev == '^') {
                    def cp = Character.codePointAt(ch.toUpperCase(), 0)
                    if (cp > 64 && cp < 96)
                        buf << "\\\$" << String.format("%02X", cp - 64)
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

    def parseMap(map, tiles)
    {
        // Parse each row of the map
        map.chunk.row.collect
        {
            // The map data is a space-separated string of tile ids. Look up those
            // tiles.
            //
            it.text().split(" ").collect { tileId ->
                (tileId == "_") ? null : tiles.find{ it.@id == tileId }
            }
        }
    }
    
    def calcImageHash(imgEl)
    {
        def data = imgEl.displayData?.find { it.@platform == "AppleII" }
        assert data : "image '${imgEl.@name}' missing AppleII platform data"
        byte[] bytes = MessageDigest.getInstance("MD5").digest(data.toString().getBytes())
        return DatatypeConverter.printHexBinary(bytes)
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

        // Parse out the hex data on each line and add it to a buffer.
        def hexStr = dataEl.text()
        def arr = new byte[8192]
        def srcPos = 0
        def dstPos = 0
        
        // Process each line
        (0..<192).each { y ->
            
            // Process all 40 bytes in one line
            (0..<40).each { x ->
                arr[dstPos+x] = Integer.parseInt(hexStr[srcPos..srcPos+1], 16)
                srcPos += 2
            }
            
            // Crazy adjustment to get to next line on Apple II hi-res screen
            dstPos += 0x400
            if (dstPos >= 0x2000) {
                dstPos -= 0x2000
                dstPos += 0x80
                if (dstPos >= 0x400) {
                    dstPos -= 0x400
                    dstPos += 40
                }
            }
        }
        
        // Put the results into the buffer
        def outBuf = ByteBuffer.allocate(8192)
        outBuf.put(arr)
        
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
        def outBuf = ByteBuffer.allocate(128*18 + 1)
        outBuf.put((byte)1) // to start with: 1 frame, no flags
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
        
        return [width, height]
    }
    
    def write2DMap(mapName, mapEl, rows)
    {
        def width, height
        (width, height) = calcMapExtent(rows)
        
        def TILES_PER_ROW = 22
        def ROWS_PER_SECTION = 23
        
        def nHorzSections = (int) ((width + TILES_PER_ROW - 1) / TILES_PER_ROW)
        def nVertSections = (int) ((height + ROWS_PER_SECTION - 1) / ROWS_PER_SECTION)
        
        def buffers = new ByteBuffer[nVertSections][nHorzSections]
        def sectionNums = new int[nVertSections][nHorzSections]
                
        // Allocate a buffer and assign a map number to each section.
        (0..<nVertSections).each { vsect ->
            (0..<nHorzSections).each { hsect ->
                def buf = ByteBuffer.allocate(512)
                buffers[vsect][hsect] = buf
                def num = maps2D.size() + 1
                sectionNums[vsect][hsect] = num
                def sectName = "$mapName-$hsect-$vsect"
                maps2D[sectName] = [num:num, buf:buf]
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
    }
   
    def write3DMap(buf, mapName, rows, scriptModule, locationsWithTriggers) // [ref BigBlue1_50]
    {
        def width = rows[0].size() + 2  // Sentinel $FF at start and end of each row
        def height = rows.size() + 2    // Sentinel rows of $FF's at start and end
        
        // Determine the set of all referenced textures, and assign numbers to them.
        def texMap = [:]
        def texList = []
        def texFlags = []
        rows.each { row ->
            row.each { tile ->
                def id = tile?.@id
                def name = tile?.@name
                if (!texMap.containsKey(id)) {
                    if (name == null || name.toLowerCase() =~ /street|blank|null/)
                        texMap[id] = 0
                    else if (stripName(name) in textures) {
                        def flags = 1
                        if (tile?.@obstruction == 'true')
                            flags |= 2
                        if (tile?.@blocker == 'true')
                            flags |= 4
                        texList.add(textures[stripName(name)].num)
                        texFlags.add(flags)
                        texMap[id] = texList.size()
                        if (tile?.@sprite == 'true')
                            texMap[id] |= 0x80; // hi-bit flag to mark sprite cells
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
            addTo[name] = [num:num, buf:wrapByteArray(cache[key].data)]
            return true
        }
        return false
    }
    
    def addToCache(kind, addTo, name, hash, buf)
    {
        def num = addTo.size() + 1
        addTo[name] = [num:num, buf:buf]
        
        def uncompressedLen = buf.position()
        def uncompressedData = new byte[uncompressedLen]
        buf.position(0)
        buf.get(uncompressedData)

        def key = kind + ":" + name
        cache[key] = [hash:hash, data:uncompressedData]
    }
    
    def grabEntireFromCache(kind, addTo, hash)
    {
        if (cache.containsKey(kind) && cache[kind].hash == hash) {
            cache[kind].ents.each { ent ->
                def num = addTo.size() + 1
                addTo[ent.name] = [num: ent.num, buf: wrapByteArray(ent.data)]
            }
            return true
        }
        return false
    }
    
    def addEntireToCache(kind, addTo, hash)
    {
        def ents = []
        addTo.each { name, ent ->
            def buf = ent.buf
            def uncompressedLen = buf.position()
            def uncompressedData = new byte[uncompressedLen]
            buf.position(0)
            buf.get(uncompressedData)
            ents << [name:name, num:ent.num, data:uncompressedData]
        }
        cache[kind] = [hash:hash, ents:ents]
    }
    
    def packTexture(imgEl)
    {
        def name = stripName(imgEl.@name)
        def hash = calcImageHash(imgEl)
        if (!grabFromCache("texture", textures, name, hash)) {
            def pixels = parseTexture(imgEl)
            calcTransparency(pixels)
            def buf = ByteBuffer.allocate(50000)
            writeTexture(buf, pixels)
            addToCache("texture", textures, name, hash, buf)
        }
    }
    
    def packFrameImage(imgEl)
    {
        def name = imgEl.@name ?: "img$num"
        def hash = calcImageHash(imgEl)
        if (!grabFromCache("frame", frames, name, hash))
            addToCache("frame", frames, name, hash, parseFrameData(imgEl))
    }
    
    def packPortrait(imgEl)
    {
        def num = portraits.size() + 1
        def name = imgEl.@name ?: "img$num"
        def animFrameNum = 0
        def animFlags
        def m = (name =~ /^(.*)\*(\d+)(\w*)$/)
        if (m) {
            name = m[0][1]
            animFrameNum = m[0][2].toInteger()
            animFlags = m[0][3].toLowerCase()
        }
        //println "Packing 126 image named '$name'."
        def buf = parse126Data(imgEl)
        if (animFrameNum == 1) {
            def flagByte
            switch (animFlags) {
                case ""  : flagByte = 0; break
                case "f" : flagByte = 0x20; break
                case "fb": flagByte = 0x40; break
                case "r" : flagByte = 0x80; break
                default  : throw new Exception("Unrecognized animation flags '$animFlags'")
            }
            def newBuf = ByteBuffer.allocate(50000) // plenty of room
            buf.flip()  // crazy stuff to append one buffer to another
            newBuf.put(buf)
            def endPos = newBuf.position()
            newBuf.position(0)
            newBuf.put((byte) (1 + flagByte))
            newBuf.position(endPos)
            portraits[name] = [num:num, buf:newBuf]
        }
        else if (animFrameNum > 1) {
            if (!portraits[name])
                throw new Exception("Can't find first frame for animation '$name'")
            num = portraits.size()  // in other words, do not increment
            buf.flip()  // crazy stuff to append one buffer to another
            buf.get() // skip 1st byte - unused flags
            def out = portraits[name].buf
            out.put(buf)
            
            // Increment the frame count
            def endPos = out.position()            
            out.position(0)
            def before = out.get()
            out.position(0)
            out.put((byte)(before+1))
            out.position(endPos)
        }
        else
            portraits[name] = [num:num, buf:buf]
        //println "...uncompressed: ${buf.position()} bytes."
    }
    
    def packTile(imgEl)
    {
        def buf = parseTileData(imgEl)
        tiles[imgEl.@id] = buf
    }

    /** Identify the avatars and number them */
    def numberAvatars(dataIn)
    {
        def nFound = 0
        dataIn.tile.sort{it.@name.toLowerCase()}.each { tile ->
            def name = tile.@name
            if (name.toLowerCase().contains("avatar"))
                avatars[name.toLowerCase().trim().replaceAll(/\s*-\s*[23][dD]\s*/, "")] = nFound++
        }
        assert nFound >= 1 : "Need at least one 'Avatar' tile."
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
        dataIn.tile.sort{it.@name.toLowerCase()}.each { tile ->
            def name = tile.@name
            def id = tile.@id
            def data = tiles[id]
            if (name.toLowerCase().contains("avatar")) {
                def num = tileMap.size()
                avatars[name.toLowerCase().trim().replaceAll(/\s*-\s*[23][dD]\s*/, "")] = num
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
                if (together.size() <= 64 && inCommon.size() > bestCommon) {
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
        //println "Packing 2D map #$num named '$name'."
        withContext("map '$name'") {
            def rows = parseMap(mapEl, tileEls)
            write2DMap(name, mapEl, rows)
        }
    }
    
    def pack3DMap(mapEl, tileEls)
    {
        def name = mapEl.@name ?: "map$num"
        def num = mapNames[name][1]
        //println "Packing 3D map #$num named '$name'."
        withContext("map '$name'") {
            def rows = parseMap(mapEl, tileEls)
            def (scriptModule, locationsWithTriggers) = packScripts(mapEl, name, rows[0].size(), rows.size())
            def buf = ByteBuffer.allocate(50000)
            write3DMap(buf, name, rows, scriptModule, locationsWithTriggers)
            maps3D[name] = [num:num, buf:buf]
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
        def inBuf = new byte[256]
        def outBuf = ByteBuffer.allocate(50000)
        if (binaryStubsOnly)
            return outBuf
        new File(path).withInputStream { stream ->
            while (true) {
                def got = stream.read(inBuf)
                if (got < 0) break
                outBuf.put(inBuf, 0, got)
            }
        }
        return outBuf
    }
    
    def readCode(name, path)
    {
        def num = code.size() + 1
        //println "Reading code #$num from '$path'."
        code[name] = [num:num, buf:readBinary(path)]
    }
    
    def readModule(name, path)
    {
        def num = modules.size() + 1
        //println "Reading module #$num from '$path'."
        def bufObj = readBinary(path)
        if (binaryStubsOnly) {
            modules[name] = [num:num, buf:bufObj]
            return
        }
        
        def bufLen = bufObj.position()
        def buf = new byte[bufLen]
        bufObj.position(0)
        bufObj.get(buf)
        
        // Look for the magic header 0xDA7E =~ "DAVE"
        assert (buf[3] & 0xFF) == 0xDA
        assert (buf[2] & 0xFF) == 0x7E
        
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
            esdIndex = fixup[sp++]
            
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
        
        return [wrapByteArray(newAsmCode), wrapByteList(byteCode), wrapByteList(newFixup)]
    }
    
    def wrapByteArray(array) {
        def buf = ByteBuffer.wrap(array)
        buf.position(array.length)
        return buf
    }
    
    def wrapByteList(list) {
        def arr = new byte[list.size]
        list.eachWithIndex { n, idx -> arr[idx] = (byte)n }
        return wrapByteArray(arr)
    }
    
    def readFont(name, path)
    {
        def num = fonts.size() + 1
        //println "Reading font #$num from '$path'."
        fonts[name] = [num:num, buf:readBinary(path)]
    }
    
    // Transform the LZ4 format to something we call "LZ4M", where the small offsets are stored
    // as one byte instead of two. In our data, that's about 1/3 of the offsets.
    //
    def recompress(data, inLen, uncompData, uncompLen)
    {
        def outLen = 0
        def sp = 0
        def dp = 0
        def cksum = 0
        while (true) 
        {
            assert dp <= sp
            
            // First comes the token: 4 bits literal len, 4 bits match len
            def token = data[dp++] = (data[sp++] & 0xFF)
            def matchLen = token & 0xF
            def literalLen = token >> 4
            
            // The literal length might get extended
            if (literalLen == 15) {
                while (true) {
                    token = data[dp++] = (data[sp++] & 0xFF)
                    literalLen += token
                    if (token != 0xFF)
                        break
                }
            }
            
            if (debugCompression)
                println String.format("Literal: ptr=\$%x, len=\$%x.", sp, literalLen)
            
            // Copy the literal bytes
            outLen += literalLen
            for ( ; literalLen > 0; --literalLen) {
                cksum ^= data[sp]
                data[dp++] = data[sp++]
            }
            
            // The last block has only literals, and no match
            if (sp == inLen)
                break
            
            // Grab the offset
            token = data[sp++] & 0xFF
            def offset = token | ((data[sp++] & 0xFF) << 8)
            
            // Re-encode the offset using 1 byte if possible
            assert offset < 32768
            if (offset < 128)
                data[dp++] = offset
            else {
                data[dp++] = (offset & 0x7F) | 0x80
                data[dp++] = (offset >> 7) & 0xFF
            }
            
            // If checksums are enabled, output the checksum so far
            if (offset < 128 && ADD_COMP_CHECKSUMS) {
                if (debugCompression)
                    println String.format("    [chksum=\$%x]", cksum & 0xFF)
                data[dp++] = (byte) cksum
            }
            
            // The match length might get extended
            if (matchLen == 15) {
                while (true) {
                    token = data[dp++] = (data[sp++] & 0xFF)
                    matchLen += token
                    if (token != 0xFF)
                        break
                }
            }
            
            matchLen += 4   // min match length is 4
            
            if (debugCompression)
                println String.format("Match: offset=\$%x, len=\$%x.", offset, matchLen)
            
            // We do nothing with the match bytes except add them to the checksum
            (0..<matchLen).each {
                cksum ^= uncompData[outLen]
                ++outLen
            }
        }
        
        // If checksums are enabled, output the final checksum
        if (ADD_COMP_CHECKSUMS) {
            if (debugCompression)
                println String.format("Final cksum: \$%x", cksum & 0xFF)
            data[dp++] = (byte) cksum
        }

        assert outLen == uncompLen
        return dp
    }
    
    def compressionSavings = 0
    
    def compress(buf)
    {
        // First, grab the uncompressed data into a byte array
        def uncompressedLen = buf.position()
        def uncompressedData = new byte[uncompressedLen]
        buf.position(0)
        buf.get(uncompressedData)
        
        // Now compress it with LZ4
        assert uncompressedLen < 327678 : "data block too big"
        assert uncompressedLen > 0
        def maxCompressedLen = compressor.maxCompressedLength(uncompressedLen)
        def compressedData = new byte[maxCompressedLen]
        def compressedLen = compressor.compress(uncompressedData, 0, uncompressedLen, 
                                                compressedData, 0, maxCompressedLen)
        assert compressedLen > 0
        
        // Then recompress to LZ4M (pretty much always smaller)
        def recompressedLen = recompress(compressedData, compressedLen, uncompressedData, uncompressedLen)
        
        // If we saved at least 20 bytes, take the compressed version.
        if ((uncompressedLen - recompressedLen) >= 20) {
            if (debugCompression)
                println String.format("  Compress. rawLen=\$%x compLen=\$%x", uncompressedLen, recompressedLen)
            compressionSavings += (uncompressedLen - recompressedLen) - 2 - (ADD_COMP_CHECKSUMS ? 1 : 0)
            return [data:compressedData, len:recompressedLen, 
                    compressed:true, uncompressedLen:uncompressedLen]
        }
        else {
            if (debugCompression)
                println String.format("  No compress. rawLen=\$%x compLen=\$%x", uncompressedLen, recompressedLen)
            return [data:uncompressedData, len:uncompressedLen, compressed:false]
        }
    }
    
    def writePartition(stream, partNum)
    {
        // Make a list of all the chunks that will be in the partition
        def chunks = []
        if (partNum == 1)
            code.values().each { chunks.add([type:TYPE_CODE, num:it.num, buf:compress(it.buf)]) }       
        modules.each { k, v ->
            if ((partNum==1 && v.num <= lastSysModule) || (partNum==2 && v.num > lastSysModule)) {
                chunks.add([type:TYPE_MODULE, num:v.num, buf:compress(v.buf)])
                chunks.add([type:TYPE_BYTECODE, num:v.num, buf:compress(bytecodes[k].buf)])
                chunks.add([type:TYPE_FIXUP, num:v.num, buf:compress(fixups[k].buf)])
            }
        }
        if (partNum == 1) {
            fonts.values().each { chunks.add([type:TYPE_FONT, num:it.num, buf:compress(it.buf)]) }
            frames.values().each { chunks.add([type:TYPE_SCREEN, num:it.num, buf:compress(it.buf)]) }
        }
        else {
            maps2D.values().each { chunks.add([type:TYPE_2D_MAP, num:it.num, buf:compress(it.buf)]) }
            tileSets.values().each { chunks.add([type:TYPE_TILE_SET, num:it.num, buf:compress(it.buf)]) }
            maps3D.values().each { chunks.add([type:TYPE_3D_MAP, num:it.num, buf:compress(it.buf)]) }
            textures.values().each { chunks.add([type:TYPE_TEXTURE_IMG, num:it.num, buf:compress(it.buf)]) }
            portraits.values().each { chunks.add([type:TYPE_PORTRAIT, num:it.num, buf:compress(it.buf)]) }
        }
        
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
                def clen = chunk.buf.uncompressedLen;
                hdrBuf.put((byte)(clen & 0xFF))
                hdrBuf.put((byte)(clen >> 8))
            }
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
        def hdrData = new byte[hdrEnd]
        hdrBuf.position(0)
        hdrBuf.get(hdrData)
        
        // Finally, write out each chunk's data, including the header.
        stream.write(hdrData)
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
        if (result != 0) {
            def errStr = errBuf.toString("UTF-8")
            if (errStr.length() > 0)
                errStr = "\nError output:\n" + errStr + "-----\n"
            throw new Exception("$programName (cd $inDir && ${args.join(' ')}) < $inFile > $outFile failed with code $result." + errStr)
        }
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
                    if (m.group(1) =~ /.*gamelib.plh/ && !(codeFile =~ /.*gameloop.pla/)) {
                        println "Dep for $codeFile: ${m.group(1)} -> gameLoop.pla"
                        deps << jitCopy(new File(baseDir, m.group(1).replace("gamelib.plh", "gameloop.pla")))
                    }
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
            return addToCache("code", code, codeName, 1, ByteBuffer.allocate(1))
            
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
        addToCache("code", code, codeName, hash, readBinary(inDir + "build/" + codeName + ".b"))
    }
    
    def assembleCore(inDir)
    {
        if (binaryStubsOnly)
            return addToCache("sysCode", sysCode, "mem", 1, ByteBuffer.allocate(1))
        
        inDir = "build/" + inDir
        def hash = getLastDep(new File(inDir, "mem.s"))
        if (grabFromCache("sysCode", sysCode, "mem", hash))
            return
            
        println "Assembling mem.s"
        new File(inDir + "build").mkdir()
        String[] args = ["acme", "-o", "build/cmd.sys#2000", "mem.s"]
        runNestedvm(acme.Acme.class,  "ACME assembler", args, inDir, null, null)
        addToCache("sysCode", sysCode, "mem", hash, readBinary(inDir + "build/cmd.sys#2000"))
    }
    
    def compileModule(moduleName, codeDir, verbose = true)
    {
        if (binaryStubsOnly)
            return addToCache("modules", modules, moduleName, 1, ByteBuffer.allocate(1))
        
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
        String[] args = ["plasm", "-AM"]
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
    }

    def readAllCode()
    {
        assembleCore("src/core/")
        
        assembleCode("render", "src/raycast/")
        assembleCode("expand", "src/raycast/")
        assembleCode("fontEngine", "src/font/")
        assembleCode("tileEngine", "src/tile/")

        compileModule("gameloop", "src/plasma/")
        compileModule("combat", "src/plasma/")
        compileModule("party", "src/plasma/")
        compileModule("diskops", "src/plasma/")
        compileModule("gen_globalScripts", "src/plasma/")
        compileModule("gen_enemies", "src/plasma/")
        compileModule("gen_items", "src/plasma/")
        compileModule("gen_players", "src/plasma/")
        lastSysModule = modules.size()
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
                ++num2D
                mapNames[name] = ['2D', num2D]
                mapNames[shortName] = ['2D', num2D]
            }
            else if (map?.@name =~ /\s*3D$/) {
                ++num3D
                mapNames[name] = ['3D', num3D]
                mapNames[shortName] = ['3D', num3D]
            }
            else
                printWarning "map name '${map?.@name}' should contain '2D' or '3D'. Skipping."
        }
    }
    
    def readCache()
    {
        File cacheFile = new File("build/world.cache")
        if (cacheFile.exists()) {
            ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(cacheFile));
            cache = inStream.readObject();
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
    
    def pack(xmlPath)
    {
        // Save time by using cache of previous run
        readCache()
        
        // Read in code chunks. For now these are hard coded, but I guess they ought to
        // be configured in a config file somewhere...?
        //
        readAllCode()
        
        // We have only one font, for now at least.
        jitCopy(new File("build/data/fonts/font.bin"))
        readFont("font", "build/data/fonts/font.bin")
        
        // Open the XML data file produced by Outlaw Editor
        def dataIn = new XmlParser().parse(xmlPath)
        def xmlLastMod = xmlPath.lastModified()
        
        // Pre-pack the data for each tile
        println "Packing images."
        dataIn.tile.each { 
            packTile(it) 
        }

        // Pack the global tile set before other tile sets (contains the player avatar, etc.)
        numberAvatars(dataIn)
        packGlobalTileSet(dataIn)
        
        // Divvy up the images by category
        def titleImgs      = []
        def uiFrameImgs    = []
        def fullscreenImgs = []
        def textureImgs    = []
        def portraitImgs   = []
        
        dataIn.image.sort{it.@name.toLowerCase()}.each { image ->
            def category = image.@category?.toLowerCase()
            def name = image.@name.toLowerCase()
            if (category == "fullscreen" && name == "title")
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
        
        assert titleImgs.size() == 1 : "Couldn't find title image. Should be category='FULLSCREEN', name='title'"
        assert uiFrameImgs.size() == 2 : "Need exactly 2 UI frames, found ${uiFramesImgs.size()} instead."
        
        // Pack each image, which has the side-effect of filling in the image name map.
        titleImgs.each { image -> packFrameImage(image) }
        uiFrameImgs.each { image -> packFrameImage(image) }
        fullscreenImgs.each { image -> packFrameImage(image) }
        textureImgs.each { image -> packTexture(image) }
        if (!grabEntireFromCache("portraits", portraits, xmlLastMod)) {
            portraitImgs.each { image -> packPortrait(image) }
            addEntireToCache("portraits", portraits, xmlLastMod)
        }
        
        // Number all the maps and record them with names
        numberMaps(dataIn)
        
        // Form the translation from item name to function name
        allItemFuncs(dataIn.global.sheets.sheet)
            
        // Pack each map This uses the image and tile maps filled earlier.
        println "Packing maps."
        dataIn.map.each { map ->
            if (map?.@name =~ /2D/)
                pack2DMap(map, dataIn.tile) 
            else if (map?.@name =~ /3D/)
                pack3DMap(map, dataIn.tile) 
            else
                printWarning "map name '${map?.@name}' should contain '2D' or '3D'. Skipping."
        }
        
        // Ready to write the output file.
        println "Writing output file."
        new File("build/root").mkdir()
        
        def part1Path = new File("build/root/game.part.1.bin").path
        new File(part1Path).withOutputStream { stream -> writePartition(stream, 1) }

        def part2Path = new File("build/root/game.part.2.bin").path
        new File(part2Path).withOutputStream { stream -> writePartition(stream, 2) }
        
        // Print stats
        println "Compression saved $compressionSavings bytes."
        if (compressionSavings > 0) {
            def endSize = new File(part1Path).length() + new File(part2Path).length()
            def origSize = endSize + compressionSavings
            def savPct = String.format("%.1f", compressionSavings * 100.0 / origSize)
            println "Size $origSize -> $endSize ($savPct% savings)"
        }
        
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
        return buf.toString()
    }
    
    def parseDice(str)
    {
        // Handle single value
        if (str =~ /^\d+$/)
            return str
            
        // Otherwise parse things like "2d6+1"
        def m = (str =~ /^(\d+)[dD](\d+)([-+]\d+)?$/)
        if (!m)
            throw new Exception("Cannot parse dice string '$str'")
        def nDice = m[0][1].toInteger()
        def dieSize = m[0][2].toInteger()
        def add = m[0][3] ? m[0][3].toInteger() : 0
        return String.format("\$%X", ((nDice << 12) | (dieSize << 8) | add))
    }
    
    def genEnemy(out, row, portraitNames)
    {
        def name = row.@name
        withContext(name) 
        {
            out.println("def _NEn_${humanNameToSymbol(name, false)}()")

            def image1 = row.@image1
            if (!portraitNames.contains(humanNameToSymbol(image1, false)))
                throw new Exception("Image '$image1' not found")

            def image2 = row.@image2
            if (image2.size() > 0 && !portraitNames.contains(humanNameToSymbol(image2, false)))
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
            def goldLoot = row.@"gold-loot";        assert goldLoot

            out.println("  return makeEnemy(" +
                        "\"$name\", " +
                        "${parseDice(hitPoints)}, " +
                        "PO${humanNameToSymbol(image1, false)}, " +
                        (image2.size() > 0 ? "PO${humanNameToSymbol(image2, false)}, " : "0, ") +
                        "$attackTypeCode, " +
                        "\"$attackText\", " +
                        "${range.replace("'", "").toInteger()}, " +
                        "${chanceToHit.toInteger()}, " +
                        "${parseDice(damage)}, " +
                        "${parseDice(groupSize)}, " +
                        "${parseDice(goldLoot)})")
            out.println("end")
        }
    }
    
    def genAllEnemies(sheet, portraitNames)
    {
        assert sheet : "Missing 'enemies' sheet"
            
        withContext("enemies sheet")
        {
            new File("build/src/plasma/gen_enemies.plh.new").withWriter { out ->
                out.println("// Generated code - DO NOT MODIFY BY HAND\n")
                out.println("const enemy_forZone = 0")
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
                out.println("predef _enemy_forZone")
                out.println("word[] funcTbl = @_enemy_forZone\n")

                // Pre-define all the enemy creation functions
                sheet.rows.row.each { row ->
                    out.println("predef _NEn_${humanNameToSymbol(row.@name, false)}")
                }

                // Figure out the mapping between "map code" and "enemy", and output the table for that
                def codeToFunc = [:]
                sheet.rows.row.each { row ->
                    addCodeToFunc("_NEn_${humanNameToSymbol(row.@name, false)}", row.@"map-code", codeToFunc) }
                outCodeToFuncTbl("mapCode_", codeToFunc, out)

                // Helper function to fill in the Enemy data structure
                out.println("""
def makeEnemy(name, healthDice, image0, image1, attackType, attackText, attackRange, chanceToHit, dmg, groupSize, goldLoot)
  word p; p = mmgr(HEAP_ALLOC, TYPE_ENEMY)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p=>w_health = rollDice(healthDice) // 4d6
  if !image1 or (rand16() % 2)
    p->b_image = image0
  else
    p->b_image = image1
  fin
  p->b_attackType = attackType
  p=>s_attackText = mmgr(HEAP_INTERN, attackText)
  p->b_enemyAttackRange = attackRange
  p->b_chanceToHit = chanceToHit
  p=>r_enemyDmg = dmg
  p=>r_groupSize = groupSize
  p=>r_goldLoot = goldLoot
  return p
end
""")

                // Now output a function for each enemy
                sheet.rows.row.each { row ->
                    genEnemy(out, row, portraitNames)
                }
                out.println()

                // And finally, a function to select an enemy given a map code.
                outCodeToFuncMethod("_enemy_forZone", "mapCode_", codeToFunc, out)

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
            "${escapeString(parseStringAttr(row, "combat-text"))})")
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
    
    def genAmmo(func, row, out)
    {
        out.println(
            "  return makeStuff(" +
            "${escapeString(parseStringAttr(row, "name"))}, " +
            "${escapeString(parseStringAttr(row, "type"))}, " +
            "${parseWordAttr(row, "price")}, " +
            "${parseWordAttr(row, "max")})")
    }
    
    def genItem(func, row, out)
    {
        out.println(
            "  return makeItem(" +
            "${escapeString(parseStringAttr(row, "name"))}, " +
            "${parseWordAttr(row, "price")}, " +
            "${parseModifier(row, "bonus-value", "bonus-attribute")}, " +
            "${parseByteAttr(row, "numofuses")})")
    }
    
    def genPlayer(func, row, out)
    {
        out.println("  word p")
        out.println(\
            "  p = makePlayer_pt2(makePlayer_pt1(" +
            "${escapeString(parseStringAttr(row, "name"))}, " +
            "${parseByteAttr(row, "intelligence")}, " +
            "${parseByteAttr(row, "strength")}, " +
            "${parseByteAttr(row, "agility")}, " +
            "${parseByteAttr(row, "stamina")}, " +
            "${parseByteAttr(row, "charisma")}, " +
            "${parseByteAttr(row, "spirit")}, " +
            "${parseByteAttr(row, "luck")}), " +
            "${parseWordAttr(row, "health")}, " +
            "${parseByteAttr(row, "aiming")}, " +
            "${parseByteAttr(row, "hand-to-hand")}, " +
            "${parseByteAttr(row, "dodging")})")
        row.attributes().sort().each { name, val ->
            if (name =~ /^skill-(.*)/) {
                out.println("  addToList(@p=>p_skills, " +
                    "makeModifier(${escapeString(name.replace("skill-", ""))}, " +
                    "${parseByteAttr(row, name)}))")
            }
            else if (name =~ /^item-/) {
                def itemFunc = itemNameToFunc[val]
                assert itemFunc : "Can't locate item '$val'"
                out.println("  addToList(@p=>p_items, itemScripts()=>$itemFunc())")
            }
        }
        out.println("  calcPlayerArmor(p)")
        out.println "  return p"
    }
    
    def addCodeToFunc(funcName, codesString, addTo)
    {
        if (codesString == null || codesString.length() == 0)
            return
            
        codesString.replace("\"", "").split(",").collect{it.trim()}.grep{it!=""}.each { code ->
            code = code.toLowerCase()
            if (!addTo.containsKey(code))
                addTo[code] = []
            addTo[code] << funcName
        }
    }
    
    def outCodeToFuncTbl(prefix, codeToFunc, out)
    {
        codeToFunc.sort().each { code, funcs ->
            funcs.eachWithIndex { func, index ->
                out.println("${index==0 ? "word[] $prefix${humanNameToSymbol(code, false)} = " : "word         = "}@$func")
            }
            out.println()
        }
    }

    def outCodeToFuncMethod(funcName, prefix, codeToFunc, out)
    {
        out.println("def $funcName(code)")
        codeToFunc.sort().each { code, funcs ->
            out.println("  if streqi(code, \"$code\"); ")
            out.println("    return randomFromArray(@$prefix${humanNameToSymbol(code, false)}, ${funcs.size()})")
            out.println("  fin")
        }
        out.println("  puts(code)")
        out.println("  fatal(\"$funcName\")")
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

        // Global mapping of item name to function, so that Players can create items.
        funcs.each { typeName, func, index, row ->
            itemNameToFunc[row.@name.trim()] = func
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

        // Make constants for the function table
        new File("build/src/plasma/gen_items.plh.new").withWriter { out ->
            out.println("// Generated code - DO NOT MODIFY BY HAND\n")
            out.println("const item_forLootCode = 0")
            out.println("const item_forStoreCode = 2")
            funcs.each { typeName, func, index, row ->
                out.println("const ${func} = ${(index+2)*2}")
            }
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
            out.println("predef _item_forLootCode, _item_forStoreCode")
            funcs.each { typeName, func, index, row ->
                out.println("predef _$func")
            }
            out.println("")

            // Tables for converting loot codes and store codes to items
            outCodeToFuncTbl("lootCode_", lootCodeToFuncs, out)
            outCodeToFuncTbl("storeCode_", storeCodeToFuncs, out)
            
            // Next, output the function table
            out.println("word[] funcTbl = @_item_forLootCode, @_item_forStoreCode")
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
  p=>r_meleeDmg = meleeDmg
  p=>r_projectileDmg = projectileDmg
  return p
end

def makeWeapon_pt2(p, attack0, attack1, attack2, weaponRange, combatText)
  p->ba_attacks[0] = attack0
  p->ba_attacks[1] = attack1
  p->ba_attacks[2] = attack2
  p->b_weaponRange = weaponRange
  p=>s_combatText = mmgr(HEAP_INTERN, combatText)
  return p
end

def makeStuff(name, kind, price, count)
  word p; p = mmgr(HEAP_ALLOC, TYPE_STUFF)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p=>s_itemKind = mmgr(HEAP_INTERN, kind)
  p=>w_price = price
  p=>w_count = count
  p=>w_maxCount = count
  return p
end

def makeItem(name, price, modifier, maxUses)
  word p; p = mmgr(HEAP_ALLOC, TYPE_ITEM)
  p=>s_name = mmgr(HEAP_INTERN, name)
  p=>w_price = price
  p=>p_modifiers = modifier
  p->b_maxUses = maxUses
  p->b_curUses = 0
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
                        case "ammo":   genAmmo(func, row, out);   break
                        case "item":   genItem(func, row, out);   break
                        default: assert false
                    }
                    out.println("end\n")
                }
            }
            
            // Code for loot and store generation
            outCodeToFuncMethod("_item_forLootCode", "lootCode_", lootCodeToFuncs, out)
            outCodeToFuncMethod("_item_forStoreCode", "storeCode_", storeCodeToFuncs, out)

            // Lastly, the outer module-level code
            out.println("return @funcTbl")
            out.println("done")
        }
        replaceIfDiff("build/src/plasma/gen_items.pla")
    }

    def genAllPlayers(sheets)
    {
        // Grab all the raw data
        def funcs = []
        sheets.find { it?.@name.equalsIgnoreCase("players") }.rows.row.each { row ->
            funcs << ["NPl_${humanNameToSymbol(row.@name, false)}", funcs.size, row] 
        }
        
        // Make constants for the function table
        new File("build/src/plasma/gen_players.plh.new").withWriter { out ->
            out.println("// Generated code - DO NOT MODIFY BY HAND\n")
            out.println("const makeInitialParty = 0")
            funcs.each { func, index, row ->
                out.println("const ${func} = ${(index+1)*2}")
            }
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
            out.println("word global, itemScripts")
            out.println()

            // Pre-define all the creation functions
            out.println("predef _makeInitialParty")
            funcs.each { func, index, row ->
                out.println("predef _$func")
            }
            out.println("")

            // Next, output the function table
            out.println("word[] funcTbl = @_makeInitialParty")
            funcs.each { func, index, row ->
                out.println("word         = @_$func")
            }
            out.println("")

            // Data structure-filling helper
            out.print("""\n\
def makePlayer_pt1(name, intelligence, strength, agility, stamina, charisma, spirit, luck)
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

def makePlayer_pt2(p, health, aiming, handToHand, dodging)
  p=>w_health = health
  p=>w_maxHealth = health
  p->b_aiming = aiming
  p->b_handToHand = handToHand
  p->b_dodging = dodging
  return p
end
""")
            
            // Generate all the functions themselves
            funcs.each { func, index, row ->
                withContext("player '${row.@name}'") {
                    out.println("\ndef _$func()")
                    genPlayer(func, row, out)
                    out.println("end\n")
                }
            }
            
            // Code for initial party creation
            out.println("def _makeInitialParty()")
            out.println("  itemScripts = mmgr(QUEUE_LOAD, MODULE_GEN_ITEMS<<8 | RES_TYPE_MODULE)")
            out.println("  mmgr(FINISH_LOAD, 1) // 1 = keep open")
            funcs.each { func, index, row ->
                if (row.@"starting-party".equalsIgnoreCase("yes"))
                    out.println("  addToList(@global=>p_players, _$func())")
            }
            out.println("  mmgr(FREE_MEMORY, itemScripts)")
            out.println("  itemScripts = NULL")
            out.println("end\n")

            // Lastly, the outer module-level code
            out.println("global = getGlobals()")
            out.println("return @funcTbl")
            out.println("done")
        }
        replaceIfDiff("build/src/plasma/gen_players.pla")
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
    
    def dataGen(xmlPath)
    {
        // Open the XML data file produced by Outlaw Editor
        def dataIn = new XmlParser().parse(xmlPath)

        // When generating code, we need to use Unix linebreaks since that's what
        // the PLASMA compiler expects to see.
        def oldSep = System.getProperty("line.separator")
        System.setProperty("line.separator", "\n")
        
        // Translate image names to constants
        new File("build/src/plasma").mkdirs()
        def portraitNames = [] as Set
        new File("build/src/plasma/gen_images.plh.new").withWriter { out ->
            def portraitNum = 0
            dataIn.image.sort{it.@name.toLowerCase()}.each { image ->
                def category = image.@category?.toLowerCase()
                def name = image.@name
                if (category == "portrait") {
                    def animFrameNum = 0
                    def animFlags
                    def m = (name =~ /^(.*)\*(\d+)(\w*)$/)
                    if (m) {
                        name = m[0][1]
                        animFrameNum = m[0][2].toInteger()
                        animFlags = m[0][3].toLowerCase()
                    }
                    if (animFrameNum <= 1) {
                        ++portraitNum
                        out.println "const PO${humanNameToSymbol(name, false)} = $portraitNum"
                        portraitNames << humanNameToSymbol(name, false)
                    }
                }
            }
        }
        replaceIfDiff("build/src/plasma/gen_images.plh")
        
        // Before we can generate global script code, we need to identify and number
        // all the maps. Same with avatars.
        numberMaps(dataIn)
        numberAvatars(dataIn)
        
        // Translate global scripts to code
        def gsmod = new ScriptModule()
        gsmod.genScriptDefs(new File("build/src/plasma/gen_globalScripts.plh.new"), dataIn.global.scripts,
            ["newGame", "help"] as Set)
        replaceIfDiff("build/src/plasma/gen_globalScripts.plh")
        gsmod.packGlobalScripts(new File("build/src/plasma/gen_globalScripts.pla.new"), dataIn.global.scripts)
        replaceIfDiff("build/src/plasma/gen_globalScripts.pla")
        
        // Translate enemies, weapons, etc. to code
        genAllEnemies(dataIn.global.sheets.sheet.find { it?.@name.equalsIgnoreCase("enemies") }, portraitNames)
        genAllItems(dataIn.global.sheets.sheet)
        genAllPlayers(dataIn.global.sheets.sheet)

        // Produce a list of assembly and PLASMA code segments
        binaryStubsOnly = true
        readAllCode()
        binaryStubsOnly = false
        new File("build/src/plasma/gen_modules.plh.new").withWriter { out ->
            code.each { k, v ->
                out.println "const CODE_${humanNameToSymbol(k, true)} = ${v.num}"
            }
            modules.each { k, v ->
                out.println "const MODULE_${humanNameToSymbol(k, true)} = ${v.num}"
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
    
    def createImage()
    {
        // Copy the PLASMA VM file to the output directory
        copyIfNewer(jitCopy(new File("build/tools/PLASMA/src/PLVM02.SYSTEM.sys")), new File("build/root/PLVM02.SYSTEM.sys"))
        
        // Copy the memory manager to the output directory
        copyIfNewer(new File("build/src/core/build/cmd.sys#2000"), new File("build/root/cmd.sys#2000"))
        
        // If we preserved a previous save game, copy it to the new image.
        def prevSave = new File("build/prevGame/game.1.save.\$f1")
        if (prevSave.exists())
            copyIfNewer(prevSave, new File("build/root/game.1.save.\$f1"))
        
        // Decompress the base image.
        // No need to delete old file; that was done by outer-level code.
        def dst = new File("game.2mg")
        Files.copy(new GZIPInputStream(new FileInputStream(jitCopy(new File("build/data/disks/base.2mg.gz")))), dst.toPath())
        
        // Now put the files into the image
        String[] args = ["-put", "game.2mg", "/", "build/root"]
        new a2copy.A2Copy().main(args)
    }
    
    static void packWorld(String xmlPath, Object watcher)
    {
        // If there's an existing error file, remove it first, so user doesn't
        // get confused by an old file.
        def errorFile = new File("pack_error.txt")
        if (errorFile.exists())
            errorFile.delete()

        def warningFile = new File("pack_warning.txt")
        if (warningFile.exists())
            warningFile.delete()
            
        def inst
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

            // Create PLASMA headers
            inst = new A2PackPartitions()
            inst.buildDir = buildDir
            inst.dataGen(xmlFile)
            
            // Pack everything into a binary file
            inst = new A2PackPartitions() // make a new one without stubs
            inst.buildDir = buildDir
            inst.pack(xmlFile)

            // And create the final disk image
            inst.createImage()
        }
        catch (Throwable t) {
            errorFile.withWriter { out ->
                out.println "Packing error: ${t.message}"
                out.println "       detail: $t"
                if (inst) {
                    out.println "\nContext:"
                    out.println "    ${inst.getContextStr()}"
                }
                out.println "\nGroovy call stack:"
                t.getStackTrace().each {
                    if (it.toString().contains(".groovy:"))
                        out.println "    $it"
                }
            }
            errorFile.eachLine { println it }
            watcher.error(t.message, inst ? inst.getContextStr() : 'outer')
        }
        
        if (inst.nWarnings > 0) {
            warningFile.withWriter { out ->
                out.println "Packing warnings:\n"
                out.println inst.warningBuf.toString()
                out.write()
            }
            watcher.warnings(inst.nWarnings, inst.warningBuf.toString())
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
                "Details written to file 'pack_error.txt'.", "Fatal packing error", 
                javax.swing.JOptionPane.ERROR_MESSAGE)
            System.exit(1)
        }

        def warnings(nWarnings, str)
        {
            javax.swing.JOptionPane.showMessageDialog(null,
                "${nWarnings} warning(s) noted during packing.\nDetails written to file 'pack_warning.txt'.",
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
        def indent = 0
        def variables = [] as Set

        def getScriptName(script)
        {
            if (script.block.size() == 0)
                return null
                
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
            out << "include \"../plasma/gen_images.plh\"\n\n"
            out << "include \"../plasma/gen_items.plh\"\n\n"
            out << "word global\n"
        }
        
        /**
         * Generate header for a set of scripts. If any of the 'required' scripts are not
         * found, they'll get a special offset of -1.
         */
        def genScriptDefs(outFile, inScripts, required)
        {
            out = new PrintWriter(new FileWriter(outFile))
            out << "// Generated code - DO NOT MODIFY BY HAND\n\n"
            
            // Generate a name for each script, and a constant in the function table.
            def found = [] as Set
            inScripts.script.eachWithIndex { script, idx ->
                def name = getScriptName(script)
                assert name
                scriptNames[script] = "sc_${humanNameToSymbol(name, false)}"
                out << "const ${scriptNames[script]} = ${idx*2}\n"
                found.add(humanNameToSymbol(name, false))
            }
            (required - found).each { name ->
                out << "const sc_$name = -1 // not found\n"
            }
            out.close()
        }

        /**
         * Pack a set of scripts that are not associated with any particular map.
         */
        def packGlobalScripts(outFile, inScripts)
        {
            startScriptFile(outFile)
            
            // Pre-define each function
            inScripts.script.each { script ->
                out << "predef ${scriptNames[script]}\n"
            }
            out << "\n"
            
            // Make a table of all the functions
            inScripts.script.eachWithIndex { script, idx ->
                if (idx == 0)
                    out << "word[] funcTbl = @${scriptNames[script]}\n"
                else
                    out << "word           = @${scriptNames[script]}\n"
            }
            out << "\n"
                
            // Generate the actual script code
            inScripts.script.each { script ->
                packScript(script) 
            }
            
            // Set up the pointer to global vars and finish up the module.
            out << "global = getGlobals()\n"
            out << "return @funcTbl\n"
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
              
            // Even if there were no scripts, we still need an init to display
            // the map name.
            makeTriggerTbl(scripts, xRange, yRange)
            scripts.each { script ->
                packScript(script) 
            }
            makeInit(mapName, initScript, maxX, maxY)
            
            out.close()
        }

        def outIndented(str) {
            out << ("  " * indent) << str
        }

        def packScript(script)
        {
            //println "   Script '$name'"
            withContext(scriptNames[script]) 
            {
                if (script.block.size() == 0) {
                    printWarning("empty script '${script?.@name}' found; skipping.")
                    return
                }

                // Record the function's name and start its definition
                out << "def ${scriptNames[script]}()\n"
                indent = 1
                
                // Need to queue up the script, to find out what variables need
                // to be declared.
                def outerOutput = out
                def buf = new StringWriter()
                out = new PrintWriter(buf)
                variables = [] as Set

                // Process the code inside it
                def proc = script.block[0]
                assert proc.@type == "procedures_defreturn"
                if (proc.statement.size() > 0) {
                    assert proc.statement.size() == 1
                    def stmt = proc.statement[0]
                    assert stmt.@name == "STACK"
                    stmt.block.each { packBlock(it) }
                }
                else
                    printWarning "empty statement found; skipping."
                    
                // Define all the variables that were mentioned
                out.close()
                out = outerOutput
                variables.each { var ->
                    outIndented("word $var\n")
                }
                variables.each { var ->
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
                        packClrPortrait(blk); break
                    case 'graphics_set_avatar':
                        packSetAvatar(blk); break
                    case 'variables_set':
                        packVarSet(blk); break
                    case 'interaction_give_item':
                        packGiveItem(blk); break
                    case 'interaction_take_item':
                        packTakeItem(blk); break
                    case 'interaction_increase_stat':
                    case 'interaction_decrease_stat':
                        packChangeStat(blk); break
                    case 'interaction_set_flag':
                    case 'interaction_clr_flag':
                        packChangeFlag(blk); break
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
            def text = getSingle(getSingle(getSingle(blk.value, 'VALUE').block, null, 'text').field, 'TEXT').text()
            
            // Break up long strings into shorter chunks for PLASMA.
            // Note: this used to be 253, but still had some random mem overwrites.
            // Decreasing to chunks of 200 seems to fix it.
            def chunks = text.findAll(/.{200}|.*/).grep(~/.+/)
            chunks.eachWithIndex { chunk, idx ->
                outIndented((idx == chunks.size()-1 && blk.@type == 'text_println') ? \
                    'scriptDisplayStrNL(' : 'scriptDisplayStr(')
                out << escapeString(chunk) << ")\n"
                // Workaround for strings filling up the frame stack
                outIndented("tossStrings()\n")
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
            packExpr(getSingle(getSingle(blk.value).block))
            out << "\n"
        }
        
        def packGiveItem(blk)
        {
            def name = getSingle(blk.field, 'NAME').text().trim()
            def itemFunc = itemNameToFunc[name]
            assert itemFunc : "Can't locate item '$name'"
            outIndented("giveItemToPlayer($itemFunc)\n")
        }
        
        def packTakeItem(blk)
        {
            def name = getSingle(blk.field, 'NAME').text().trim()
            def itemFunc = itemNameToFunc[name]
            assert itemFunc : "Can't locate item '$name'"
            outIndented("takeItemFromPlayer(${escapeString(name)})\n")
        }

        def nameToStat(name) {
            switch (name.toLowerCase().trim()) {
                case "intelligence": return "@S_INTELLIGENCE"; return
                case "strength":     return "@S_STRENGTH";     return
                case "agility":      return "@S_AGILITY";      return
                case "stamina":      return "@S_STAMINA";      return
                case "spirit":       return "@S_SPIRIT";       return
                case "luck":         return "@S_LUCK";         return
                case "health":       return "@S_HEALTH";       return
                case "max health":   return "@S_MAX_HEALTH";   return
                case "aiming":       return "@S_AIMING";       return
                case "hand to hand": return "@S_HAND_TO_HAND"; return
                case "dodging":      return "@S_DODGING";      return
                case "gold":         return "@S_GOLD";         return
                default: assert false : "Unrecognized stat '$name'"
            }
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
            outIndented("setStat($stat, getStat($stat) ${blk.@type == 'interaction_increase_stat' ? '+' : '-'} $amount)\n")
        }

        def packGetStat(blk)
        {
            def name = getSingle(blk.field, 'NAME').text()
            def stat = nameToStat(name)
            out << "getStat($stat)"
        }

        def packGetFlag(blk)
        {
            def name = getSingle(blk.field, 'NAME').text()
            out << "getGameFlag(${escapeString(name)})"
        }

        def packChangeFlag(blk)
        {
            def name = getSingle(blk.field, 'NAME').text()
            outIndented("setGameFlag(${escapeString(name)}, ${blk.@type == 'interaction_set_flag' ? 1 : 0})\n")
        }

        def isStringExpr(blk)
        {
            return blk.@type == "text_getstring" || blk.@type == "text"
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
        
        def packVarGet(blk)
        {
            def name = "v_" + humanNameToSymbol(getSingle(blk.field, "VAR").text(), false)
            variables << name
            out << name
        }
        
        def packHasItem(blk)
        {
            def name = getSingle(blk.field, "NAME").text()
            out << "playerHasItem(${escapeString(name)})"
        }

        def packExpr(blk)
        {
            switch (blk.@type) {
                case 'math_number':
                    out << getSingle(blk.field, "NUM").text().toInteger()
                    break
                case 'text_getboolean':
                    out << "getYN()"
                    break
                case 'text_getstring':
                    out << "getStringResponse()"
                    break
                case 'logic_compare':
                    packLogicCompare(blk)
                    break
                case 'variables_get':
                    packVarGet(blk)
                    break
                case 'text':
                    out << escapeString(getSingle(blk.field, 'TEXT').text())
                    break
                case 'interaction_has_item':
                    packHasItem(blk)
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
            
            outIndented("return queue_setMap(${mapNum[0] == '2D' ? 0 : 1}, ${mapNum[1]}, $x, $y, $facing)\n")
        }
        
        def packSetPortrait(blk)
        {
            def portraitName = getSingle(blk.field, 'NAME').text()
            def portrait = portraits[portraitName]
            if (!portrait) {
                printWarning "portrait '$portraitName' not found; skipping set_portrait."
                return
            }
            outIndented("setPortrait(${portrait.num})\n")
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
        }

        def packClrEncounterZones(blk)
        {
            outIndented("clearEncounterZones()\n")
        }

        def packStartEncounter(blk)
        {
            assert blk.field.size() == 1
            def code = getSingle(blk.field, 'CODE')
            outIndented("if !scriptCombat(${escapeString(code)})); return; fin\n")
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
                out << "predef ${scriptNames[script]}\n"
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
            
            // Set up the pointer to global vars
            out << "global = getGlobals()\n"
            
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
    
}