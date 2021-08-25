#!/usr/bin/env python3

import base64
import hashlib
import re
import sys
import xml.etree.ElementTree as ET

seenhashes = set()
scriptlines = 0

def hashstr(s):
    md5 = hashlib.md5(s.encode())
    return re.sub(r'=+$', '', base64.b32encode(md5.digest()[0:6]).decode())

def processsentence(sentence):
    global scriptlines
    h = hashstr(sentence)
    if h in seenhashes:
        print(f"{h}    <dupe>")
    else:
        print(f"{h}    {sentence}")
        seenhashes.add(h)
    scriptlines += 1

def processnode(textfield):
    text = textfield.text
    if text is not None:
        lines = re.split(r'(\^[mM])', text)
        for i in range(0, len(lines), 2):
            line = lines[i]
            sep = lines[i+1] if i+1 < len(lines) else None
            assert line is not None
            #if sep is not None:
            #    processsentence(sep)
            parts = re.split(r'((?<!Mr)(?<!Mrs)(?<!Ms)(?<!Dr)[.!?…]+[)”"=]?\s*)', line)
            #print(f"parts={parts!r}")
            for j in range(0, len(parts), 2):
                sentence = parts[j] + (parts[j+1] if j+1 < len(parts) else '')
                if not sentence == "":
                    processsentence(sentence)

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

tree = ET.parse(sys.argv[1])
trav(tree.getroot(), [])

#for el in tree.iter():
#    if 'type' in el.attrib and 'text_print' in el.attrib['type']
#    print(f"tag={el.tag} type={el.attrib['type'] if 'type' in el.attrib else None} text={el.text!r}")
