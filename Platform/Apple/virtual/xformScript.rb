#!/usr/bin/env ruby

length = 0
File.readlines(ARGV[0]).each do |line|
  line.scan(/KEY:(.*?) SEED:/).each { |match|
    print $1
    length += 1
    if length > 80
      print "\n"
      length = 0
    end
  }
  line.scan(/STRING:(.*)/).each { |match|
    puts $1
    length = 0
  }
end
print "\n"