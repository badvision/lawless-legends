#!/usr/bin/env python3

import base64
import hashlib
import re
import sys
import xml.etree.ElementTree as ET

seenhashes = set()
scriptlines = 0
changing = False
new_hashes = {}

###############################################################################
def hashstr(s):
    md5 = hashlib.md5(s.encode())
    return re.sub(r'=+$', '', base64.b32encode(md5.digest()[0:6]).decode())

###############################################################################
def processsentence(sentence):
    global scriptlines
    h = hashstr(sentence)
    if changing:
        if not h in new_hashes:
            print(f"hmm, hash {h!r} is missing from new_hashes. Sentence: {sentence!r}")
            return sentence
        return new_hashes[h]
    else:
        if h in seenhashes:
            print(f"{h}\t<dupe>")
        else:
            print(f"{h}\t{sentence}")
        scriptlines += 1
        seenhashes.add(h)

###############################################################################
def processnode(textfield):
    text = textfield.text
    if text is not None:
        lines = re.split(r'(\^[mM])', text)
        out = []
        for i in range(0, len(lines), 2):
            line = lines[i]
            sep = lines[i+1] if i+1 < len(lines) else None
            assert line is not None
            parts = re.split(r'((?<!Mr)(?<!Mrs)(?<!Ms)(?<!Dr)[.!?…]+[)”"=]?\s*)', line)
            for j in range(0, len(parts), 2):
                sentence = parts[j] + (parts[j+1] if j+1 < len(parts) else '')
                if not sentence == "":
                    newsent = processsentence(sentence)
                    # Retain exact spacing from old sentence, since it is hard
                    # to see in the correction doc.
                    oldstartsp = re.match(r'^\s*', sentence)[0]
                    oldendsp   = re.search(r'\s*$', sentence)[0]
                    newsent = oldstartsp + newsent.strip() + oldendsp
                    out.append(newsent)
            if changing and sep:
                out.append(sep)
        if changing:
            newtext = "".join(out)
            textfield.text = newtext

###############################################################################
def trav(node, parentpath):
    global scriptlines
    if node.tag.endswith("script"):
        if scriptlines > 0:
            print()
            scriptlines = 0
    path = parentpath + [f"{node.tag.replace('{outlaw}', '')}{':'+node.attrib['type'] if 'type' in node.attrib else ''}"]
    if node.tag.endswith("next"):
        path = path[0:-2]
    strpath = ";".join(path)
    if re.search(r'block:text_(print|story).*block:text.*field', strpath):
        processnode(node)
    for kid in node:
        trav(kid, path)

###############################################################################
def read_hashes(filename):
    out = {}
    with open(filename, "r") as io:
        for line in io.readlines():
            if line == '\n':
                continue
            line = line.replace("\ufeff", "") # Get rid of byte order mark from Word
            m = re.match(r'^(?P<hashcode>[A-Z0-9]{10}) {8}(?P<sentence>.*)\n$', line)
            assert m, f"Can't parse {line!r}"
            d = m.groupdict()
            if m['sentence'] != "<dupe>":
                out[m['hashcode']] = m['sentence']
    return out

###############################################################################
# From https://stackoverflow.com/questions/54439309/how-to-preserve-namespaces-when-parsing-xml-via-elementtree-in-python
def register_all_namespaces(filename):
    namespaces = dict([node for _, node in ET.iterparse(filename, events=['start-ns'])])
    for ns in namespaces:
        ET.register_namespace(ns, namespaces[ns])

###############################################################################

register_all_namespaces(sys.argv[1])
tree = ET.parse(sys.argv[1])
if len(sys.argv) == 3:  # progname infile newhashes
    new_hashes = read_hashes(sys.argv[2])
    changing = True
else:
    changing = False

trav(tree.getroot(), [])

if changing:
    print("Writing 'out.xml'.")
    with open('out.xml', 'wb') as io:
        io.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n""".encode())
        tree.write(io, encoding='utf-8')
