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
class PackMap 
{
    def parseMap(tiles, map)
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
                    if (pixBits == 0)
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
                /*
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
        

    def writeMap(stream, rows, names)
    {
        def width = rows[0].size()
        def height = rows.size()

        // Header: one-char code followed by two-byte length 
        // (length should not include the 5-byte header)
        //
        def len = width*height
        stream.write((int)'M')  // for "Map"
        stream.write(width)
        stream.write(height)
        stream.write(len & 0xFF)
        stream.write((len>>8) & 0xFF)
        
        // After the header comes the raw data
        rows.each { row ->
            row.each { tile ->
                stream.write(names.findIndexOf { it == tile?.@name } + 1)
            }
        }
    }
    
    // The renderer wants bits of the two pixels interleaved
    def combine(pix1, pix2) {
        return ((pix1 & 1) << 0) | ((pix2 & 1) << 1) |
               ((pix1 & 2) << 1) | ((pix2 & 2) << 2) |
               ((pix1 & 4) << 2) | ((pix2 & 4) << 3);
    }
    
    def writeImage(stream, image)
    {
        // First, accumulate pixel data for all 5 mip levels plus the orig image
        def buf = ByteBuffer.allocate(50000)
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
        def len = buf.position() // len doesn't include 3-byte header
        
        // Write the header now that we have the length.
        stream.write((int)'T') // for 'Texture'
        stream.write(len & 0xFF)
        stream.write((len>>8) & 0xFF)
        
        // And copy the data to the output
        def tmp = new byte[buf.position()]
        buf.position(0)
        buf.get(tmp)
        stream.write(tmp)
    }
    
    def pack(xmlPath, binPath)
    {
        // Open the XML data file produced by Outlaw Editor
        def dataIn = new XmlParser().parse(xmlPath)
        
        // Locate the map named 'main', or failing that, the first map
        def map = dataIn.map.find { it.@name == 'main' }
        if (!map) 
            map = dataIn.map[0]
        assert map : "Can't find map 'main' nor any map at all";
            
        // Identify all the tiles and make a list of rows
        def rows = parseMap(dataIn.tile, map);
        
        // Determine the unique names of all the 'obstruction' tiles. Those
        // are the ones that turn into texture images.
        //
        def names = rows.flatten().grep{it?.@obstruction == 'true'}.
            collect{it.@name}.sort().unique()
        
        println "Parsing images."
        def images = names.collect { name ->
            parseImage(dataIn.image.find { it.@name == name })
        }
        
        // Ready to start writing the output file.
        new File(binPath).withOutputStream { stream ->
            println "Writing map."
            writeMap(stream, rows, names)
            images.eachWithIndex { image, idx ->
                println "Writing image #${idx+1}."
                writeImage(stream, image) 
            }
            stream.write(0) // properly terminate the file
        }
        
        println "Done."
    }
    
    static void main(String[] args) 
    {
        if (args.size() != 2) {
            println "Usage: convert yourMapFile.xml out.bin"
            System.exit(1);
        }
        new PackMap().pack(args[0], args[1])
    }
}

