#!/bin/sh 

echo stage3 $@

TIME=$(( 360/$6 ))

echo processing time $TIME

sleep $TIME
exit 0 

