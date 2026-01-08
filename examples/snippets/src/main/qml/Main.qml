//
// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
//

import QtQuick
import QtQuick.Controls
import QtQml.Models

import QtBridge

// Simple application for serving documentation snippets
// @start region="qmlapplication-full"
ApplicationWindow {
    id: root
    height: 300
    visible: true
    width: 200

    // This is a creatable (non-singleton) type. They are declared here in QML document:
    MyType {
        id: myInstance
        onSomethingHappened: () => { console.log("Something happened on creatable type")}
        // Elements can also have children which can be queried at Java-side
        MyChildType {
            id: myChildInstance1
        }
        MyChildType {
            id: myChildInstance2
        }
    }

    // This refers to a singleton class; they are not declared here in QML document,
    // but a (single) singleton instance is automatically created on first use
    Connections {
        function onSomethingHappened() {
            console.log("Something happened on singleton");
        }
        target: MySingleton
    }

    Column {
        ListView {
            id: view

            height: root.width
            model: myInstance.numbers // MyType 'numbers' used as model
            width: 100

            delegate: Button {
                id: delegate
                required property var model
                height: 40
                text: "Increment " + model.value
                width: view.width
                onClicked: delegate.model.value += 1 // Updating a value in model
            }
        }

        Button {
            height: 40
            text: myInstance.greeting
            width: root.width

            onClicked: {
                // Updating creatable MyType's property
                myInstance.greeting = "Howdy";
                // Updating singleton MySingleton's property:
                MySingleton.greeting = "Howdy";
            }
        }

        Button {
            height: 40
            text: "Do something"
            width: root.width
            onClicked: {
                // Calling creatable MyType's function
                myInstance.doSomething();
                // Calling singleton MySingleton's function
                MySingleton.doSomething();
            }
        }
    }
}
// @end
