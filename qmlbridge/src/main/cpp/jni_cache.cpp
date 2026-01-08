// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

#include "jni_cache.h"
#include "jni_context.h"

#include <QtCore/qdebug.h>
#include <QtCore/qmutex.h>

using namespace Qt::StringLiterals;

// Map javaType name to a JNI descriptor
static QString toJniType(QStringView javaType)
{
    const auto t = javaType.trimmed();

    // Arrays: "[" + element-type
    if (t.size() >= 2 && t.at(t.size() - 2) == u'[' && t.at(t.size() - 1) == u']')
        return QString(u'[') + toJniType(t.left(t.size() - 2));

    // Primitives
    if (t == u"boolean") return u"Z"_s;
    if (t == u"byte")    return u"B"_s;
    if (t == u"char")    return u"C"_s;
    if (t == u"short")   return u"S"_s;
    if (t == u"int")     return u"I"_s;
    if (t == u"long")    return u"J"_s;
    if (t == u"float")   return u"F"_s;
    if (t == u"double")  return u"D"_s;
    if (t == u"void")    return u"V"_s;

    // Reference type: Lpkg/ClassName;
    QString out;
    out.reserve(t.size() + 2);
    out += u'L';
    for (QChar ch : t)
        out += (ch == u'.') ? u'/' : ch;
    out += u';';
    return out;
}

static QString stripGenerics(const QString &type)
{
    int anglePos = type.indexOf('<'_L1);
    if (anglePos >= 0) {
        return type.left(anglePos).trimmed();
    }
    return type.trimmed();
}

static QByteArray toJniSignature(const QString &methodDecl,
                                 const QString &returnType, QString &methodNameOut)
{
    const auto parenOpen = methodDecl.indexOf('('_L1);
    const auto parenClose = methodDecl.lastIndexOf(')'_L1);

    if (parenOpen < 0 || parenClose < 0 || parenClose <= parenOpen) {
        qWarning() << "Invalid method declaration format:" << methodDecl;
        methodNameOut.clear();
        return {};
    }

    methodNameOut = methodDecl.left(parenOpen).trimmed();
    if (methodNameOut.isEmpty()) {
        qWarning() << "Empty method name in declaration:" << methodDecl;
        return {};
    }

    const auto paramsPart = methodDecl.mid(parenOpen + 1, parenClose - parenOpen - 1).trimmed();
    QStringList params;
    if (!paramsPart.isEmpty()) {
        params = paramsPart.split(','_L1, Qt::SkipEmptyParts);

        for (QString &param : params) {
            param = stripGenerics(param);
            if (param.isEmpty()) {
                qWarning() << "Empty parameter type in declaration:" << methodDecl;
            }
        }
    }
    QString jniParams;
    for (const auto &param : std::as_const(params)) {
        auto jniType = toJniType(param);
        if (jniType.isEmpty()) {
            qWarning() << "Failed to convert parameter type:" << param << "in" << methodDecl;
            return {};
        }
        jniParams += jniType;
    }
    const auto cleanReturnType = stripGenerics(returnType);
    auto jniReturn = toJniType(cleanReturnType);
    if (jniReturn.isEmpty()) {
        qWarning() << "Failed to convert return type:" << returnType << "in" << methodDecl;
        return {};
    }
    return "(%1)%2"_L1.arg(jniParams, jniReturn).toLatin1();
}

struct ClassCache
{
    QMutex mutex;
    QHash<qint64, JNICache::JClassEntry> proxyClasses;
    QHash<QByteArray, JNICache::JClassEntry> globalClasses;
    QHash<qint64, JNICache::JMethodEntry> qmlCompletionHandlers;
    qint64 nextProxyKey = 1;
};
Q_GLOBAL_STATIC(ClassCache, s_classCache)

bool JNICache::registerGlobalClass(const QByteArray &className, jclass classRef)
{
    if (className.isEmpty()) {
        qWarning() << "JNICache::registerGlobalClass: Invalid parameters";
        return false;
    }
    QMutexLocker locker(&s_classCache->mutex);
    if (s_classCache->globalClasses.contains(className))
        return true;

    const auto globalRef = createGlobalRef(classRef);
    JClassEntry entry;
    entry.globalClassRef = globalRef;
    s_classCache->globalClasses[className] = entry;
    return true;
}
jclass JNICache::getGlobalClass(const QByteArray &className)
{
    if (className.isEmpty())
        return nullptr;

    QMutexLocker locker(&s_classCache->mutex);
    const auto it = s_classCache->globalClasses.find(className);
    if (it != s_classCache->globalClasses.end())
        return it->globalClassRef;
    return nullptr;
}

bool JNICache::unregisterGlobalClass(const QByteArray &className)
{
    if (className.isEmpty())
        return false;
    if (s_classCache.isDestroyed())
        return true; // we assume it'd been unregistered it was destroyed

    QMutexLocker locker(&s_classCache->mutex);
    const auto it = s_classCache->globalClasses.find(className);
    if (it == s_classCache->globalClasses.end())
        return false;
    if (it->globalClassRef)
        JniContext::getEnv()->DeleteGlobalRef(it->globalClassRef);
    s_classCache->globalClasses.erase(it);
    return true;
}
bool JNICache::isGlobalClassRegistered(const QByteArray &className)
{
    if (className.isEmpty())
        return false;

    QMutexLocker locker(&s_classCache->mutex);
    return s_classCache->globalClasses.contains(QByteArray(className));
}
JNICache::JMethodEntry JNICache::registerGlobalMethod(const QByteArray &className,
                                                      const QByteArray &methodName,
                                                      const QByteArray &signature, bool isStatic)
{
    if (className.isEmpty() || methodName.isEmpty() || signature.isEmpty()) {
        qWarning() << "JNICache::registerGlobalMethod: Invalid parameters";
        return{};
    }

    QMutexLocker locker(&s_classCache->mutex);
    const auto it = s_classCache->globalClasses.find(className);
    if (it == s_classCache->globalClasses.end()) {
        qWarning() << "JNICache::registerGlobalMethod: Class not registered:" << className;
        return {};
    }

    const QByteArray nameKey = makeMethodKey(methodName, signature);

    if (it->methodsByName.contains(nameKey))
        return it->methodsByName[nameKey];

    const auto method = findMethod(it->globalClassRef, methodName, signature, isStatic);
    if (!method)
        return {};

    JMethodEntry entry;
    entry.method = method;
    it->methodsByName[nameKey] = entry;

    return entry;
}
JNICache::JMethodEntry JNICache::getGlobalMethod(const QByteArray &className,
                                                 const QByteArray &methodName,
                                                 const QByteArray &signature,
                                                 const bool isStatic, const bool autoRegister)
{
    QMutexLocker locker(&s_classCache->mutex);
    const auto classIt = s_classCache->globalClasses.find(className);

    if (classIt == s_classCache->globalClasses.end())
        return {};

    const auto nameKey = makeMethodKey(methodName, signature);
    const auto methodIt = classIt->methodsByName.find(nameKey);
    if (methodIt == classIt->methodsByName.end()) {
        if (!autoRegister)
            return {};
        locker.unlock();
        return registerGlobalMethod(className, methodName, signature, isStatic);
    }
    return methodIt.value();
}

qint64 JNICache::ensureProxyClass(jclass userProxyClass)
{
    Q_ASSERT(userProxyClass != nullptr);
    QMutexLocker locker(&s_classCache->mutex);
    const auto env = JniContext::getEnv();

    // Check if class already exists in cache
    for (auto &&[key, entry] : s_classCache->proxyClasses.asKeyValueRange()) {
        if (env->IsSameObject(entry.globalClassRef, userProxyClass))
            return key;
    }

    // Not found, create new entry
    const auto newKey = s_classCache->nextProxyKey++;
    auto &entry = s_classCache->proxyClasses[newKey];
    entry.globalClassRef = createGlobalRef(userProxyClass);
    entry.methods.clear();
    entry.fields.clear();
    return newKey;
}

void JNICache::registerProxyMethod(const qint64 proxyKey, const int methodKey,
                                   const QString &javaSignature, const QString &returnType,
                                   bool retIsPrimitive, const QList<bool> &parmIsPrimitive)
{
    QMutexLocker locker(&s_classCache->mutex);
    const auto classIt = s_classCache->proxyClasses.find(proxyKey);
    if (classIt == s_classCache->proxyClasses.end()) {
        qWarning() << "registerProxyMethod: proxyKey not found in cache:" << proxyKey;
        return;
    }

    auto &classEntry = classIt.value();
    if (!classEntry.globalClassRef) {
        qWarning() << "registerProxyMethod: class for proxyKey" << proxyKey
                   << "has null globalClazzRef";
        return;
    }

    QString methodName;
    const auto jniSignature = toJniSignature(javaSignature, returnType, methodName);

    if (jniSignature.isEmpty() || methodName.isEmpty()) {
        qWarning() << "Failed to parse method declaration:" << javaSignature;
        return;
    }
    const auto env = JniContext::getEnv();
    const auto methodId =
            env->GetMethodID(classEntry.globalClassRef, methodName.toUtf8().constData(),
                             jniSignature.constData());
    if (!methodId) {
        qWarning() << " Failed to find method:" << methodName
                   << " with signature: " << jniSignature;
        return;
    }
    classEntry.methods.insert(methodKey, JMethodEntry{methodId, retIsPrimitive, parmIsPrimitive});
}

void JNICache::registerProxySignal(qint64 proxyKey, int signalIndex,
                                   const QString &javaSignature,
                                   const QList<QByteArray> paramCppType)
{
    QMutexLocker locker(&s_classCache->mutex);
    const auto classIt = s_classCache->proxyClasses.find(proxyKey);
    if (classIt == s_classCache->proxyClasses.end()) {
        qWarning() << "registerProxyMethod: proxyKey not found in cache:" << proxyKey;
        return;
    }

    auto &classEntry = classIt.value();
    if (!classEntry.globalClassRef) {
        qWarning() << "registerProxyMethod: class for proxyKey" << proxyKey
                   << "has null globalClazzRef";
        return;
    }

    QList<int> parmMetaTypeIds;
    for (const auto &param : paramCppType) {
        // Get metatype ID. Currently all parameters are in the known ID range
        // (i.e. not in the user range). Therefore we could hard-code these already
        // at KSP handling level too, but for now let's retain flexibility; in case
        // we encounter a use case for custom metatypes, then we can handle those here
        // as well.
        QMetaType mt = QMetaType::fromName(param);
        if (mt.isValid())
            parmMetaTypeIds.push_back(mt.id());
        else
            qWarning("Unsupported parameter type %s in %s ",
                     param.constData(), qPrintable(javaSignature));
    }
    classEntry.signalz.insert(javaSignature.toUtf8(),
                              JSignalEntry{signalIndex, parmMetaTypeIds});
}

void JNICache::registerProxyField(const qint64 proxyKey, const int fieldKey,
                                  const QString &fieldName, const QString &signature)
{
    QMutexLocker locker(&s_classCache->mutex);
    const auto classIt = s_classCache->proxyClasses.find(proxyKey);
    if (classIt == s_classCache->proxyClasses.end()) {
        qWarning() << "registerProxyField: proxyKey not found in cache:" << proxyKey;
        return;
    }
    auto &classEntry = classIt.value();
    if (!classEntry.globalClassRef) {
        qWarning() << "registerProxyField: class for proxyKey" << proxyKey
                   << "has null globalClassRef";
        return;
    }

    const auto jniSignature = toJniType(signature);
    if (fieldName.isEmpty() || jniSignature.isEmpty()) {
        qWarning() << "Invalid field name or signature:" << fieldName << signature;
        return;
    }
    const auto env = JniContext::getEnv();
    const auto fieldId = env->GetFieldID(classEntry.globalClassRef, fieldName.toUtf8().constData(),
                                         jniSignature.toUtf8().constData());
    if (!fieldId) {
        qWarning() << "Failed to find field:" << fieldName << "with signature:" << jniSignature;
        return;
    }
    classEntry.fields.insert(fieldKey, JFieldEntry{fieldId});
}

std::optional<JNICache::JMethodEntry> JNICache::getProxyMethod(
    const qint64 proxyKey, const int methodKey)
{
    QMutexLocker locker(&s_classCache->mutex);
    const auto classIt = s_classCache->proxyClasses.find(proxyKey);
    if (classIt == s_classCache->proxyClasses.end())
        return std::nullopt;

    const auto &methods = classIt->methods;
    const auto methodIt = methods.find(methodKey);
    if (methodIt == methods.end())
        return std::nullopt;

    return methodIt.value();
}

std::optional<JNICache::JSignalEntry> JNICache::getProxySignal(
    qint64 proxyKey, const QByteArray &javaSignature)
{
    QMutexLocker locker(&s_classCache->mutex);
    const auto classIt = s_classCache->proxyClasses.find(proxyKey);
    if (classIt == s_classCache->proxyClasses.end())
        return std::nullopt;

    const auto &signalz = classIt->signalz;
    const auto methodIt = signalz.find(javaSignature);
    if (methodIt == signalz.end())
        return std::nullopt;

    return methodIt.value();
}

JNICache::JFieldEntry JNICache::getProxyField(const qint64 proxyKey, const int fieldKey)
{
    QMutexLocker locker(&s_classCache->mutex);
    const auto classIt = s_classCache->proxyClasses.find(proxyKey);
    if (classIt == s_classCache->proxyClasses.end())
        return {};

    const auto &fields = classIt->fields;
    const auto fieldIt = fields.find(fieldKey);
    if (fieldIt == fields.constEnd())
        return {};
    return fieldIt.value();
}

void JNICache::registerQmlCompletionHandler(jstring methodName, jclass userClass)
{
    if (s_classCache.isDestroyed())
        return;

    if (!methodName || ! userClass) {
        qWarning() << "QML Completion handler name or class missing";
        return;
    }

    const auto env = JniContext::getEnv();
    const char *name = methodName ? env->GetStringUTFChars(methodName, nullptr) : nullptr;
    auto guard = qScopeGuard([&](){
        if (name) env->ReleaseStringUTFChars(methodName, name);
    });

    const jmethodID methodID = env->GetMethodID(userClass, name, "()V");
    if (!methodID) {
        qWarning() << "QML Completion method not found:" << name;
        return;
    }

    // Ensure we have a proxy id for this class and store the method
    const qint64 proxyKey = JNICache::ensureProxyClass(userClass);
    QMutexLocker locker(&s_classCache->mutex);
    s_classCache->qmlCompletionHandlers.insert(proxyKey, JMethodEntry{ methodID });
}

std::optional<JNICache::JMethodEntry> JNICache::qmlCompletionHandler(qint64 proxyKey)
{
    if (s_classCache.isDestroyed())
        return std::nullopt;

    QMutexLocker locker(&s_classCache->mutex);
    const auto it = s_classCache->qmlCompletionHandlers.constFind(proxyKey);
    if (it == s_classCache->qmlCompletionHandlers.cend())
        return std::nullopt;

    return *it;
}

void JNICache::clear()
{
    if (s_classCache.isDestroyed())
        return;
    QMutexLocker locker(&s_classCache->mutex);
    for (const auto &it: std::as_const(s_classCache->proxyClasses))
        JniContext::getEnv()->DeleteGlobalRef(it.globalClassRef);
    s_classCache->proxyClasses.clear();
    for (const auto &it: std::as_const(s_classCache->globalClasses))
        JniContext::getEnv()->DeleteGlobalRef(it.globalClassRef);
    s_classCache->globalClasses.clear();
}

jclass JNICache::createGlobalRef(const jclass localRef)
{
    if (!localRef)
        return nullptr;

    const auto globalRef = reinterpret_cast<jclass>(JniContext::getEnv()->NewGlobalRef(localRef));
    if (!globalRef) {
        qWarning() << "JNICache: Failed to create global reference";
        return nullptr;
    }
    return globalRef;
}

QByteArray JNICache::makeMethodKey(const QByteArray &methodName, const QByteArray &signature)
{
    return methodName + ":" + signature;
}

jmethodID JNICache::findMethod(jclass clazz, const QByteArray &name,
                               const QByteArray &signature,
                               const bool isStatic)
{
    if (!clazz || name.isEmpty() ||signature.isEmpty())
        return nullptr;

    const auto method = isStatic
        ? JniContext::getEnv()->GetStaticMethodID(clazz, name, signature)
        : JniContext::getEnv()->GetMethodID(clazz, name, signature);

    if (!method) {
        if (JniContext::getEnv()->ExceptionCheck())
            JniContext::getEnv()->ExceptionClear();
        qWarning() << "JNICache: Method not found:" << name << signature;
    }
    return method;
}
