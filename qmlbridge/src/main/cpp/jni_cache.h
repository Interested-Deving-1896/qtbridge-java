// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

#ifndef JNI_CACHE_H
#define JNI_CACHE_H

#include <QtCore/qhash.h>

#include <jni.h>

class JNICache
{
public:
    struct JMethodEntry {
        jmethodID method;

        // Return type information
        bool retIsPrimitive = false;
        qint8 retShape = 0;
        qint8 retType = 0;

        // Parameter expectations
        QList<bool> parmIsPrimitive;
        QList<qint8> parmShape;
        QList<qint8> parmType;
    };

    struct JSignalEntry {
        int signalIndex;
        // Stores the metatype id of each parameter to support
        // faster type conversions when signals are emitted.
        QList<int> parmMetaTypeIds;
    };

    struct JFieldEntry {
        jfieldID field;
        // Property type information
        qint8 shape = 0;
        qint8 type = 0;
        bool isPrimitive = false;
    };

    struct JClassEntry {
        jclass globalClassRef;
        QHash<int, JMethodEntry> methods;
        QHash<int, JFieldEntry> fields;
        // Key is UTF-8 encoded signal Java signature, eg. "my€uroSignal(java.lang.Boolean)"
        QHash<QByteArray, JSignalEntry> signalz;
        // Key is UTF-8 encoded name:jniSignature, eg. "get:(I)Ljava/lang/Object;"
        QHash<QByteArray, JMethodEntry> methodsByName;
    };

    /*** TYPE-BASED API (for globally registered classes like JNIObject<Tag>)  ***/
    static bool registerGlobalClass(const QByteArray &className, jclass classRef);
    static jclass getGlobalClass(const QByteArray &className);
    static bool unregisterGlobalClass(const QByteArray &className);
    static bool isGlobalClassRegistered(const QByteArray &className);
    static JMethodEntry registerGlobalMethod(const QByteArray &className, const QByteArray &methodName,
                                     const QByteArray &signature, bool isStatic);
    static JMethodEntry getGlobalMethod(const QByteArray &className, const QByteArray &methodName,
                                        const QByteArray &signature, bool isStatic = false,
                                        bool autoRegister = true);

    /*** KEY-BASED API (for dynamically registered proxy classes) ***/
    static qint64 ensureProxyClass(jclass userProxyClass);
    static void registerProxyMethod(const qint64 proxyKey, const int methodKey,
                                   const QString &javaSignature, const QString &returnType,
                                   bool retIsPrimitive, qint8 retShape, qint8 retType,
                                   const QList<bool> &parmIsPrimitive,
                                   const QList<qint8> &parmShape,
                                   const QList<qint8> &parmType);
    static void registerProxySignal(qint64 proxyKey, int signalIndex, const QString &javaSignature,
                                    const QList<QByteArray> paramCppType);
    static void registerProxyField(qint64 proxyKey, int fieldKey, const QString &fieldName,
                                   const QString &signature,
                                   bool isPrimitive, qint8 shape, qint8 type);

    static std::optional<JMethodEntry> getProxyMethod(qint64 proxyKey, int methodKey);
    static std::optional<JSignalEntry> getProxySignal(qint64 proxyKey, const QByteArray &javaSignature);
    static JFieldEntry getProxyField(qint64 proxyKey, int fieldKey);

    static void registerQmlCompletionHandler(jstring methodName, jclass userClass);
    static std::optional<JMethodEntry> qmlCompletionHandler(qint64 proxyKey);

    static void clear();

private:
    // helper fucntions
    static jclass createGlobalRef(jclass localRef);
    static QByteArray makeMethodKey(const QByteArray &methodName, const QByteArray &signature);
    static jmethodID findMethod(jclass clazz, const QByteArray &name, const QByteArray &signature,
                                bool isStatic);
};

#endif
