#!/usr/bin/env bash

egrep 'field name="TEXT"' data/world/world.xml | sed 's/ *<[^>]*>//g' > gameStrings.txt
