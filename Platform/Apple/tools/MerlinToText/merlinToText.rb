#!/usr/bin/env ruby

# Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
# (the "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
# Unless required by applicable law or agreed to in writing, software distributed under 
# the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
# ANY KIND, either express or implied. See the License for the specific language 
# governing permissions and limitations under the License.

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
