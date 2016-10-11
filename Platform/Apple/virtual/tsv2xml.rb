#!/opt/local/bin/ruby

require 'nokogiri'
require 'pp'

colNames = []
out = Nokogiri::XML::Document.new
out.add_child(sheet = out.create_element("sheet"))
sheet["name"] = ARGV[0].sub(/.*\//, "").sub(/\..*$/, "").sub(/.* - /, "").capitalize
rowsEl = nil
open(ARGV[0], "r").readlines.each_with_index { |line, idx|
  fields = line.chomp.split("\t")
  if idx == 0
    sheet.add_child(columnsEl = out.create_element("columns"))
    fields.each { |inField|
      outField = inField.downcase.gsub("xd6", "").gsub("#", "num").gsub(/[^-a-zA-Z0-9 ]/, "").strip.gsub(/\s+/, "-")
      colNames << outField
      columnsEl.add_child out.create_element("column", :name => outField)
    }
    sheet.add_child(rowsEl = out.create_element("rows"))
  else
    rowsEl.add_child(rowEl = out.create_element("row"))
    fields.each_with_index { |val, idx| rowEl[colNames[idx]] = val }
  end
}

puts sheet