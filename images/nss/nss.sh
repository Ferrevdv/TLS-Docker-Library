#!/bin/bash
cd "$(dirname "$0")" || exit 1
source ../helper-functions.sh

nss_versions=(3_87_RTM 3_86_RTM 3_85_RTM 3_84_RTM  3_79_3_RTM 3_83_RTM 3_82_RTM 3_79_1_RTM 3_81_RTM 3_80_RTM  3_79_RTM 3_78_RTM 3_77_RTM 3_76_RTM 3_75_RTM 3_74_RTM 3_73_1_RTM 3_73_RTM 3_72_1_RTM 3_72_RTM 3_71_RTM 3_70_RTM 3_69_1_RTM 3_69_RTM 3_68_2_RTM 3_68_1_RTM 3_68_RTM 3_67_RTM 3_66_RTM 3_65_RTM 3_64_RTM 3_63_1_RTM 3_63_RTM 3_62_RTM 3_61_RTM 3_60_RTM 3_59_1_RTM 3_59_RTM 3_58_RTM 3_57_RTM 3_56_RTM 3_55_RTM 3_54_RTM 3_53_1_RTM 3_52_1_RTM 3_51_1_RTM 3_50_RTM 3_49_2_RTM 3_48_1_RTM 3_47_1_RTM 3_46_1_RTM 3_45_RTM 3_44_4_RTM 3_43_RTM 3_42_1_RTM 3_41_RTM 3_40_1_RTM 3_39_RTM 3_38_RTM 3_37_3_RTM 3_36_8_RTM)
nspr_versions=(4_35_RTM 4_35_RTM 4_35_RTM 4_35_RTM 4_35_RTM 4_34_1_RTM 4_34_1_RTM 4_34_1_RTM 4_34_RTM 4_34_RTM 4_34_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_32_RTM 4_31_RTM 4_30_RTM 4_30_RTM 4_30_RTM 4_30_RTM 4_30_RTM 4_29_RTM 4_29_RTM 4_29_RTM 4_29_RTM   4_29_RTM 4_29_RTM 4_29_RTM 4_28_RTM 4_27_RTM 4_26_RTM 4_25_RTM   4_25_RTM   4_25_RTM   4_25_RTM 4_25_RTM   4_24_RTM   4_23_RTM   4_22_RTM   4_21_RTM 4_21_RTM   4_21_RTM 4_20_RTM   4_20_RTM 4_20_RTM   4_20_RTM 4_19_RTM 4_19_RTM   4_19_RTM)

typeset -i i=0 max=${#nss_versions[*]}
while (( i < max ))
do
    v=$(echo ${nss_versions[$i]} | sed 's/_RTM//g' | tr '_' '.')
    _docker build --build-arg NSS_VERSION=${nss_versions[$i]} --build-arg NSPR_VERSION=${nspr_versions[$i]} --build-arg VERSION=${v} -t ${DOCKER_REPOSITORY}nss-server:${v} --target nss-server .
    _docker build --build-arg NSS_VERSION=${nss_versions[$i]} --build-arg NSPR_VERSION=${nspr_versions[$i]} --build-arg VERSION=${v} -t ${DOCKER_REPOSITORY}nss-client:${v} --target nss-client .
    i=i+1
done

nss_versions=(3_35_RTM 3_34_1_RTM 3_33_RTM 3_32_1_RTM 3_31_1_RTM 3_30_2_RTM 3_29_5_RTM 3_28_1_RTM)
nspr_versions=(4_18_RTM 4_17_RTM 4_17_RTM 4_16_RTM 4_15_RTM 4_13_RTM 4_13_RTM 4_13_RTM)

typeset -i i=0 max=${#nss_versions[*]}
while (( i < max ))
do
    v=$(echo ${nss_versions[$i]} | sed 's/_RTM//g' | tr '_' '.')
    _docker build --build-arg NSS_VERSION=${nss_versions[$i]} --build-arg NSPR_VERSION=${nspr_versions[$i]} --build-arg VERSION=${v} -t ${DOCKER_REPOSITORY}nss-server:${v} --target nss-server -f Dockerfile_pre_3_36_8 .
    _docker build --build-arg NSS_VERSION=${nss_versions[$i]} --build-arg NSPR_VERSION=${nspr_versions[$i]} --build-arg VERSION=${v} -t ${DOCKER_REPOSITORY}nss-client:${v} --target nss-client -f Dockerfile_pre_3_36_8 .
    i=i+1
done


exit "$EXITCODE"
