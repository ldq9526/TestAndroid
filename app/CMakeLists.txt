cmake_minimum_required(VERSION 3.4.1)

set(OpenCV_STATIC ON)
set(OpenCV_DIR "D:/Android/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED)

add_library(native-lib SHARED src/main/cpp/native-lib.cpp)

find_library(log-lib log)

target_link_libraries(native-lib jnigraphics ${OpenCV_LIBS} ${log-lib})