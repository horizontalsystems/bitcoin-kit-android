cmake_minimum_required(VERSION 3.22.1)
project(mwebkit)

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(mwebkit SHARED src/main/cpp/mwebkit.cpp)

target_compile_options(mwebkit PRIVATE -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.)
target_link_options(mwebkit PRIVATE "-Wl,--build-id=none")

set(EXTERNAL_LIBS_DIR ${CMAKE_SOURCE_DIR}/external-libs)

############
# libmwebd: the MWEB daemon built by go/build.sh
############

# IMPORTED_NO_SONAME makes the linker record the plain file name as the
# dependency (libmwebd.so carries no SONAME), so the runtime loader finds it
# next to libmwebkit.so instead of looking for the build-time path.
add_library(mwebd SHARED IMPORTED)
set_target_properties(mwebd PROPERTIES
        IMPORTED_LOCATION ${EXTERNAL_LIBS_DIR}/${ANDROID_ABI}/libmwebd.so
        IMPORTED_NO_SONAME TRUE)

target_include_directories(mwebkit PRIVATE ${EXTERNAL_LIBS_DIR}/include)

find_library(log-lib log)

target_link_libraries(mwebkit mwebd ${log-lib})
