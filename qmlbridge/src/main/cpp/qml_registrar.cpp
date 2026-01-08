// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

#include "qml_registrar.h"

#include "jni_context.h"
#include "jni_method_invoker.h"
#include "jni_object.h"
#include "jni_type.h"
#include "qobject_java_proxy.h"

#include <QtQml/qqml.h>
#include <QtQml/qqmlengine.h>
#include <QtQml/qqmlprivate.h>

#include <map>

using namespace Utility::JNI;

// Creation context are used when QML asks to instantiate a type;
// it stores necessary data to instantiate the user class and Qt Object.
struct CreationContext {
    std::string name; // For debugging
    qint64 cacheKey;
    jclass userClassGlobalRef = nullptr;
    jmethodID qtObjectConstructor = nullptr; // QtObject constructor
    jmethodID userObjectConstructor = nullptr; // Parameterless user-object constructor
    std::unique_ptr<QObjectJavaProxy> creationProxy; // Retain proxy to guarantee metaObject()
};

static void createQmlInstantiableType(void *memory, void *args);
static void creationHelper(QObjectJavaProxy *proxy, CreationContext *ctx, bool prototype = false);
static void registerQmlSingletonType(const char *typeName, const char *importName,
                                     std::shared_ptr<CreationContext> ctx,
                                     int major = 1, int minor = 0);
static void registerQmlInstantiableType(const char *typeName,
                                        const char *importName,
                                        std::shared_ptr<CreationContext> ctx,
                                        int major = 1, int minor = 0);
static void nativeRegisterQmlType(JNIEnv* env, jclass, jstring jname, jstring jmodule,
                                  jboolean isSingleton, jstring jQmlCompleteMethod,
                                  jclass userClass);

void QmlRegistrar::initializeJNI(JNIEnv *env) {
    JNINativeMethod methods[] = {
        JNIUtilities::createJNIMethod("nativeRegisterQmlType",
                "(Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;Ljava/lang/Class;)V",
                (void*)&nativeRegisterQmlType),
    };
    jclass javaClass = env->FindClass("org/qtproject/qt/bridge/core/QtQmlRegistration");
    env->RegisterNatives(javaClass, methods, std::size(methods));
    env->DeleteLocalRef(javaClass);
}

using CreationContextMap = std::map<std::string, std::shared_ptr<CreationContext>>;
Q_GLOBAL_STATIC(CreationContextMap, s_creationContexts);

void QmlRegistrar::clearRegistrations()
{
    if (s_creationContexts.isDestroyed())
        return;
    s_creationContexts->clear();
}


// Called from Java to register a QML type towards QML engine. Once registered, QML
// then knows which function to call when it needs to instantiate such a type. Types
// can be either singletons or regular instantiable types.
static void nativeRegisterQmlType(
    JNIEnv* env, jclass, jstring jname, jstring jmodule,
    jboolean isSingleton, jstring jQmlCompleteMethod, jclass userClass)
{
    const char *name = jname ? env->GetStringUTFChars(jname,   nullptr) : nullptr;
    const char *module = jmodule ? env->GetStringUTFChars(jmodule, nullptr) : nullptr;

    auto guard = qScopeGuard([&](){
        if (jname) env->ReleaseStringUTFChars(jname, name);
        if (jmodule) env->ReleaseStringUTFChars(jmodule, module);
    });

    // Key for storing creation contexts; use both type-name and module-name
    // in the key, as different modules may have same-name types
    const auto key = std::string(module ? module : "") + "::" + std::string(name ? name : "");
    const auto it = s_creationContexts->find(key);
    if (it != s_creationContexts->end()) {
        qWarning() << "Type" << key << "already registered";
        return;
    }

    // Creation context
    auto ctx = std::make_shared<CreationContext>();
    ctx->name = key;
    ctx->userClassGlobalRef = (jclass)env->NewGlobalRef(userClass);
    ctx->cacheKey = JNICache::ensureProxyClass(userClass);

    const auto qtObjectClass = JNIObject<JavaQtObject>::get();
    ctx->userObjectConstructor =
        env->GetMethodID(ctx->userClassGlobalRef, "<init>", "()V");
    // QtObject(Object userObject, jlong handle, boolean ownedByQml)
    ctx->qtObjectConstructor =
        env->GetMethodID(qtObjectClass, "<init>", "(Ljava/lang/Object;JZ)V");

    if (!ctx->qtObjectConstructor || !ctx->userObjectConstructor || env->ExceptionCheck()) {
        qWarning() << "Failed to resolve object constructors";
        env->ExceptionDescribe();
        env->ExceptionClear();
        return;
    }

    // Instantiate a user-object as a ~prototype so that the 'proxy' gets the proper
    // dynamic metaobject bits (== user-class signals, slots and properties). This may change
    // in future, see QTBUG-140439; the user-side object instantiation may have user-side
    // side-effects, and thus we shouldn't instantiate them here unnecessarily.
    ctx->creationProxy =  std::make_unique<QObjectJavaProxy>(ctx->cacheKey);
    creationHelper(ctx->creationProxy.get(), ctx.get(), true);

    if (isSingleton)
        registerQmlSingletonType(name, module, ctx);
    else
        registerQmlInstantiableType(name, module, ctx);

    // Keep contexts alive for type creations
    s_creationContexts->emplace(key, ctx);

    // Register @QMLComplete handler if present
    if (jQmlCompleteMethod)
        JNICache::registerQmlCompletionHandler(jQmlCompleteMethod, userClass);

}

// Helper function that does the actual instantiation when needed
static void creationHelper(QObjectJavaProxy *proxy, CreationContext *ctx, bool prototype)
{
    JNIEnv *env = JniContext::getEnv();
    // 1) New user-side object
    jobject userObjectLocal = env->NewObject(ctx->userClassGlobalRef, ctx->userObjectConstructor);
    if (!userObjectLocal || env->ExceptionCheck()) {
        qWarning() << "User object creation failed" << ctx->name;
        checkAndClearException(env);
    }

    // 2) New QtObject, takes also care of dynamic metaobject creation
    jobject qtObjectLocal = env->NewObject(JNIObject<JavaQtObject>::get(),
                                           ctx->qtObjectConstructor, userObjectLocal,
                                           jlong(proxy),
                                           true); // Owned by QML
    if (!qtObjectLocal || env->ExceptionCheck()) {
        qWarning() << "QtObject creation failed" << ctx->name;
        checkAndClearException(env);
    }

    // TODO QTBUG-140439. Leave java objects empty to allow early GC() of the
    // user java-object and the QtObject for those objects we create just for
    // getting the metaobject (i.e. as prototypes). But this is not ideal, preferably
    // this complexity could be avoided by removing the need to build the actual
    // prototypes.
    if (prototype) {
        env->DeleteLocalRef(userObjectLocal);
        env->DeleteLocalRef(qtObjectLocal);
        return;
    }

    // Associate the QObject proxy with user-side object so that QML-initiated calls
    // reach the user class (Java methods are resolved on the userobject)
    proxy->registerUserObject(userObjectLocal, true /* owned by QML */);

    // Keep a reference to the QtObject to prevent it from being garbage collected
    // (the proxy holds it, even if the object isn’t used directly by native code)
    proxy->registerQtObject(qtObjectLocal);

    env->DeleteLocalRef(userObjectLocal);
    env->DeleteLocalRef(qtObjectLocal);
}

static void registerQmlSingletonType(const char *typeName,
                                     const char *importName,
                                     std::shared_ptr<CreationContext> ctx,
                                     int major, int minor)
{
    // Called by QML when a singleton needs to be created, typically when a
    // singleton is accessed for the first time
    auto  createQmlSingletonType = [ctx](QQmlEngine *engine, QJSEngine *) -> QObject *{
        auto *proxy = new QObjectJavaProxy(ctx->cacheKey);
        creationHelper(proxy, ctx.get());
        return proxy;
    };

    QQmlPrivate::RegisterSingletonType type = {
        0, // struct version
        importName,
        QTypeRevision::fromVersion(major, minor),
        typeName,
        nullptr, // scriptApi creation function
        createQmlSingletonType,
        ctx->creationProxy->metaObject(),
        QQmlPrivate::QmlMetaType<QObjectJavaProxy>::self(),
        nullptr, // extensionObjectCreate
        nullptr, // extensionMetaObject
        QTypeRevision::zero()
    };
    QQmlPrivate::qmlregister(QQmlPrivate::SingletonRegistration, &type);
}

static void registerQmlInstantiableType(const char *typeName,
                                        const char *importName,
                                        std::shared_ptr<CreationContext> ctx,
                                        int major, int minor)
{
    const auto meta_object = ctx->creationProxy->metaObject();

    QQmlPrivate::RegisterType type = {
        QQmlPrivate::RegisterType::CurrentVersion,
        QQmlPrivate::QmlMetaType<QObjectJavaProxy>::self(),
        QQmlPrivate::QmlMetaType<QObjectJavaProxy>::list(),
        sizeof(QObjectJavaProxy),
        createQmlInstantiableType,
        ctx.get(),
        QString(),
        nullptr,
        importName,
        QTypeRevision::fromVersion(major, minor),
        typeName,
        meta_object,
        qmlAttachedPropertiesFunction(nullptr, meta_object),
        meta_object,
        QQmlPrivate::StaticCastSelector<QObjectJavaProxy, QQmlParserStatus>::cast(),
        QQmlPrivate::StaticCastSelector<QObjectJavaProxy, QQmlPropertyValueSource>::cast(),
        QQmlPrivate::StaticCastSelector<QObjectJavaProxy, QQmlPropertyValueInterceptor>::cast(),
        nullptr,
        nullptr,
        nullptr,
        QTypeRevision::zero(),
        QQmlPrivate::StaticCastSelector<QObjectJavaProxy, QQmlFinalizerHook>::cast(),
        {},
    };
    QQmlPrivate::qmlregister(QQmlPrivate::TypeRegistration, &type);
}

// Called by QML when an instantiable type needs to be created,
// typically when it encounters a MyType{} QML element
static void createQmlInstantiableType(void *memory, void *args)
{
    const auto ctx = static_cast<CreationContext*>(args);
    // We need to use placement allocator, ie. allocate the proxy object where QML tells us
    auto *proxy = new (memory) QObjectJavaProxy(ctx->cacheKey);
    creationHelper(proxy, ctx);
}
