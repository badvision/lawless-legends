#!/usr/bin/env ruby

MULT = 32
#BIG_NUM = 65521  # should be prime
#BIG_NUM = 65519
BIG_NUM = 32749

def linCong(seed)
  if seed == 0
    seed = 1
  else
    seed &= 0x7FFF
    seed >= BIG_NUM and seed -= BIG_NUM
    5.times {
      seed <<= 1
      seed >= BIG_NUM and seed -= BIG_NUM
    }
  end
  return seed
end

def rnd02(seed)
  magic = 0x2227
  if seed == 0
    seed = magic
  elsif seed == 0x8000
    seed = 0
  else
    seed <<= 1
    if (seed & 0x8000) > 0
      seed = (seed & 0x7fff) ^ magic
    end
  end
  return seed
end

done = {}

minLoop = nil
(BIG_NUM..0xFFFF).each { |invalSeed|
  out = linCong(invalSeed)
  out >= 0 && out < BIG_NUM or raise("Bad: $%x -> $%x" % [invalSeed, out])
}
(0..BIG_NUM-1).each { |startSeed|
  next if done[startSeed]
  n = 0
  seed = startSeed
  loop do
    print "%016b %04X -> " % [seed, seed]
    seed = linCong(seed)
    puts "%04X" % seed
    if done[seed]
      length = n - done[seed]
      puts "loop at #{startSeed} -> #{seed}: length=#{length}"
      minLoop = minLoop ? [minLoop, length].min : length
      break
    end
    done[seed] = n
    n += 1
  end
}
puts "minLoop=#{minLoop}"
