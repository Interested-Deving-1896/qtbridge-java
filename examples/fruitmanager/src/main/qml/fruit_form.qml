//
// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
//

import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQml.Models

import QtBridge

ApplicationWindow {
    id: root
    width: 880
    height: 640
    visible: true
    title: qsTr("Fruit Manager")

    // ---- Shared layout constants ----
    readonly property int labelColWidth: 120

    // ---- Status/notifications ----
    Timer {
        id: clearStatusTimer
        interval: 2500
        onTriggered: statusDisplay.text = ""
    }

    function notifyStatus(message, color = "green") {
        statusDisplay.color = color
        statusDisplay.text = message
        clearStatusTimer.restart()
    }

    function handleError(errorMessage) {
        root.notifyStatus(errorMessage, "orangered")
    }

    function handleSuccess(successMessage) {
        root.notifyStatus(successMessage, "green")
    }

    // ---- Backend connections ----
    Connections {
        target: Backend

        function onValidationError(message) {
            root.handleError(message)
        }

        function onDuplicateError(duplicate) {
            root.handleError(qsTr("List already contains entry '%1'!").arg(duplicate))
        }

        function onOperationError(message) {
            root.handleError(message)
        }

        function onFruitAdded(fruitName) {
            root.handleSuccess(qsTr("Fruit '%1' added successfully.").arg(fruitName))
            resetAddForm()
        }

        function onFruitUpdated(fruitName) {
            root.handleSuccess(qsTr("Fruit '%1' updated successfully.").arg(fruitName))
        }

        function onFruitDeleted(fruitName) {
            root.handleSuccess(qsTr("Fruit '%1' deleted successfully.").arg(fruitName))
        }
    }

    // Helper function to reset the add form
    function resetAddForm() {
        nameField.clear()
        tropicalCheck.checked = false
        caloriesField.text = "0"
        weightField.text = "0.0"
        colorBox.currentIndex = Math.max(0, colorBox.model.indexOf("PINK"))
        categoryBox.currentIndex = Math.max(0, categoryBox.model.indexOf("OTHER"))
    }

    property var colorOptions: Backend.colorOptions
    property var categoryOptions: Backend.categoryOptions

    // ---- Header ----
    header: ToolBar {
        RowLayout {
            anchors.fill: parent
            Label {
                text: qsTr("Fruit Manager")
                font.pixelSize: 18
                font.bold: true
                Layout.fillWidth: true
                Layout.alignment: Qt.AlignVCenter
            }
        }
    }

    // ---- Main content ----
    ColumnLayout {
        id: mainLayout
        anchors.fill: parent
        anchors.margins: 12
        spacing: 12

        // KPI row
        RowLayout {
            Layout.fillWidth: true
            spacing: 16
            Label {
                text: qsTr("Total fruits: %1").arg(lv.count ?? "0")
                color: "forestgreen"
                font.bold: true
            }
        }

        // List of fruits
        Frame {
            Layout.fillWidth: true
            Layout.fillHeight: true
            padding: 0
            clip: true

            ListView {
                id: lv
                anchors.fill: parent
                model: Backend.fruitList
                spacing: 4
                boundsBehavior: Flickable.StopAtBounds

                delegate: ItemDelegate {
                    id: ld
                    required property int index
                    required property var model

                    width: lv.width
                    hoverEnabled: true

                    contentItem: RowLayout {
                        anchors.fill: parent
                        anchors.margins: 8
                        spacing: 12

                        // Accent color
                        Rectangle {
                            width: 10
                            radius: 4
                            color: ld.model.color.hex
                            Layout.alignment: Qt.AlignVCenter
                            Layout.preferredHeight: 44
                        }

                        // Name + badges + nutrition info
                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 4

                            RowLayout {
                                spacing: 6
                                Layout.fillWidth: true

                                Label {
                                    text: `#${index + 1}  ${ld.model.name ?? ""}`
                                    font.bold: true
                                    elide: Text.ElideRight
                                    Layout.alignment: Qt.AlignVCenter
                                    Layout.fillWidth: true
                                }

                                // Category badge
                                Rectangle {
                                    radius: 6
                                    color: "#ECEFF1"
                                    border.color: "#B0BEC5"
                                    Layout.alignment: Qt.AlignVCenter
                                    Layout.preferredHeight: 22
                                    Layout.minimumWidth: 50
                                    Layout.maximumWidth: 100
                                    RowLayout {
                                        anchors.fill: parent
                                        anchors.margins: 6
                                        Label {
                                            text: ld.model.category.name ?? "OTHER"
                                            color: "#37474F"
                                            font.pointSize: 10
                                            verticalAlignment: Text.AlignVCenter
                                        }
                                    }
                                }

                                // Tropical badge
                                Rectangle {
                                    visible: ld.model.isTropical
                                    radius: 6
                                    color: "#E8F5E9"
                                    border.color: "#81C784"
                                    Layout.alignment: Qt.AlignVCenter
                                    Layout.preferredHeight: 22
                                    Layout.minimumWidth: 60
                                    Layout.maximumWidth: 100
                                    RowLayout {
                                        anchors.fill: parent
                                        anchors.margins: 6
                                        Label {
                                            text: qsTr("Tropical")
                                            color: "#2E7D32"
                                            font.pointSize: 10
                                            verticalAlignment: Text.AlignVCenter
                                        }
                                    }
                                }
                            }

                            RowLayout {
                                spacing: 14
                                Layout.alignment: Qt.AlignVCenter
                                Label {
                                    text: qsTr("Calories: %1 kcal").arg(ld.model.nutrition.calories ?? 0)
                                    color: "#546E7A"
                                }
                                Label {
                                    text: qsTr("Weight: %1 g").arg((ld.model.nutrition.weight ?? 0.0).toFixed(1))
                                    color: "#546E7A"
                                }
                            }
                        }

                        // Actions column
                        RowLayout {
                            spacing: 8
                            Layout.alignment: Qt.AlignVCenter
                            ToolButton {
                                icon.name: "edit"; text: qsTr("Edit")
                                onClicked: editDialog.openFor(index, {
                                    name: ld.model.name,
                                    color: ld.model.color,
                                    category: ld.model.category,
                                    isTropical: ld.model.isTropical,
                                    calories: ld.model.nutrition.calories,
                                    weight: ld.model.nutrition.weight
                                })
                            }
                            ToolButton {
                                icon.name: "delete"; text: qsTr("Delete")
                                onClicked: Backend.deleteFruit(index)
                            }
                        }
                    }
                }

                ScrollBar.vertical: ScrollBar { }
            }
        }

        // Add fruit form
        GroupBox {
            title: qsTr("Add fruit")
            Layout.fillWidth: true

            ColumnLayout {
                id: addForm
                Layout.fillWidth: true
                spacing: 8

                // Name
                RowLayout {
                    Layout.fillWidth: true
                    Label {
                        text: qsTr("Name")
                        horizontalAlignment: Text.AlignRight
                        Layout.preferredWidth: root.labelColWidth
                    }
                    TextField {
                        id: nameField
                        placeholderText: qsTr("e.g., Mango")
                        Layout.fillWidth: true
                        onAccepted: addButton.clicked()
                    }
                }

                // Color
                RowLayout {
                    Layout.fillWidth: true
                    Label {
                        text: qsTr("Color")
                        horizontalAlignment: Text.AlignRight
                        Layout.preferredWidth: root.labelColWidth
                    }
                    ComboBox {
                        id: colorBox
                        model: root.colorOptions
                        Layout.fillWidth: true
                        currentIndex: model.indexOf("PINK") >= 0 ? model.indexOf("PINK") : 0
                    }
                }

                // Category
                RowLayout {
                    Layout.fillWidth: true
                    Label {
                        text: qsTr("Category")
                        horizontalAlignment: Text.AlignRight
                        Layout.preferredWidth: root.labelColWidth
                    }
                    ComboBox {
                        id: categoryBox
                        model: root.categoryOptions
                        Layout.fillWidth: true
                        currentIndex: model.indexOf("OTHER") >= 0 ? model.indexOf("OTHER") : 0
                    }
                }

                // Tropical
                RowLayout {
                    Layout.fillWidth: true
                    Label {
                        text: qsTr("Tropical")
                        horizontalAlignment: Text.AlignRight
                        Layout.preferredWidth: root.labelColWidth
                    }
                    CheckBox {
                        id: tropicalCheck
                    }
                    Item { Layout.fillWidth: true }
                }

                // Calories
                RowLayout {
                    Layout.fillWidth: true
                    Label {
                        text: qsTr("Calories")
                        horizontalAlignment: Text.AlignRight
                        Layout.preferredWidth: root.labelColWidth
                    }
                    TextField {
                        id: caloriesField
                        placeholderText: "0"
                        text: "0"
                        inputMethodHints: Qt.ImhDigitsOnly
                        validator: IntValidator { bottom: 0; top: 5000 }
                        Layout.preferredWidth: 140
                        onAccepted: addButton.clicked()
                    }
                    Item { Layout.fillWidth: true }
                }

                // Weight (g)
                RowLayout {
                    Layout.fillWidth: true
                    Label {
                        text: qsTr("Weight (g)")
                        horizontalAlignment: Text.AlignRight
                        Layout.preferredWidth: root.labelColWidth
                    }
                    TextField {
                        id: weightField
                        placeholderText: "0.0"
                        text: "0.0"
                        inputMethodHints: Qt.ImhFormattedNumbersOnly
                        validator: DoubleValidator { bottom: 0.0; top: 10000.0; decimals: 2 }
                        Layout.preferredWidth: 140
                        onAccepted: addButton.clicked()
                    }
                    Item { Layout.fillWidth: true }
                }

                // Add button
                RowLayout {
                    Layout.fillWidth: true
                    Item { Layout.preferredWidth: root.labelColWidth }
                    Button {
                        id: addButton
                        text: qsTr("Add")
                        enabled: nameField.text.trim() !== ""
                        onClicked: {
                            Backend.addFruitWithValidation(
                                nameField.text.trim(),
                                String(colorBox.currentText),
                                String(categoryBox.currentText),
                                Boolean(tropicalCheck.checked),
                                Number(caloriesField.text),
                                Number(weightField.text)
                            )
                        }
                    }
                    Item { Layout.fillWidth: true }
                }
            }
        }
    }

    // ---- Edit dialog ----
    Dialog {
        id: editDialog
        modal: true
        title: qsTr("Edit fruit")
        standardButtons: Dialog.Ok | Dialog.Cancel

        property int editIndex: -1

        function openFor(index, data) {
            editIndex = index
            editName.text = data.name ?? ""
            editColor.currentIndex = Math.max(0, root.colorOptions.indexOf(data.color.name ?? "PINK"))
            editCategory.currentIndex = Math.max(0, root.categoryOptions.indexOf(data.category.name ?? "OTHER"))
            editTropical.checked = !!data.isTropical
            editCalories.text = (Number.isFinite(data.calories) ? data.calories : 0).toString()
            editWeight.text = (Number.isFinite(data.weight) ? data.weight : 0.0).toString()
            open()
        }

        onAccepted: {
            Backend.updateFruitWithValidation(
                editIndex,
                editName.text.trim(),
                String(editColor.currentText),
                String(editCategory.currentText),
                Boolean(editTropical.checked),
                Number(editCalories.text),
                Number(editWeight.text)
            )
        }

        contentItem: ColumnLayout {
            spacing: 10
            Layout.fillWidth: true

            // Name
            RowLayout {
                Layout.fillWidth: true
                Label {
                    text: qsTr("Name")
                    horizontalAlignment: Text.AlignRight
                    Layout.preferredWidth: root.labelColWidth
                }
                TextField {
                    id: editName
                    Layout.fillWidth: true
                    onAccepted: editDialog.accept()
                }
            }

            // Color
            RowLayout {
                Layout.fillWidth: true
                Label {
                    text: qsTr("Color")
                    horizontalAlignment: Text.AlignRight
                    Layout.preferredWidth: root.labelColWidth
                }
                ComboBox {
                    id: editColor
                    model: root.colorOptions
                    Layout.fillWidth: true
                }
            }

            // Category
            RowLayout {
                Layout.fillWidth: true
                Label {
                    text: qsTr("Category")
                    horizontalAlignment: Text.AlignRight
                    Layout.preferredWidth: root.labelColWidth
                }
                ComboBox {
                    id: editCategory
                    model: root.categoryOptions
                    Layout.fillWidth: true
                }
            }

            // Tropical
            RowLayout {
                Layout.fillWidth: true
                Label {
                    text: qsTr("Tropical")
                    horizontalAlignment: Text.AlignRight
                    Layout.preferredWidth: root.labelColWidth
                }
                CheckBox { id: editTropical }
                Item { Layout.fillWidth: true }
            }

            // Calories
            RowLayout {
                Layout.fillWidth: true
                Label {
                    text: qsTr("Calories")
                    horizontalAlignment: Text.AlignRight
                    Layout.preferredWidth: root.labelColWidth
                }
                TextField {
                    id: editCalories
                    inputMethodHints: Qt.ImhDigitsOnly
                    validator: IntValidator { bottom: 0; top: 5000 }
                    Layout.preferredWidth: 140
                    onAccepted: editDialog.accept()
                }
                Item { Layout.fillWidth: true }
            }

            // Weight (g)
            RowLayout {
                Layout.fillWidth: true
                Label {
                    text: qsTr("Weight (g)")
                    horizontalAlignment: Text.AlignRight
                    Layout.preferredWidth: root.labelColWidth
                }
                TextField {
                    id: editWeight
                    validator: DoubleValidator { bottom: 0.0; top: 10000.0; decimals: 2 }
                    Layout.preferredWidth: 140
                    onAccepted: editDialog.accept()
                }
                Item { Layout.fillWidth: true }
            }
        }
    }

    // ---- Footer status bar ----
    footer: Frame {
        padding: 6
        RowLayout {
            anchors.fill: parent
            Label {
                id: statusDisplay
                text: ""
                color: "green"
                font.bold: true
                elide: Text.ElideRight
                Layout.fillWidth: true
                horizontalAlignment: Text.AlignHCenter
            }
        }
    }
}
