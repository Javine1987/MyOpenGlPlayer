cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall")

add_library(gl2jni SHARED jni_bridge.cpp)

add_subdirectory(glm)

target_link_libraries(gl2jni android log EGL GLESv2)
