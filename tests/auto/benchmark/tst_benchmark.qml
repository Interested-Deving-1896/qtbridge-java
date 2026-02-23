/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

Item {
    TestCase {
        name: "Benchmarks"
        when: windowShown

        // For dynamic instantiations
        Component {
            id: myTypeComponent
            MyType {}
        }

        function init() {
            BenchmarkBackend.initialize();
        }
        function benchmark_list_appendItem() {
            BenchmarkBackend.appendItem("foo");
        }
        function benchmark_list_reset() {
            BenchmarkBackend.reset();
        }
        function benchmark_list_contains_true() {
            verify(BenchmarkBackend.contains("Item 1"));
        }
        function benchmark_list_contains_false() {
            verify(!BenchmarkBackend.contains("nonexistent"));
        }
        function benchmark_function_call_void() {
            BenchmarkBackend.voidFunction();
        }
        function benchmark_function_call_parameters() {
            BenchmarkBackend.parameterFunction(1, 2, "foo", "bar", 3, 4, "baz", 5);
        }
        function benchmark_function_call_returnValue() {
            let returnValue = BenchmarkBackend.returningFunction();
        }
        function benchmark_function_emiting_function() {
            BenchmarkBackend.emitingFunction();
        }
        function benchmark_property_readwrite() {
            BenchmarkBackend.stringProperty = "Hi"
            BenchmarkBackend.intProperty = 321
            compare(BenchmarkBackend.stringProperty, "Hi")
            compare(BenchmarkBackend.intProperty, 321)
        }
        function benchmark_instantiation() {
            // Instantiate more than one instance so that we measure cached operation
            for (let i = 0; i < 10; ++i)
                var instance = myTypeComponent.createObject();
        }
    }
}
