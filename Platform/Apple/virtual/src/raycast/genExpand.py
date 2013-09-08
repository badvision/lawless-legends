#!/usr/bin/env python

# This program generates optimized texture expansion code.

import copy, math, os, re, sys

# Main control on size vs. speed
rowInterval = 5

# Height of textures, in pixels
textureHeight = 64

# Screen height, in pixels
screenHeight = 128

# Other variables
allSegs = {}
allHeights = []
finalSuffixMap = {}
dstHeights = set()

outFile = open("expand.s", "w")
outFile.write("    .org $800\n")
outFile.write("    .include \"render.i\"\n")
outFile.write("\n")

def calcKey(cmds):
  return ".".join([str(c) for c in cmds])

# Make labels that are compact, unique, and representative.
# The letters below happen to make lots of semi-words that
# sound like "Totoro", which pleases me :)
#
def calcLabel(cmds):
  label = "e_"
  didNum = False
  for c in cmds:
    if c == "<":
      label += "r"
    elif c == "L":
      label += "t"
    elif didNum:
      label += "o"
    else:
      label += str(c)
      didNum = True
  return label

# Determine how much space a given set of commands will take in code
def calcSpace(cmds):
  space = 0 # for RTS
  n = 0
  for c in cmds:
    if c == "L":
      space += 3 # LDA (ptr),y / INY
    elif c == "<":
      space += 1 # LSR
    else:
      n += c
      space += 3 # STA row,x
  return space

allLabels = set()
generatedLabels = set()

class Segment:
  def __init__(s, cmds):
    s.cmds = cmds
    s.key = calcKey(cmds)
    s.label = calcLabel(cmds)
    s.space = calcSpace(s.cmds) + 1
    s.refs = 1
    s.truncLabel = None
    s.truncPos = None
    s.generated = False

  def finalSuffix(s):
    return ".".join(calcKey(s.cmds[-3:]))

  def findSuffix(s, o):
    n = min(len(s.cmds), len(o.cmds))
    for i in range(0,n):
      if (s.cmds[-1-i] != o.cmds[-1-i]):
        if i > 0:
          return s.cmds[-i:]
        return []
    return s.cmds[-n:]

  def genCode(s, grouped):
    global outFile, allLabels, generatedLabels
    print("genCode %s, trunc=%r" % (s.label, s.truncPos))
    first = grouped
    for i in range(len(s.cmds)):
      if i == s.truncPos:
        if grouped:
          if s.truncPos == 0:
            # Routine is entirely duplicated elsewhere: no need to do anything
            return
          if s.refs == 1:
            # singletons aren't generated near their target
            outFile.write("    jmp %s\n" % s.truncLabel) 
          else:
            # non-singletons are generated near their target
            outFile.write("    bra %s\n" % s.truncLabel)
        else:
          outFile.write("    jsr %s\n" % s.truncLabel)
        break
      label = calcLabel(s.cmds[i:])
      if label not in generatedLabels and (first or label in allLabels):
        outFile.write("%s:\n" % label)
        generatedLabels.add(label)
      first = False
      c = s.cmds[i]
      if c == 'L':
        outFile.write("    lda (pTex),y\n")
        outFile.write("    iny\n")
      elif c == '<':
        outFile.write("    lsr\n")
      else:
        assert c < screenHeight
        outFile.write("    sta %s*BLIT_STRIDE + blitRoll,x\n" % c)
    else:
      if grouped:
        outFile.write("    rts\n")
    if grouped:
      outFile.write("\n")
    outFile.flush()
    s.generated = True

def makeCmds(dstHeight):
  global allHeights, dstHeights

  dstHeights.add(dstHeight)

  # Figure out which texture mip-map level to use
  srcHeight = 2
  mipLevel = 5
  while srcHeight < textureHeight and srcHeight*2 <= dstHeight:
    mipLevel -= 1
    srcHeight *= 2

  y0 = (screenHeight/2) - (dstHeight/2)
  y1 = (screenHeight/2) + (dstHeight/2)
  b = 0
  r = 0
  cmds = []

  texOff = 0
  if y0 < 0 or y1 > screenHeight:
    texOff = -y0 / 2.0 * srcHeight / dstHeight
    r = int(((texOff*2) % 1) * dstHeight) # the "*2" here is because each byte stores 2 pixels
    if (texOff % 1) >= 0.5:
      cmds.append("L")
      cmds.append("<")
      b = 2
    #print("y0=%d texOff=%.3f r=%d b=%d" % (y0, texOff, r, b))
    texOff = int(texOff)
    y0 = max(0, y0)
    y1 = min(screenHeight, y1)

  #print("Doing dstHeight=%d y0=%d y1=%d texOff=%d" % (dstHeight, y0, y1, texOff))

  for y in range(y0, y1):
    if b == 0:
      cmds.append("L")
      b = 1
    cmds.append(y)
    if y < y1-1:
      r += srcHeight
      if r >= dstHeight:
        r -= dstHeight
        if b == 1:
          cmds.append("<")
          b = 2
        else:
          b = 0
      assert r < dstHeight, "wrapped twice?"

  print("%d ->\t%d:\t%s" % (srcHeight, dstHeight, calcKey(cmds)))
  allHeights.append((srcHeight, dstHeight, mipLevel, texOff, segment(cmds)))

def flushSeg(seg):
  global allSegs
  if seg.key in allSegs:
    allSegs[seg.key].refs += 1
  else:
    allSegs[seg.key] = seg
    fs = seg.finalSuffix()
    if not fs in finalSuffixMap:
      finalSuffixMap[fs] = set()
    finalSuffixMap[fs].add(seg.key)
  return seg.key

def segment(cmds):
  global rowInterval
  segs = []
  seg = []
  n = 0
  for c in cmds:
    seg.append(c)
    if c != "L" and c != "<":
      n += c
      if (c % rowInterval) == (rowInterval-1):
        segs.append(flushSeg(Segment(seg)))
        seg = []
  if len(seg) > 0:
    segs.append(flushSeg(Segment(seg)))
  return segs

for dstHeight in range(2,128,2):
  makeCmds(dstHeight)
for dstHeight in range(128,192,4):
  makeCmds(dstHeight)
for dstHeight in range(192,256,8):
  makeCmds(dstHeight)

# Create the jump table
dstHeight = 0
for h in range(0, 256, 2):
  if h == 0:
    outFile.write("expand_vec:\n")
  if h in dstHeights:
    dstHeight = h
  outFile.write("    .addr expand_%d\n" % dstHeight)
outFile.write("\n")

# Include the expand header code
outFile.write("    .include \"expand_hdr.i\"\n")

# Let's optimize.
segsToOpt = copy.copy(allSegs)
while len(segsToOpt) > 0:

  # Find the segment that would gain the most by being referenced.
  maxGain = 0
  bestDeps = None
  bestSeg = None
  for k1 in sorted(segsToOpt):
    deps = []
    gain = -1
    for k2 in sorted(finalSuffixMap[segsToOpt[k1].finalSuffix()]):
      if k2 == k1: continue # don't reference ourselves
      if not k2 in segsToOpt: continue # skip already-finished segs
      suf = segsToOpt[k1].findSuffix(segsToOpt[k2])
      if len(suf) < 3: continue
      sufKey = calcKey(suf)
      assert segsToOpt[k1].key.endswith(sufKey), \
             "suf %s vs %s = bad %s" % (segsToOpt[k1].key, segsToOpt[k2].key, sufKey)
      assert segsToOpt[k2].key.endswith(sufKey), \
             "suf %s vs %s = bad %s" % (segsToOpt[k1].key, segsToOpt[k2].key, sufKey)
      deps.append(k2)
      gain += calcSpace(suf) - 2 # 2 for branch
    if gain > maxGain:
      maxGain = gain
      bestDeps = deps
      bestSeg = k1
  if bestSeg is None:
    break

  # Display what we found
  print("Seg: %s\tgain %d" % (bestSeg, maxGain))
  for dep in bestDeps:
    suf = segsToOpt[bestSeg].findSuffix(segsToOpt[dep])
    print("\t%d\t%s" % (calcSpace(suf)-2, segsToOpt[dep].key))

  # Generate code for the non-singletons
  segsToOpt[bestSeg].refs += 1
  for dep in bestDeps:
    suf = segsToOpt[bestSeg].findSuffix(segsToOpt[dep])
    segsToOpt[dep].truncPos = len(segsToOpt[dep].cmds) - len(suf)
    segsToOpt[dep].truncLabel = calcLabel(suf)
    allLabels.add(segsToOpt[dep].truncLabel)
  for i in range(0, len(bestDeps)/2):
    if segsToOpt[bestDeps[i]].refs > 1:
      segsToOpt[bestDeps[i]].genCode(True)
  segsToOpt[bestSeg].genCode(True)
  for i in range(len(bestDeps)/2, len(bestDeps)):
    if segsToOpt[bestDeps[i]].refs > 1:
      segsToOpt[bestDeps[i]].genCode(True)

  # Remove these segments from optimization consideration
  del segsToOpt[bestSeg]
  for dep in bestDeps:
    del segsToOpt[dep]

# Now generate the controlling code
for (srcHeight, dstHeight, mipLevel, texOff, segs) in allHeights:
  outFile.write("; Produce %d rows from %d rows\n" % (dstHeight, srcHeight))
  outFile.write("expand_%d:\n" % dstHeight)
  outFile.write("    jsr selectMip%d\n" % mipLevel)
  if (texOff != 0):
    outFile.write("    ldy #%d\n" % texOff)
  for i in range(len(segs)):
    seg = allSegs[segs[i]]
    if seg.refs == 1 and not(seg.generated):
      seg.genCode(False)
      if i == len(segs)-1:
        # Special case for generating zero-size
        if "expand_0" not in generatedLabels:
          outFile.write("expand_0:\n")
          generatedLabels.add("expand_0")
        outFile.write("    rts\n")
    else:
      outFile.write("    %s %s\n" % ("jsr" if i < len(segs)-1 else "jmp", calcLabel(seg.cmds)))
  outFile.write("\n")

# Generate the misc segments missed earlier
for seg in allSegs.itervalues():
  if not seg.generated:
    seg.genCode(True)

outFile.close()
