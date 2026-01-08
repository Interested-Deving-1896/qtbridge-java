/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "jni_qml_application.h"

#include "converter.h"
#include "jni_utilities.h"
#include "qml_registrar.h"

#include <QtQml/qqmlcontext.h>
#include <QtQml/qqmlengine.h>

#include <QtQuickTest/quicktest.h>

#include <QtCore/qthread.h>
#include <QtCore/qurl.h>

void JNICALL nativeCreateQApplication(JNIEnv *env, jclass, jobjectArray argv)
{
    const jsize argc = env->GetArrayLength(argv);
    std::vector<std::string> arguments;

    for (jsize i = 0; i < argc; ++i) {
        jstring jstr = static_cast<jstring>(env->GetObjectArrayElement(argv, i));
        const char *cstr = jstr ? env->GetStringUTFChars(jstr, nullptr) : nullptr;
        arguments.emplace_back(cstr ? cstr : "");
        if (cstr)
            env->ReleaseStringUTFChars(jstr, cstr);
        if (jstr)
            env->DeleteLocalRef(jstr);
    }

    QMLApplication::initializeApp(arguments);
}


jboolean JNICALL nativeLoadQMLFile(JNIEnv *env, jclass, jstring fileName)
{
    if (!QMLApplication::instance()->ensureCanLoadQML())
        return JNI_FALSE;

    bool success = QMLApplication::instance()->loadQml(Utility::JNI::toQString(fileName));
    return success ? JNI_TRUE : JNI_FALSE;
}

jboolean JNICALL nativeLoadQMLContent(JNIEnv *env, jclass, jstring content)
{
    if (!QMLApplication::instance()->ensureCanLoadQML())
        return JNI_FALSE;

    bool success = QMLApplication::instance()->loadQmlContent(Utility::JNI::toQString(content));
    return success ? JNI_TRUE : JNI_FALSE;
}

void JNICALL nativeExecuteQApplication(JNIEnv *env, jclass)
{
    if (QMLApplication::instance()->ensureAppAndThreadThrowIfNot())
        QMLApplication::instance()->execute();
}

void JNICALL nativeQuit(JNIEnv *env, jclass)
{
    if (QMLApplication::instance()->ensureAppAndThreadThrowIfNot())
        QMLApplication::instance()->quitApp();
}

QMLApplication::QMLApplication(const std::vector<std::string> &arguments)
{
    m_argc = static_cast<int>(arguments.size());
    m_arguments = arguments;
    m_argv.resize(m_argc);
    for (int i = 0; i < m_argc; ++i)
        m_argv[i] = m_arguments[i].data();
    m_qapp = new QGuiApplication(m_argc, m_argv.data());
    m_qmlEngine = new QQmlApplicationEngine();
}

int executeTest(std::vector<std::string> arguments)
{
    std::vector<std::string> storage;
    storage.reserve(arguments.size() + 1);
    storage.emplace_back("qtbridgeqmltestrunner"); // argv[0] expected by quick_test_main()
    storage.insert(storage.end(), arguments.begin(), arguments.end()); // argv[1..]

    std::vector<char*> argv;
    argv.reserve(storage.size());
    for (auto &s : storage)
        argv.push_back(s.data());

    const int argc = static_cast<int>(argv.size());
    // 'sourceDir' parameter is passed as nullptr because we supply it with "-input" option in argv
    return quick_test_main(argc, argv.data(), "qtbridge-autotest", nullptr);
}

jint JNICALL nativeExecuteTest(JNIEnv *env, jclass, jobjectArray argv)
{
    const jsize argc = env->GetArrayLength(argv);
    std::vector<std::string> arguments;

    for (jsize i = 0; i < argc; ++i) {
        jstring jstr = static_cast<jstring>(env->GetObjectArrayElement(argv, i));
        const char *cstr = jstr ? env->GetStringUTFChars(jstr, nullptr) : nullptr;
        arguments.emplace_back(cstr ? cstr : "");
        if (cstr)
            env->ReleaseStringUTFChars(jstr, cstr);
        if (jstr)
            env->DeleteLocalRef(jstr);
    }
    return executeTest(arguments);
}

void initializeQuickApplicationJNI(JNIEnv *env)
{
    static JNINativeMethod methods[] = {
        JNIUtilities::createJNIMethod("nativeCreateApplication", "([Ljava/lang/String;)V",
                                      (void *) &nativeCreateQApplication),
        JNIUtilities::createJNIMethod("nativeExecuteApplication", "()V", (void *)
                                      &nativeExecuteQApplication),
        JNIUtilities::createJNIMethod("nativeQuitApplication", "()V", (void *) &nativeQuit),
        JNIUtilities::createJNIMethod("nativeLoadQMLFile", "(Ljava/lang/String;)Z",
                                      (void *) &nativeLoadQMLFile),
        JNIUtilities::createJNIMethod("nativeLoadQMLContent", "(Ljava/lang/String;)Z",
                                      (void *) &nativeLoadQMLContent)
    };
    jclass javaClass = env->FindClass("org/qtproject/qt/bridge/core/QtQuickApplication");
    env->RegisterNatives(javaClass, methods, std::size(methods));
    env->DeleteLocalRef(javaClass);
}

void initializeTestApplicationJNI(JNIEnv *env)
{
    static JNINativeMethod methods[] = {
        JNIUtilities::createJNIMethod("nativeExecuteTest", "([Ljava/lang/String;)I",
                                      (void *) &nativeExecuteTest),
    };
    jclass javaClass = env->FindClass("org/qtproject/qt/bridge/utils/QtAutotestApplication");
    env->RegisterNatives(javaClass, methods, std::size(methods));
    env->DeleteLocalRef(javaClass);
}

void QMLApplication::initializeJNI(JNIEnv *env)
{
    initializeQuickApplicationJNI(env);
    initializeTestApplicationJNI(env);
}

bool QMLApplication::ensureAppAndThreadThrowIfNot()
{
    if (!m_qapp) {
        JNIUtilities::throwIllegalStateException("QtQuickApplication is not initialized or has already been released");
        return false;
    }
    if (QThread::currentThread() != m_qapp->thread()) {
        JNIUtilities::throwIllegalStateException("QtQuickApplication must be accessed in the thread it was created");
        return false;
    }
    return true;
}

bool QMLApplication::ensureCanLoadQML()
{
    if (!ensureAppAndThreadThrowIfNot())
        return false;

    if (m_qmlLoaded) {
        JNIUtilities::throwIllegalStateException("Cannot load QML after QtQuickApplication has already loaded it");
        return false;
    }

    return true;
}

// Initializes the native Qt application singleton. Because we need to pass
// arguments and argv, the initialization is separate from instance(). This function
// must be called exactly once before any calls to instance().
void QMLApplication::initializeApp(const std::vector<std::string> &arguments)
{
    Q_ASSERT(!appInstance);
    appInstance.reset(new QMLApplication(arguments));
}

QMLApplication *QMLApplication::instance()
{
    Q_ASSERT(appInstance);
    return appInstance.get();
}

void QMLApplication::execute()
{
    Q_ASSERT(m_qapp);
    m_qapp->exec();
    // Release resources once execution has ended; applications
    // cannot be restarted. Also destructors would run in a thread
    // where we couldn't really delete QGuiApplication (or use
    // deleteLater / deferred delete on it)

    // First release creation contexts that may have prototype QObjects, and it's
    // better that they are destoyed in the right thread (this thread)
    QmlRegistrar::clearRegistrations();
    delete m_qmlEngine;
    delete m_qapp;
    m_qmlEngine = nullptr;
    m_qapp = nullptr;
}

void QMLApplication::quitApp() const
{
    Q_ASSERT(m_qapp);
    m_qapp->quit();
}

bool QMLApplication::loadQml(const QString &qmlPath)
{
    m_qmlEngine->load(QUrl::fromLocalFile(qmlPath));
    if (m_qmlEngine->rootObjects().isEmpty()) {
        m_qmlLoaded = false;
        qCritical() << "Failed to load QML file at path:" << qmlPath;
    } else {
        m_qmlLoaded = true;
    }
    return m_qmlLoaded;
}

bool QMLApplication::loadQmlContent(const QString &qmlContent)
{
    m_qmlEngine->loadData(qmlContent.toUtf8());
    if (m_qmlEngine->rootObjects().isEmpty()) {
        m_qmlLoaded = false;
        qCritical() << "Failed to load QML content";
    } else {
        m_qmlLoaded = true;
    }
    return m_qmlLoaded;
}
