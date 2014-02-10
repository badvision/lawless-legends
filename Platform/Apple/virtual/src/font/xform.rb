#!/usr/bin/env ruby

require 'optparse'

# Parse the command-line
options = {}
optparse = OptionParser.new do |opts|
  opts.banner = "Usage: #{$0} [inFile] [outFile]"
end
optparse.parse!
if ARGV.length != 2 then
  puts optparse
  exit 1
end
inFileName, outFileName = ARGV

File.open(inFileName, "r") do |inFile|
  File.open(outFileName, "w") do |outFile|
    until inFile.eof?
      b = inFile.read(1).unpack("C")[0]
      if b == 0xA0 then
        outFile.print "\t"
        next
      end
      b &= 0x7f
      if b == 13 then
        outFile.print "\n"
        next
      end
      outFile.print b.chr
    end
  end
end
