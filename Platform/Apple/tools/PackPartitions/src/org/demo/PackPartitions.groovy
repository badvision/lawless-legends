/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.demo

import java.nio.ByteBuffer
import java.nio.channels.Channels
import net.jpountz.lz4.LZ4Factory

/**
 *
 * @author mhaye
 */
class PackPartitions 
{
    def TRANSPARENT_COLOR = 15
    
    def TYPE_CODE        = 1
    def TYPE_2D_MAP      = 2
    def TYPE_3D_MAP      = 3
    def TYPE_TILE_SET    = 4
    def TYPE_TEXTURE_IMG = 5
    def TYPE_FRAME_IMG   = 6
    def TYPE_FONT        = 7
    def TYPE_MODULE      = 8
    def TYPE_BYTECODE    = 9
    def TYPE_FIXUP       = 10

    def mapNames  = [:]  // map name (and short name also) to map.2dor3d, map.num
    def code      = [:]  // code name to code.num, code.buf    
    def maps2D    = [:]  // map name to map.num, map.buf
    def maps3D    = [:]  // map name to map.num, map.buf
    def tiles     = [:]  // tile id to tile.buf
    def tileSets  = [:]  // tileset name to tileset.num, tileset.buf
    def textures  = [:]  // img name to img.num, img.buf
    def frames    = [:]  // img name to img.num, img.buf
    def fonts     = [:]  // font name to font.num, font.buf
    def modules   = [:]  // module name to module.num, module.buf
    def bytecodes = [:]  // module name to bytecode.num, bytecode.buf
    def fixups    = [:]  // module name to fixup.num, fixup.buf
    
    def compressor = LZ4Factory.fastestInstance().highCompressor()
    
    def ADD_COMP_CHECKSUMS = false
    
    def debugCompression = false
    
    def javascriptOut = null
    
    def currentContext = []    
    def nWarnings = 0
    
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
    
    def printError(str)
    {
        System.out.format("Error in ${getContextStr()}: %s\n", str)
    }
    
    def printWarning(str)
    {
        System.out.format("Warning in ${getContextStr()}: %s\n", str)
        ++nWarnings
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
        def result = rows[0..63].collect { it[0..31] }
        
        // Kludge alert! strip high bits in Building6.
        if (imgEl.@name == "Forestville building 1 - 3d") {
            println "hacking high bits in texture 'Forestville building 1 - 3d'"
            def rowNum = 0
            result = result.collect { row ->
                rowNum++
                row.collect { pix ->
                    (rowNum <= 48 && pix >= 4) ? pix-4 :
                    (pix == 4) ? 0 :
                    (pix == 5) ? 1 :
                    pix
                }
            }
        }
        else if (imgEl.@name == "Block Tree - 3D") {
            println "hacking high bits in texture 'Block Tree - 3D'"
            def rowNum = 0
            result = result.collect { row ->
                rowNum++
                row.collect { pix ->
                    (pix >= 4) ? pix-4 :
                    pix
                }
            }
        }
        
        return result
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
        }
        
        // Put the results into the buffer
        def outBuf = ByteBuffer.allocate(128*18)
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

    def write2DMap(mapName, mapEl, rows)
    {
        def width = rows[0].size()
        def height = rows.size()

        def TILES_PER_ROW = 22
        def ROWS_PER_SECTION = 23
        
        def nHorzSections = (int) ((width + TILES_PER_ROW - 1) / TILES_PER_ROW)
        def nVertSections = (int) ((height + ROWS_PER_SECTION - 1) / ROWS_PER_SECTION)
        
        def buffers = new ByteBuffer[nVertSections][nHorzSections]
        def sectionNums = new int[nVertSections][nHorzSections]
                
        // Start the map index, which will list all the section numbers.
        def indexBuf = ByteBuffer.allocate(512)
        def indexNum = maps2D.size() + 1
        maps2D[mapName] = [num:indexNum, buf:indexBuf]
        indexBuf.put((byte)nHorzSections)
        indexBuf.put((byte)nVertSections)
        
        // Allocate a buffer and assign a map number to each section.
        (0..<nVertSections).each { vsect ->
            (0..<nHorzSections).each { hsect ->
                def buf = ByteBuffer.allocate(512)
                buffers[vsect][hsect] = buf
                def num = maps2D.size() + 1
                sectionNums[vsect][hsect] = num
                def sectName = "$mapName-$hsect-$vsect"
                maps2D[sectName] = [num:num, buf:buf]
                indexBuf.put((byte)num)
            }
        }
        
        // Finish the index buffer with the map name
        writeString(indexBuf, mapName.replaceFirst(/ ?-? ?2D/, ""))

        // Now create each map section
        (0..<nVertSections).each { vsect ->
            (0..<nHorzSections).each { hsect ->

                def hOff = hsect * TILES_PER_ROW
                def vOff = vsect * ROWS_PER_SECTION
                
                def (tileSetNum, tileMap) = packTileSet(rows, hOff, TILES_PER_ROW, vOff, ROWS_PER_SECTION)

                def xRange = hOff ..< hOff+TILES_PER_ROW
                def yRange = vOff ..< vOff+ROWS_PER_SECTION
                def sectName = "$mapName-$hsect-$vsect"
                def (scriptModule, locationsWithTriggers) = packScripts(mapEl, sectName, xRange, yRange)
                
                // Header: first come links to other map sections
                def buf = buffers[vsect][hsect]
                buf.put((byte) (vsect > 0) ? sectionNums[vsect-1][hsect] : 0xFF)               // north
                buf.put((byte) (hsect < nHorzSections-1) ? sectionNums[vsect][hsect+1] : 0xFF) // east
                buf.put((byte) (vsect < nVertSections-1) ? sectionNums[vsect+1][hsect] : 0xFF) // south
                buf.put((byte) (hsect > 0) ? sectionNums[vsect][hsect-1] : 0xFF)               // west
                
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
                        def flags = ([colNum, rowNum] in locationsWithTriggers) ? 0x20 : 0
                        buf.put((byte)((tile ? tileMap[tile.@id] : 0) | flags))
                    }
                }
            }
        }
    }
   
    /**
     * Dump map data to Javascript code, to help in debugging the raycaster. This way,
     * the Javascript version can run the same map, and we can compare its results to
     * the 6502 results.
     */
    def dumpJsMap(rows, texMap)
    {
        def width = rows[0].size()+2
        
        // Write the map data. First comes the sentinel row.
        javascriptOut.println("var map = [")
        javascriptOut.print("  [")
        (0..<width).each { javascriptOut.print("-1,") }
        javascriptOut.println("],")
        
        // Now the real map data
        rows.each { row ->
            javascriptOut.print("  [-1,")
            row.each { tile ->
                def b = texMap[tile?.@id]
                if ((b & 0x80) == 0)
                    javascriptOut.format("%2d,", b)
                else
                    javascriptOut.print(" 0,")
            }
            javascriptOut.println("-1,],")
        }
        
        // Finish the map data with another sentinel row
        javascriptOut.print("  [")
        (0..<width).each { javascriptOut.print("-1,") }
        javascriptOut.println("]")
        javascriptOut.println("];\n")
        
        // Then write out the sprites
        javascriptOut.println("var allSprites = [")
        rows.eachWithIndex { row, y ->
            row.eachWithIndex { tile, x ->
                def b = texMap[tile?.@id]
                if ((b & 0x80) != 0) {
                    // y+1 below to account for initial sentinel row
                    javascriptOut.format("  {type:%2d, x:%2d.5, y:%2d.5},\n", b & 0x7f, x, y+1)
                }
            }
        }
        javascriptOut.println("];\n")
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
        
        // Followed by name
        writeString(buf, mapName.replaceFirst(/ ?-? ?3D/, ""))
        
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
                buf.put((byte)texMap[tile?.@id] | flags)
            }
            buf.put((byte)0xFF) // sentinel at end of row
        }

        // Sentinel row of $FF at end of map
        (0..<width).each { buf.put((byte)0xFF) }
        
        if (javascriptOut)
            dumpJsMap(rows, texMap)
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
    
    def packTexture(imgEl)
    {
        def num = textures.size() + 1
        def name = imgEl.@name ?: "img$num"
        //println "Packing texture #$num named '${imgEl.@name}'."
        def pixels = parseTexture(imgEl)
        calcTransparency(pixels)
        def buf = ByteBuffer.allocate(50000)
        writeTexture(buf, pixels)
        textures[stripName(imgEl.@name)] = [num:num, buf:buf]
    }
    
    def packFrameImage(imgEl)
    {
        def num = frames.size() + 1
        def name = imgEl.@name ?: "img$num"
        //println "Packing frame image #$num named '${imgEl.@name}'."
        def buf = parseFrameData(imgEl)
        frames[imgEl.@name] = [num:num, buf:buf]
        return buf
    }
    
    def pack126(imgEl)
    {
        println "Packing 126 image named '${imgEl.@name}'."
        def buf = parse126Data(imgEl)
        buf = compress(buf)
        println "...compressed: ${buf.len} bytes."
    }
    
    def packTile(imgEl)
    {
        def buf = parseTileData(imgEl)
        tiles[imgEl.@id] = buf
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
        def nFound = 0
        dataIn.tile.each { tile ->
            def name = tile.@name
            def id = tile.@id
            def data = tiles[id]
            if (name.equalsIgnoreCase("Avatars1 - 2D")) {
                def num = tileMap.size()
                tileIds.add(id)
                tileMap[id] = num
                data.flip() // crazy stuff to append one buffer to another
                buf.put(data)
                nFound += 1
            }
        }
        assert nFound == 1
        
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
                if (together.size() <= 32 && inCommon.size() > bestCommon) {
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
                    assert num < 32 : "Error: Only 31 kinds of tiles are allowed on any given map."
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
        def rows = parseMap(mapEl, tileEls)
        write2DMap(name, mapEl, rows)
    }
    
    def pack3DMap(mapEl, tileEls)
    {
        def name = mapEl.@name ?: "map$num"
        def num = mapNames[name][1]
        //println "Packing 3D map #$num named '$name'."
        withContext("map '$name'") {
            def (scriptModule, locationsWithTriggers) = packScripts(mapEl, name)
            def rows = parseMap(mapEl, tileEls)
            def buf = ByteBuffer.allocate(50000)
            write3DMap(buf, name, rows, scriptModule, locationsWithTriggers)
            maps3D[name] = [num:num, buf:buf]
        }
    }
    
    def packScripts(mapEl, mapName, xRange = null, yRange = null)
    {
        if (!mapEl.scripts)
            return [0, [] as Set]
            
        def num = modules.size() + 1
        def name = "mapScript$num"
        //println "Packing scripts for map $mapName, to module $num."
        
        ScriptModule module = new ScriptModule()
        if (!module.packScripts(mapEl.scripts[0], xRange, yRange)) {
            //println "...no scripts applied; will re-use module num"
            return [0, [] as Set]
        }
            
        modules[name]   = [num:num, buf:wrapByteList(module.data)]
        bytecodes[name] = [num:num, buf:wrapByteList(module.bytecode)]
        fixups[name]    = [num:num, buf:wrapByteList(module.fixups)]
        return [num, module.locationsWithTriggers]
    }
    
    def readBinary(path)
    {
        def inBuf = new byte[256]
        def outBuf = ByteBuffer.allocate(50000)
        def stream = new File(path).withInputStream { stream ->
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
        assert asmCodeStart >= 0 && asmCodeStart < byteCodeStart
        assert byteCodeStart >= asmCodeStart && byteCodeStart < fixupStart
        assert initStart == 0 || (initStart+2 >= byteCodeStart && initStart+2 < fixupStart)
        assert fixupStart < buf.length
        
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
        
        // Translate offsets in all the fixups
        def newFixup = []
        dp = 0
        while (fixup[sp] != 0) {
            assert (fixup[sp++] & 0xFF) == 0x81 // We can only handle WORD sized INTERN fixups
            def addr = fixup[sp++] & 0xFF
            addr |= (fixup[sp++] & 0xFF) << 8
            
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
            
            def target = (codeBuf[addr] & 0xFF) | ((codeBuf[addr+1] & 0xFF) << 8)
            //println String.format("...target=0x%04x", target)
            
            if (invDefs.containsKey(target)) {
                target = invDefs[target]
                //println String.format("...translated to def offset 0x%04x", target)
            }
            else {
                target -= 0x1000
                target -= asmCodeStart
                target += stubsSize   // account for the stubs we prepended to the asm code
                //println String.format("...adjusted to target offset 0x%04x", target)
            }
            assert target >= 5 && target < newAsmCode.length
            
            // Put the adjusted target back in the code
            codeBuf[addr] = (byte)(target & 0xFF)
            codeBuf[addr+1] = (byte)((target >> 8) & 0xFF)
            
            // And record the fixup
            newFixup.add((byte)((addr>>8) & 0xFF) | (inByteCode ? 0x80 : 0))
            newFixup.add((byte)(addr & 0xFF))
            assert fixup[sp++] == 0  // not sure what the zero byte is
        }
        newFixup.add((byte)0xFF)
        
        modules[name] = [num:num, buf:wrapByteArray(newAsmCode)]
        bytecodes[name] = [num:num, buf:wrapByteList(byteCode)]
        fixups[name] = [num:num, buf:wrapByteList(newFixup)]
    }
    
    def wrapByteArray(array) {
        def buf = ByteBuffer.wrap(array)
        buf.position(array.length)
        return buf
    }
    
    def wrapByteList(list) {
        return wrapByteArray(list.toArray(new byte[list.size]))
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
                println String.format("Literal: ptr=\$%x, len=\$%x.", (0x4400+sp), literalLen)
            
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
    
    def writePartition(stream)
    {
        // Make a list of all the chunks that will be in the partition
        def chunks = []
        code.values().each { chunks.add([type:TYPE_CODE, num:it.num, buf:compress(it.buf)]) }
        modules.each { k, v ->
            chunks.add([type:TYPE_MODULE, num:v.num, buf:compress(v.buf)])
            chunks.add([type:TYPE_BYTECODE, num:v.num, buf:compress(bytecodes[k].buf)])
            chunks.add([type:TYPE_FIXUP, num:v.num, buf:compress(fixups[k].buf)])
        }
        fonts.values().each { chunks.add([type:TYPE_FONT, num:it.num, buf:compress(it.buf)]) }
        frames.values().each { chunks.add([type:TYPE_FRAME_IMG, num:it.num, buf:compress(it.buf)]) }
        maps2D.values().each { chunks.add([type:TYPE_2D_MAP, num:it.num, buf:compress(it.buf)]) }
        tileSets.values().each { chunks.add([type:TYPE_TILE_SET, num:it.num, buf:compress(it.buf)]) }
        maps3D.values().each { chunks.add([type:TYPE_3D_MAP, num:it.num, buf:compress(it.buf)]) }
        textures.values().each { chunks.add([type:TYPE_TEXTURE_IMG, num:it.num, buf:compress(it.buf)]) }
        
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
            hdrBuf.put((byte)(len >> 8) | (chunk.buf.compressed ? 0x80 : 0))
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
    
    def pack(xmlPath, binPath, javascriptPath)
    {
        // Read in code chunks. For now these are hard coded, but I guess they ought to
        // be configured in a config file somewhere...?
        //
        println "Reading code resources."
        readCode("render", "src/raycast/build/render.b")
        readCode("expand", "src/raycast/build/expand.b")
        readCode("fontEngine", "src/font/build/fontEngine.b")
        readCode("tileEngine", "src/tile/build/tile.b")
        
        println "Reading modules."
        readModule("gameloop", "src/plasma/build/gameloop.b")
        
        // We have only one font, for now at least.
        println "Reading fonts."
        readFont("font", "data/fonts/font.bin")
        
        // Open the XML data file produced by Outlaw Editor
        def dataIn = new XmlParser().parse(xmlPath)
        
        // Pre-pack the data for each tile
        println "Packing tiles."
        dataIn.tile.each { 
            packTile(it) 
        }

        // Pack the global tile set before other tile sets (contains the player avatar, etc.)
        packGlobalTileSet(dataIn)
        
        // Pack each image, which has the side-effect of filling in the
        // image name map. Handle frame images separately.
        //
        println "Packing frame images and textures."
        dataIn.image.each { image ->
            if (image.category.text() == "title")
                packFrameImage(image)
        }
        dataIn.image.each { image ->
            if (image.category.text() == "frame")
                packFrameImage(image)
        }
        dataIn.image.each { image ->
            if (image.category.text() == "126")
                pack126(image)
            else
                packTexture(image)
        }
        
        // If doing Javascript debugging, open that output file too.
        if (javascriptPath)
            javascriptOut = new File(javascriptPath).newPrintWriter()
            
        // Number all the maps and record them with names
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
        
        // Ready to start writing the output file.
        println "Writing output file."
        new File(binPath).withOutputStream { stream -> writePartition(stream) }
        
        // Finish up Javacript if necessary
        if (javascriptPath)
            javascriptOut.close()
        
        // Lastly, print stats
        println "Compression saved $compressionSavings bytes."
        if (compressionSavings > 0) {
            def endSize = new File(binPath).length()
            def origSize = endSize + compressionSavings
            def savPct = String.format("%.1f", compressionSavings * 100.0 / origSize)
            println "Size $origSize -> $endSize ($savPct% savings)"
        }
        
        println "Done."
    }
    
    static void main(String[] args) 
    {
        // Set auto-flushing for stdout
        System.out = new PrintStream(new BufferedOutputStream(System.out), true)
        
        // Verify that assertions are enabled
        def flag = false
        try {
            assert false
        }
        catch (AssertionError e) {
            flag = true
        }
        if (!flag) {
            println "Error: assertions must be enabled. Run with 'ea-'"
            System.exit(1);            
        }
        
        // Check the arguments
        if (args.size() != 2 && args.size() != 3) {
            println "Usage: convert yourOutlawFile.xml game.part.0.bin [intcastMap.js]"
            println "   (where intcastMap.js is to aid in debugging the Javascript raycaster)"
            System.exit(1);
        }

        // If there's an existing error file, remote it.
        def errorFile = new File("pack_error.txt")
        if (errorFile.exists())
            errorFile.delete()
            
        // Go for it.
        def inst = new PackPartitions()
        try {
            inst.pack(args[0], args[1], args.size() > 2 ? args[2] : null)
        }
        catch (Throwable t) {
            errorFile.withWriter { out ->
                out.println "Packing error: ${t.message}"
                out.println "\nContext:"
                out.println "    ${inst.getContextStr()}"
                out.println "\nGroovy call stack:"
                t.getStackTrace().each {
                    if (it.toString().contains(".groovy:"))
                        out.println "    $it"
                }
            }
            def msg = "Fatal error encountered in ${inst.getContextStr()}.\nDetails written to file 'pack_error.txt'."
            println msg
            System.out.flush()
            javax.swing.JOptionPane.showMessageDialog(null, msg, "Fatal packing error", 
                javax.swing.JOptionPane.ERROR_MESSAGE)
            System.exit(1)
        }
        
        if (inst.nWarnings > 0) {
            javax.swing.JOptionPane.showMessageDialog(null,
                "${inst.nWarnings} warning(s) noted during packing.\nCheck console for details.",
                "Pack warnings",
                javax.swing.JOptionPane.ERROR_MESSAGE)
        }
    }

    class ScriptModule
    {
        def data = []
        def bytecode = []
        def fixups = []

        def nScripts = 0

        def locationsWithTriggers = [] as Set

        def vec_locationTrigger = 0x300
        def vec_displayStr      = 0x303
        def vec_getYN           = 0x306
        def vec_setMap          = 0x309
        def vec_setSky          = 0x30C
        def vec_setGround       = 0x30F
        def vec_teleport        = 0x312

        def addString(str)
        {
            assert str.size() < 256 : "String too long, max is 255 characters: $str"
            def addr = dataAddr()
            emitDataByte(str.size())
            str.each { ch -> emitDataByte((byte)ch) }
            return addr
        }

        /**
         * Pack scripts from a map. Either the whole map, or optionally just an X and Y
         * bounded section of it.
         * 
         * Returns true if any matching scripts were found.
         */
        def packScripts(inScripts, xRange = null, yRange = null)
        {
            // If we're only processing a section of the map, make sure this script is
            // referenced within that section.
            //
            def scripts = []
            inScripts.script.eachWithIndex { script, idx ->
                def name = script.name[0].text()
                if (name.toLowerCase() == "init")
                    scripts << script
                else if (script.locationTrigger.any { trig ->
                            (!xRange || trig.@x.toInteger() in xRange) &&
                            (!yRange || trig.@y.toInteger() in yRange) })
                    scripts << script
            }
                
            nScripts = scripts.script.size()
            if (nScripts == 0)
                return false
                
            makeStubs()
            scripts.eachWithIndex { script, idx ->
                packScript(idx, script) 
            }
            makeInit(scripts, xRange, yRange)
            emitFixupByte(0xFF)
            //println "data: $data"
            //println "bytecode: $bytecode"
            //println "fixups: $fixups"
            return true
        }

        def makeStubs()
        {
            // Emit a stub for each function, including the init function
            (0..nScripts).each { it ->
                emitDataByte(0x20)  // JSR
                emitDataWord(0x3DC) // Aux mem interp ($3DC)
                emitDataWord(0)     // Placeholder for the bytecode offset
            }
        }

        def startFunc(scriptNum)
        {
            def fixAddr = (scriptNum * 5) + 3
            assert data[fixAddr] == 0 && data[fixAddr+1] == 0
            data[fixAddr] = (byte)(bytecodeAddr() & 0xFF)
            data[fixAddr+1] = (byte)((bytecodeAddr() >> 8) & 0xFF)
        }

        def packScript(scriptNum, script)
        {
            def name = script.name[0].text()
            if (name.toLowerCase() == "init") // this special script gets processed later
                return
            
            //println "   Script '$name'"
            withContext("script '$name'") 
            {
                if (script.block.size() == 0) {
                    printWarning("empty script found; skipping.")
                    return
                }

                // Record the function's start address in its corresponding stub
                startFunc(scriptNum+1)

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
                
                // And complete the function
                finishFunc()
            }
        }

        def finishFunc()
        {
            // Finish off the function with a return value and return opcode
            emitCodeByte(0)     // ZERO
            emitCodeByte(0x5C)  // RET
        }

        def packBlock(blk)
        {
            withContext("${blk.@type}") {
                switch (blk.@type) 
                {
                    case 'text_print':
                    case 'text_println':
                        packTextPrint(blk); break
                    case  'controls_if':
                        packIfStmt(blk); break
                    case 'events_set_map':
                        packSetMap(blk); break
                    case 'events_set_sky':
                        packSetSky(blk); break
                    case 'events_set_ground':
                        packSetGround(blk); break
                    case 'events_teleport':
                        packTeleport(blk); break
                    default:
                        printWarning "don't know how to pack block of type '${blk.@type}'"
                }
            }

            // Strangely, blocks seem to be chained together, but hierarchically. Whatever.
            blk.next.each { it.block.each { packBlock(it) } }
        }

        def dataAddr()
        {
            return data.size()
        }

        def bytecodeAddr()
        {
            return bytecode.size()
        }

        def emitDataByte(b)
        {
            assert b <= 255
            data.add((byte)(b & 0xFF))
        }

        def emitDataWord(w)
        {
            emitDataByte(w & 0xFF)
            emitDataByte(w >> 8)
        }

        def emitCodeByte(b)
        {
            assert b <= 255
            bytecode.add((byte)(b & 0xFF))
        }

        def emitCodeWord(w)
        {
            emitCodeByte(w & 0xFF)
            emitCodeByte(w >> 8)
        }

        def emitFixupByte(b)
        {
            assert b <= 255
            fixups.add((byte)(b & 0xFF))
        }

        def emitDataFixup(toAddr)
        {
            // Src addr is reversed (i.e. it's hi then lo)
            emitFixupByte(dataAddr() >> 8)
            emitFixupByte(dataAddr() & 0xFF)
            emitDataWord(toAddr)
        }

        def emitCodeFixup(toAddr)
        {
            // Src addr is reversed (i.e. it's hi then lo), and marked by hi-bit flag.
            emitFixupByte((bytecodeAddr() >> 8) | 0x80)
            emitFixupByte(bytecodeAddr() & 0xFF)
            emitCodeWord(toAddr)
        }

        def packTextPrint(blk)
        {
            if (blk.value.size() == 0) {
                printWarning "empty text_print block, skipping."
                return
            }
            def val = blk.value[0]
            assert val.@name == 'VALUE'
            assert val.block.size() == 1
            def valBlk = val.block[0]
            assert valBlk.@type == 'text'
            assert valBlk.field.size() == 1
            def fld = valBlk.field[0]
            assert fld.@name == 'TEXT'
            def text = fld.text()
            //println "            text: '$text'"

            emitCodeByte(0x26)  // LA
            def textAddr = addString(text)
            emitCodeFixup(textAddr)
            emitCodeByte(0x54)  // CALL
            emitCodeWord(vec_displayStr)
            emitCodeByte(0x30) // DROP
        }

        def packIfStmt(blk)
        {
            if (blk.value.size() == 0) {
                printWarning "missing condition; skipping."
                return
            }
            assert blk.value.size() == 1
            def cond = blk.value[0]
            assert cond.@name == 'IF0'
            assert cond.block.size() == 1
            assert cond.block[0].@type == 'text_getboolean'

            //print "            Conditional on getboolean,"

            emitCodeByte(0x54)  // CALL
            emitCodeWord(vec_getYN)
            emitCodeByte(0x4C)  // BRFS
            def branchStart = bytecodeAddr()
            emitCodeWord(0)     // placeholder until we know how far to jump over

            assert blk.statement.size() == 1
            def doStmt = blk.statement[0]
            assert doStmt.block.size() == 1
            packBlock(doStmt.block[0])
            
            // Now calculate and fix the relative branch
            def branchEnd = bytecodeAddr()
            def rel = branchEnd - branchStart
            bytecode[branchStart] = (byte)(rel & 0xFF)
            bytecode[branchStart+1] = (byte)((rel >> 8) & 0xFF)
        }

        def packSetMap(blk)
        {
            assert blk.field.size() == 1
            def fld = blk.field[0]
            assert fld.@name == 'NAME'
            def mapName = fld.text()
            def mapNum = mapNames[mapName]
            if (!mapNum) {
                printWarning "map '$mapName' not found; skipping set_map."
                return
            }
            //println "            Set map to '$mapName' (num $mapNum)"
            assert mapNum : "Map $mapName not found!"
            
            emitCodeByte(0x2A) // CB
            assert mapNum[0] == '2D' || mapNum[0] == '3D'
            emitCodeByte(mapNum[0] == '2D' ? 0 : 1)
            emitCodeByte(0x2A) // CB
            emitCodeByte(mapNum[1])
            emitCodeByte(0x54)  // CALL
            emitCodeWord(vec_setMap)
            emitCodeByte(0x30) // DROP
        }
        
        def packSetSky(blk)
        {
            assert blk.field.size() == 1
            def fld = blk.field[0]
            assert fld.@name == 'COLOR'
            def color = fld.text().toInteger()
            assert color >= 0 && color <= 15
            //println "            Set sky to $color"
            
            emitCodeByte(0x2A) // CB
            emitCodeByte(color)
            emitCodeByte(0x54) // CALL
            emitCodeWord(vec_setSky)
            emitCodeByte(0x30) // DROP
        }

        def packSetGround(blk)
        {
            assert blk.field.size() == 1
            def fld = blk.field[0]
            assert fld.@name == 'COLOR'
            def color = fld.text().toInteger()
            assert color >= 0 && color <= 15
            //println "            Set ground to $color"
            
            emitCodeByte(0x2A) // CB
            emitCodeByte(color)
            emitCodeByte(0x54) // CALL
            emitCodeWord(vec_setGround)
            emitCodeByte(0x30) // DROP
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
            //println "            Teleport to ($x,$y) facing $facing"
            
            emitCodeByte(0x2C) // CW
            emitCodeWord(x)
            emitCodeByte(0x2C) // CW
            emitCodeWord(y)
            emitCodeByte(0x2A) // CB
            emitCodeByte(facing)
            emitCodeByte(0x54) // CALL
            emitCodeWord(vec_teleport)
            emitCodeByte(0x30) // DROP
        }

        def makeInit(scripts, xRange, yRange)
        {
            //println "    Script: special 'init'"
            startFunc(0)
            
            // Emit the code the user has stored for the init script. While we're scanning,
            // might as well also collate all the location triggers into a sorted map.
            //
            TreeMap triggers = [:]
            scripts.eachWithIndex { script, idx ->
                def name = script.name[0].text()
                if (name.toLowerCase() == "init")
                {
                    if (script.block.size() == 1) {
                        def proc = script.block[0]
                        assert proc.@type == "procedures_defreturn"
                        assert proc.statement.size() == 1
                        def stmt = proc.statement[0]
                        assert stmt.@name == "STACK"
                        stmt.block.each { packBlock(it) }
                    }
                }
                else {
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
                            triggers[y][x] = (idx+1) * 5  // address of function
                        }
                    }
                }
            }
            
            // If any triggers, register and output a trigger table
            if (triggers.size()) 
            {
                // Code to register the table
                emitCodeByte(0x26)  // LA
                emitCodeFixup(dataAddr())
                emitCodeByte(0x54) // CALL
                emitCodeWord(vec_locationTrigger)
                emitCodeByte(0x30) // DROP
                
                // The table itself goes in the data segment.
                triggers.each { y, xs ->
                    emitDataByte(y)
                    emitDataByte(2 + (xs.size() * 3))  // 2 bytes for y+off, plus 3 bytes per trigger (x, adrlo, adrhi)
                    xs.each { x, funcAddr ->
                        emitDataByte(x)
                        emitDataFixup(funcAddr)
                        // Record a list of trigger locations for the caller's reference
                        locationsWithTriggers << [x, y]
                    }
                }
                emitDataByte(0xFF) // mark the end end of the trigger table
            }
            
            // All done with the init function.
            finishFunc()
        }

        def packFixups()
        {
            def buf = []
            fixups.each { fromOff, fromType, toOff, toType ->
                assert fromType == 'bytecode'
                assert toType   == 'data'
                toOff += (nScripts+1) * 5

            }
        }
    }
    
}