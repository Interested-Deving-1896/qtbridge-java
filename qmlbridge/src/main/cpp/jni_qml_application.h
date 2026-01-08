/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef JNI_QML_APPLICATION_H
#define JNI_QML_APPLICATION_H

#include <QtQml/qqmlapplicationengine.h>

#include <QtGui/qguiapplication.h>

#include <QtCore/qstring.h>

#include <jni.h>

class QMLApplication
{
public:
    static QMLApplication *instance();
    static void initializeApp(const std::vector<std::string>& arguments);
    static void initializeJNI(JNIEnv *env);

    bool ensureAppAndThreadThrowIfNot();
    bool ensureCanLoadQML();
    bool loadQml(const QString &qmlPath);
    bool loadQmlContent(const QString &qmlContent);
    void execute();

    void quitApp() const;

private:
    inline static std::unique_ptr<QMLApplication> appInstance;

    QMLApplication(const std::vector<std::string> &arguments);
    QGuiApplication *m_qapp;
    QQmlApplicationEngine *m_qmlEngine;
    int m_argc = 0;
    std::vector<char*> m_argv;
    std::vector<std::string> m_arguments;

    bool m_qmlLoaded;
};
#endif // JNI_QML_APPLICATION_H
