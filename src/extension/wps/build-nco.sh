#!/bin/bash

declare -r NCO_VERSION=4.3.4
declare -r NCO_SRC=http://dust.ess.uci.edu/nco/src/nco_${NCO_VERSION}.orig.tar.gz
declare -r DEBIAN_CONTROL_SRC=http://dust.ess.uci.edu/nco/src/nco_${NCO_VERSION}-1.debian.tar.gz

# main
main() {
    nco_src_basename=`basename $NCO_SRC`
    debian_src_basename=`basename $DEBIAN_CONTROL_SRC`

    rm -rf nco-$NCO_VERSION
    rm -f $nco_src_basename $debian_src_basename

    # retrieve
    wget $NCO_SRC
    wget $DEBIAN_CONTROL_SRC

    # extract
    tar -xf $nco_src_basename && \
        cd nco-$NCO_VERSION && \
        tar -xf ../$debian_src_basename

    # build!
    dpkg-buildpackage -B -nc
}

main "$@"
