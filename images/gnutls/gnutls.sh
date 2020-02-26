#!/bin/bash
cd "$(dirname "$0")" || exit 1
source ../helper-functions.sh
array=(0 0_1 1 2 3 4)
typeset -i i=0 max=${#array[*]}
while (( i < max ))
do
	echo "Building: GnuTLS 3.6.${array[$i]}"
	_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_6_${array[$i]}-server -f Dockerfile-3_6_x --target gnutls-server .
	_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_6_${array[$i]}-client -f Dockerfile-3_6_x --target gnutls-client .
	i=i+1
done

# FIXME: Build error in test with v7 -> check that
array=(0 1 2 4 5 6 7)
typeset -i i=0 max=${#array[*]}
while (( i < max ))
do
	echo "Building: GnuTLS 3.5.${array[$i]}"
	#_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_5_${array[$i]}-server -f Dockerfile-3_5_0-7 --target gnutls-server .
	#_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_5_${array[$i]}-client -f Dockerfile-3_5_0-7 --target gnutls-client .
	i=i+1
done

array=(8 9 10 11 12 13 14 15 16 17 18 19)
typeset -i i=0 max=${#array[*]}
while (( i < max ))
do
	echo "Building: GnuTLS 3.5.${array[$i]}"
	_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_5_${array[$i]}-server -f Dockerfile-3_5_8-16 --target gnutls-server .
	_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_5_${array[$i]}-client -f Dockerfile-3_5_8-16 --target gnutls-client .
	i=i+1
done

# FIXME: Build error in test with v17 -> check that
array=(0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17)
typeset -i i=0 max=${#array[*]}
while (( i < max ))
do
	echo "Building: GnuTLS 3.4.${array[$i]}"
	#_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_4_${array[$i]}-server -f Dockerfile-3_4_x --target gnutls-server .
	#_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_4_${array[$i]}-client -f Dockerfile-3_4_x --target gnutls-client .
	i=i+1
done

# FIXME: Build error in test with v15 -> check that
array=(0 1 2 3 4 5 6 8 9 10 11 12 13 14 15)
typeset -i i=0 max=${#array[*]}
while (( i < max ))
do
	echo "Building: GnuTLS 3.3.${array[$i]}"
	#_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_3_${array[$i]}-server -f Dockerfile-3_3_0-15 --target gnutls-server .
	#_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_3_${array[$i]}-client -f Dockerfile-3_3_0-15 --target gnutls-client .
	i=i+1
done

# FIXME: Build error in test with v28 -> check that
array=(16 17 18 19 20 21 22 23 24 25 26 27 28)
typeset -i i=0 max=${#array[*]}
while (( i < max ))
do
	echo "Building: GnuTLS 3.3.${array[$i]}"
	#_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_3_${array[$i]}-server -f Dockerfile-3_3_x --target gnutls-server .
	#_docker build --build-arg VERSION=${array[$i]} -t gnutls-3_3_${array[$i]}-client -f Dockerfile-3_3_x --target gnutls-client .
	i=i+1
done

exit "$EXITCODE"
