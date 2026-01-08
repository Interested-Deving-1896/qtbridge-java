# Copyright (C) 2025 The Qt Company Ltd.
# SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

cmake_minimum_required(VERSION 3.16)
project(QtBridgeNative VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(QT_NO_PRIVATE_MODULE_WARNING ON)

find_package(Qt6 REQUIRED COMPONENTS Core CorePrivate Gui Qml QuickTest)
find_package(JNI REQUIRED)

qt_standard_project_setup()

add_library(QtBridgeNative SHARED
    jni_object.h java_object.h jni_type.h
    jni_type_signature.h
    converter.cpp converter.h
    dynamic_metaobject.cpp dynamic_metaobject.h
    jni_context.h jni_context.cpp
    jni_qtlistmodel.cpp jni_qtlistmodel.h
    jni_qml_application.cpp jni_qml_application.h
    qobject_java_proxy.cpp qobject_java_proxy.h
    jni_qtobject.cpp jni_qtobject.h
    jni_utilities.cpp jni_utilities.h
    qtlistmodel.cpp qtlistmodel.h
    signature_helper.h
    qml_registrar.h qml_registrar.cpp
    jni_cache.h jni_cache.cpp
    jni_proxy_userobject_map.h jni_proxy_userobject_map.cpp
    jni_qmlchildren.h jni_qmlchildren.cpp
)

target_include_directories(QtBridgeNative
    PRIVATE ${JNI_INCLUDE_DIRS}
)

target_link_libraries(QtBridgeNative
    PRIVATE
        Qt6::Core
        Qt6::CorePrivate
        Qt6::Gui
        Qt6::Qml
        Qt6::QuickTest
)

target_compile_definitions(QtBridgeNative
    PRIVATE
        QT_NO_CAST_FROM_ASCII
        QT_NO_CAST_TO_ASCII
        QT_NO_CONTEXTLESS_CONNECT
)
