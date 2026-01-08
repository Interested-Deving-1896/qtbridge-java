/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef SIGNATURE_HELPER_H
#define SIGNATURE_HELPER_H

#include "jni_type_signature.h"

namespace Utility::JNI
{
    class SignatureHelper
    {
    public:
        template<typename ReturnType, typename... Args>
        static constexpr auto buildMethodJniSignature() {
            constexpr auto paramSig = buildParamsJniSignature<Args...>();
            constexpr auto retSig= JNISignature<ReturnType>::value();
            return paramSig + retSig;
        }
    private:

        template<typename... Args>
        static constexpr auto buildParamsJniSignature() {
            return makeConstexpr("(")+ buildParamList<Args...>() + makeConstexpr(")");
        }
        template <typename... Args>
        static constexpr auto buildParamList() {
            if constexpr (sizeof...(Args) == 0)
                return makeConstexpr("");
            return (makeConstexpr("") + ... + JNISignature<Args>::value());
        }
    };
} // namespace Utility::JNI

#endif // SIGNATURE_HELPER_H
