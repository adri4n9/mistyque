#!/bin/bash

cd traces

# create a folder with the date to backup traces
folder=$(date +%Y%m%d%H%M)

mkdir "$folder"
mkdir "$folder"/traces

#backup all traces
cp *.bin   "$folder"/traces/

#  convert to dare devil format
python2 ../DCAAnalysis.py

## the script assumes daredevil is installed in the path

# execute the  analysis on the address
daredevil -c mem_addr1_rw1*

#backup daredevil intermediate results
cp *.input   "$folder"
cp *.output  "$folder"
cp *.trace   "$folder"
cp *.config  "$folder"

#clean last run
rm *.*
cd ..