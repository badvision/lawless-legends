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
    
    def TYPE_CODE  = 1
    def TYPE_MAP   = 2
    def TYPE_IMAGE = 3
    
    def images = [:]  // img name to img.num, img.buf
    def tiles  = [:]  // tile name to tile.num, tile.buf
    def maps   = [:]  // map name to map.num, map.buf
    def code   = [:]  // code name to code.num, code.buf
    
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
    
    def parseImage(imgEl)
    {
        // Locate the data for the Apple II (as opposed to C= etc.)
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
    def reduceImage(imgIn)
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
    
    def printImage(rows)
    {
        rows.each { row ->
            print "    "
            row.each { pix -> print pix }
            println ""
        }
    }
        

    def writeMap(buf, rows) // [ref BigBlue1_50]
    {
        def width = rows[0].size()
        def height = rows.size()
        def unknown = [] as Set

        // Header: just the width and height
        buf.put((byte)width)
        buf.put((byte)height)
        
        // After the header comes the raw data
        rows.each { row ->
            row.each { tile ->
                if (tile?.@obstruction == 'true') {
                    def name = tile?.@name
                    if (name in images)
                        buf.put((byte)images[name].num)
                    else {
                        // Alert only once about each unknown name
                        if (!(name in unknown))
                            println "Can't match tile name '$name' to any image; treating as blank."
                        unknown.add(name)
                        buf.put((byte)0)
                    }
                }
                else
                    buf.put((byte)0)
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
    
    def writeImage(buf, image)
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
                image = reduceImage(image)
        }
    }
    
    def packImage(imgEl)
    {
        def num = images.size() + 1
        def name = imgEl.@name ?: "img$num"
        println "Packing image #$num named '${imgEl.@name}'."
        def pixels = parseImage(imgEl)
        calcTransparency(pixels)
        def buf = ByteBuffer.allocate(50000)
        writeImage(buf, pixels)
        images[imgEl.@name] = [num:num, buf:buf]
    }
    
    def packMap(mapEl, tileEls)
    {
        def num = maps.size() + 1
        def name = mapEl.@name ?: "map$num"
        println "Packing map #$num named '$name'."
        def rows = parseMap(mapEl, tileEls)
        def buf = ByteBuffer.allocate(50000)
        writeMap(buf, rows)
        maps[name] = [num:num, buf:buf]
    }
    
    def writeBufToStream(stream, buf)
    {
        def endPos = buf.position()
        def bytes = new byte[endPos]
        buf.position(0)
        buf.get(bytes)
        stream.write(bytes)
    }
    
    def readCode(name, path)
    {
        def num = code.size() + 1
        println "Reading code #$num from '$path'."
        def inBuf = new byte[256]
        def outBuf = ByteBuffer.allocate(50000)
        def stream = new File(path).withInputStream { stream ->
            while (true) {
                def got = stream.read(inBuf)
                if (got < 0) break
                outBuf.put(inBuf, 0, got)
            }
        }
        code[name] = [num:num, buf:outBuf]
    }
    
    def writePartition(stream)
    {
        // Make a list of all the chunks that will be in the partition
        def chunks = []
        code.values().each { chunks.add([type:TYPE_CODE, num:it.num, buf:it.buf]) }
        maps.values().each { chunks.add([type:TYPE_MAP, num:it.num, buf:it.buf]) }
        images.values().each { chunks.add([type:TYPE_IMAGE, num:it.num, buf:it.buf]) }
        
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
        readCode("render", "src/raycast/build/render.bin#7000")
        readCode("expand", "src/raycast/build/expand.bin#0800")
        
        // Open the XML data file produced by Outlaw Editor
        def dataIn = new XmlParser().parse(xmlPath)
        
        // Pack each image, which has the side-effect of filling in the
        // image name map.
        //
        dataIn.image.each { packImage(it) }
        
        // Pack each map. This uses the image map filled earlier, and
        // fills the map name map.
        //
        dataIn.map.each { packMap(it, dataIn.tile) }
        
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

