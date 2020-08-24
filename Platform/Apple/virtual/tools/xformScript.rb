#!/usr/bin/env ruby

print "R"
File.readlines(ARGV[0]).each do |line|
  line.scan(/KEY:(.*?) SEED:/).each { |match| print $1 }
  line.scan(/STRING:(.*)/).each { |match| puts $1 }
end
print "\n"