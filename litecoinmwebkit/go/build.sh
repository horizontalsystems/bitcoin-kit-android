#!/usr/bin/env bash
# Builds libmwebd.so (the MWEB daemon as a shared library with a C interface)
# for every Android ABI the kit ships, into ../external-libs/<abi>/. Go has no
# c-archive build mode for Android, so the daemon is a shared library that the
# JNI shim (src/main/cpp) links against and Gradle packages from external-libs.
#
#   ./build.sh            prepare patched sources and build all ABIs
#   ./build.sh prepare    only prepare build/neutrino and build/mwebd
#   ./build.sh host       prepare and build cmd/mwebd for this machine
#
# Requirements: Go (the version pinned by go.mod's toolchain line is fetched
# automatically), Android NDK r27 at $ANDROID_NDK_HOME or
# $ANDROID_HOME/ndk/27.0.12077973, and `patch`.
set -euo pipefail

cd "$(dirname "$0")"

NDK_VERSION=27.0.12077973
API=23
ABIS=(arm64-v8a armeabi-v7a x86_64)
EXTERNAL_LIBS=../external-libs
PATCHES=$(cd .. && pwd)/patches

mod_dir() {
    go list -m -json "$1" 2>/dev/null | sed -n 's/.*"Dir": "\(.*\)",*/\1/p'
}

# prepare copies the pinned upstream modules out of the module cache into
# build/ and applies the patches from ../patches. go.mod replaces the two
# modules with these copies.
prepare() {
    rm -rf build
    mkdir -p build
    for mod in neutrino mwebd; do
        version=$(sed -n "s|.*github.com/ltcmweb/$mod \(v[0-9.]*\)$|\1|p" go.mod | head -1)
        GOFLAGS=-mod=mod go mod download "github.com/ltcmweb/$mod@$version"
        src=$(GOFLAGS=-mod=mod go mod download -json "github.com/ltcmweb/$mod@$version" | sed -n 's/.*"Dir": "\(.*\)",*/\1/p')
        cp -R "$src" "build/$mod"
        chmod -R u+w "build/$mod"
        for p in "$PATCHES/$mod"/*.patch; do
            echo "applying $(basename "$p") to $mod $version"
            (cd "build/$mod" && patch -p1 --no-backup-if-mismatch --forward < "$p")
        done
    done
    go mod verify
}

ndk_root() {
    if [ -n "${ANDROID_NDK_HOME:-}" ]; then echo "$ANDROID_NDK_HOME"; return; fi
    local sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}
    echo "$sdk/ndk/$NDK_VERSION"
}

build_abi() {
    local abi=$1 goarch goarm= triple
    case $abi in
        arm64-v8a)   goarch=arm64; triple=aarch64-linux-android ;;
        armeabi-v7a) goarch=arm; goarm=7; triple=armv7a-linux-androideabi ;;
        x86_64)      goarch=amd64; triple=x86_64-linux-android ;;
        *) echo "unknown abi $abi" >&2; exit 1 ;;
    esac
    local ndk host out
    ndk=$(ndk_root)
    host=$(ls "$ndk/toolchains/llvm/prebuilt")
    out=$EXTERNAL_LIBS/$abi
    mkdir -p "$out"
    echo "building $abi"
    CGO_ENABLED=1 GOOS=android GOARCH=$goarch GOARM=$goarm \
    CC="$ndk/toolchains/llvm/prebuilt/$host/bin/${triple}${API}-clang" \
    CGO_CFLAGS="-O2 -ffile-prefix-map=$PWD=." \
    go build -trimpath -buildvcs=false -buildmode=c-shared \
        -ldflags="-s -w -buildid= -extldflags=-Wl,-z,max-page-size=16384,--build-id=none" \
        -o "$out/libmwebd.so" .
    # The generated header is replaced by the hand-written
    # external-libs/include/mwebd.h, which uses plain C types only.
    rm -f "$out/libmwebd.h"
    ls -l "$out/libmwebd.so"
}

case "${1:-all}" in
    prepare) prepare ;;
    host)    prepare; go build -trimpath -o build/mwebd-host ./cmd/mwebd; ls -l build/mwebd-host ;;
    all)     prepare; for abi in "${ABIS[@]}"; do build_abi "$abi"; done ;;
    abi)     build_abi "$2" ;;
    *) echo "usage: $0 [prepare|host|all|abi <abi>]" >&2; exit 1 ;;
esac
