/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.demo

import java.nio.ByteBuffer
import java.nio.channels.Channels

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
    def TYPE_TILE_IMG    = 4
    def TYPE_TEXTURE_IMG = 5
    def TYPE_FRAME_IMG   = 6
    def TYPE_FONT        = 7

    def code     = [:]  // code name to code.num, code.buf    
    def maps2D   = [:]  // map name to map.num, map.buf
    def maps3D   = [:]  // map name to map.num, map.buf
    def tiles    = [:]  // tile name to tile.num, tile.buf
    def textures = [:]  // img name to img.num, img.buf
    def frames   = [:]  // img name to img.num, img.buf
    def fonts    = [:]
    
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
        if (imgEl.@name == "Building6") {
            println "hacking high bits in Building6"
            def rowNum = 0
            result = result.collect { row ->
                rowNum++
                row.collect { pix ->
                    (rowNum <= 25 && pix == 3) ? 7 :
                    (rowNum <= 25 && pix == 0) ? 4 :
                    (rowNum >  25 && pix == 4) ? 0 :
                    (rowNum >  25 && pix == 7) ? 3 :
                    (rowNum >  25 && pix == 5) ? 0 :
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

    def write2DMap(buf, mapName, rows)
    {
        def width = rows[0].size()
        def height = rows.size()
        
        // Determine the set of all referenced tiles, and assign numbers to them.
        def tileMap = [:]
        def tileList = []
        rows.each { row ->
            row.each { tile ->
                def id = tile?.@id
                def name = tile?.@name
                if (!tileMap.containsKey(id)) {
                    tileMap[id] = 0
                    if (name in tiles) {
                        tileList.add(tiles[name].num)
                        tileMap[id] = tileList.size()
                    }
                    else if (id)
                        println "Warning: can't match tile name '$name' to any image; treating as blank."
                }
            }
        }

        // Header: width and height
        buf.put((byte)width)
        buf.put((byte)height)
        
        // Followed by name
        writeString(buf, mapName.replaceFirst(/ ?-? ?2D/, ""))
        
        // Followed by the list of tiles
        tileList.each { buf.put((byte)it) }
        buf.put((byte)0)
        
        // After the header comes the raw data
        rows.each { row ->
            row.each { tile ->
                buf.put((byte)tileMap[tile?.@id])
            }
        }
    }
    
    def write3DMap(buf, mapName, rows) // [ref BigBlue1_50]
    {
        def width = rows[0].size()
        def height = rows.size()
        
        // Determine the set of all referenced textures, and assign numbers to them.
        def texMap = [:]
        def texList = []
        rows.each { row ->
            row.each { tile ->
                def id = tile?.@id
                def name = tile?.@name
                if (!texMap.containsKey(id)) {
                    texMap[id] = 0
                    if (tile?.@obstruction == 'true') {
                        if (name in textures) {
                            texList.add(textures[name].num)
                            texMap[id] = texList.size()
                        }
                        else if (id)
                            println "Warning: can't match tile name '$name' to any image; treating as blank."
                    }
                    else if (name != 'street')
                        println "Note: ignoring non-obstruction '$name' until sprite support is added."
                }
            }
        }

        // Header: width and height
        buf.put((byte)width)
        buf.put((byte)height)
        
        // Followed by name
        writeString(buf, mapName.replaceFirst(/ ?-? ?3D/, ""))
        
        // Followed by the list of textures
        texList.each { buf.put((byte)it) }
        buf.put((byte)0)
        
        // After the header comes the raw data
        rows.each { row ->
            row.each { tile ->
                buf.put((byte)texMap[tile?.@id])
            }
        }
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
    
    def packTexture(imgEl)
    {
        def num = textures.size() + 1
        def name = imgEl.@name ?: "img$num"
        //println "Packing texture #$num named '${imgEl.@name}'."
        def pixels = parseTexture(imgEl)
        calcTransparency(pixels)
        def buf = ByteBuffer.allocate(50000)
        writeTexture(buf, pixels)
        textures[imgEl.@name] = [num:num, buf:buf]
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
    
    def packTile(imgEl)
    {
        def num = tiles.size() + 1
        def name = imgEl.@name ?: "img$num"
        //println "Packing tile image #$num named '${imgEl.@name}'."
        def buf = parseTileData(imgEl)
        tiles[imgEl.@name] = [num:num, buf:buf]
        return buf
    }
    
    def pack2DMap(mapEl, tileEls)
    {
        def num = maps2D.size() + 1
        def name = mapEl.@name ?: "map$num"
        //println "Packing 2D map #$num named '$name'."
        def rows = parseMap(mapEl, tileEls)
        def buf = ByteBuffer.allocate(50000)
        write2DMap(buf, name, rows)
        maps2D[name] = [num:num, buf:buf]
    }
    
    def pack3DMap(mapEl, tileEls)
    {
        def num = maps3D.size() + 1
        def name = mapEl.@name ?: "map$num"
        //println "Packing 3D map #$num named '$name'."
        def rows = parseMap(mapEl, tileEls)
        def buf = ByteBuffer.allocate(50000)
        write3DMap(buf, name, rows)
        maps3D[name] = [num:num, buf:buf]
    }
    
    def writeBufToStream(stream, buf)
    {
        def endPos = buf.position()
        def bytes = new byte[endPos]
        buf.position(0)
        buf.get(bytes)
        stream.write(bytes)
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
    
    def readFont(name, path)
    {
        def num = fonts.size() + 1
        //println "Reading font #$num from '$path'."
        fonts[name] = [num:num, buf:readBinary(path)]
    }
    
    def writePartition(stream)
    {
        // Make a list of all the chunks that will be in the partition
        def chunks = []
        code.values().each { chunks.add([type:TYPE_CODE, num:it.num, buf:it.buf]) }
        fonts.values().each { chunks.add([type:TYPE_FONT, num:it.num, buf:it.buf]) }
        frames.values().each { chunks.add([type:TYPE_FRAME_IMG, num:it.num, buf:it.buf]) }
        maps2D.values().each { chunks.add([type:TYPE_2D_MAP, num:it.num, buf:it.buf]) }
        tiles.values().each { chunks.add([type:TYPE_TILE_IMG, num:it.num, buf:it.buf]) }
        maps3D.values().each { chunks.add([type:TYPE_3D_MAP, num:it.num, buf:it.buf]) }
        textures.values().each { chunks.add([type:TYPE_TEXTURE_IMG, num:it.num, buf:it.buf]) }
        
        // Generate the header chunk. Leave the first 2 bytes for the # of pages in the hdr
        def hdrBuf = ByteBuffer.allocate(50000)
        hdrBuf.put((byte)0)
        hdrBuf.put((byte)0)
        
        // Write the four bytes for each resource
        chunks.each { chunk ->
            hdrBuf.put((byte)chunk.type)
            assert chunk.num >= 1 && chunk.num <= 255
            hdrBuf.put((byte)chunk.num)
            def len = chunk.buf.position()
            //println "  chunk: type=${chunk.type}, num=${chunk.num}, len=$len"
            hdrBuf.put((byte)(len & 0xFF))
            hdrBuf.put((byte)(len >> 8))
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
        writeBufToStream(stream, hdrBuf)
        chunks.each { writeBufToStream(stream, it.buf) }
    }
    
    def pack(xmlPath, binPath)
    {
        // Read in code chunks. For now these are hard coded, but I guess they ought to
        // be configured in a config file somewhere...?
        //
        println "Reading code resources."
        readCode("render", "src/raycast/build/render.b")
        readCode("expand", "src/raycast/build/expand.b")
        readCode("fontEngine", "src/font/build/fontEngine.b")
        
        // We have only one font, for now at least.
        println "Reading fonts."
        readFont("font", "data/fonts/font.bin")
        
        // Open the XML data file produced by Outlaw Editor
        def dataIn = new XmlParser().parse(xmlPath)
        
        // Pack each tile, which has the side-effect of filling in the
        // tile name map.
        //
        println "Packing tile images."
        dataIn.tile.each { packTile(it) }
        
        // Pack each image, which has the side-effect of filling in the
        // image name map. Handle frame images separately.
        //
        println "Packing frame images and textures."
        dataIn.image.each { image ->
            if (image.category.text() == "frame" )
                packFrameImage(image)
            else
                packTexture(image)
        }
        
        // Pack each map This uses the image and tile maps filled earlier.
        println "Packing maps."
        dataIn.map.each { map ->
            if (map?.@name =~ /2D/)
                pack2DMap(map, dataIn.tile) 
            else if (map?.@name =~ /3D/)
                pack3DMap(map, dataIn.tile) 
            else
                println "Warning: map name '$name' should contain '2D' or '3D'. Skipping."
        }
        
        // Ready to start writing the output file.
        println "Writing output file."
        new File(binPath).withOutputStream { stream -> writePartition(stream) }
        
        println "Done."
    }
    
    static void main(String[] args) 
    {
        if (args.size() != 2) {
            println "Usage: convert yourOutlawFile.xml DISK.PART.0.bin"
            System.exit(1);
        }
        new PackPartitions().pack(args[0], args[1])
    }
}

