#!/bin/bash

array=(a b c d e f -pre3)
typeset -i i=0 max=${#array[*]}
docker build --build-arg VERSION= -t openssl-1_1_0-server -f Dockerfile-1_1_0x --target openssl-server .
docker build --build-arg VERSION= -t openssl-1_1_0-client -f Dockerfile-1_1_0x --target openssl-client .
while (( i < max ))
do
	echo "Feld $i: Openssl 1.1.0${array[$i]}"
	docker build --build-arg VERSION=${array[$i]} -t openssl-1_1_0${array[$i]}-server -f Dockerfile-1_1_0x --target openssl-server .
	docker build --build-arg VERSION=${array[$i]} -t openssl-1_1_0${array[$i]}-client -f Dockerfile-1_1_0x --target openssl-client .
	i=i+1
done
