//
// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
//

import QtQuick
import QtQuick.Controls

import Animals // Cat, Dog
import Two.Wheels // Bike
import Four.Wheels.Vehicle // Car

ApplicationWindow {
    id: root
    height: 400
    width: 600
    visible: true

    Cat {
        id: cat
        onMeowed: midText.text += "meowed\n"
    }
    Dog {
        id: dog
        onBarked: midText.text += "barked\n"
    }
    Bike {
        id: bike
        onPedalled: midText.text += "pedalled\n"
    }
    Car {
        id: car
        onRoared: midText.text += "roared\n"
    }

    Row {
        anchors.fill: parent
        Column {
            width: root.width / 3
            height: root.height
            Button {
                text: "Test"
                onClicked: {
                    cat.meow()
                    dog.bark()
                    bike.pedal()
                    car.roar()
                }
            }
        }
        Rectangle {
            id: midInfo
            width: root.width / 3
            height: root.height
            color: "lightsteelblue"
            Text {
                id: midText
            }
        }
    }
}
