/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

import org.qtproject.qt.bridge.core.QtListModel;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;

// Registers this Java class as a QML singleton with the name "FruitBasket".
// Allows QML to create and interact with this class as if it were a native QML object.
// @start region="qmlregistrable-singleton"
@QMLRegistrable(singleton = true)
public class FruitBasket {
    // @end
    // @start region="qmlsignals-usage"
    public interface QmlCallback {
        void duplicateFound(String fruit);
        void erased();
        void blankFound();
    }
    // Establishes a binding to a callback interface that is used to emit signals or notifications
    // from Java to QML. This allows QML to react to specific events like validation failures or updates.
    @QMLSignals
    QmlCallback qmlCallback;
    // @end

    // Repository class responsible for managing data operations.
    // Acts as the data source for the ViewModel.
    private final Repository db = new Repository();

    // A QtListModel wrapping a list of strings fetched from the repository.
    // Exposed to QML as a QAbstractListModel, allowing it to be used in model-driven QML components
    private final QtListModel<String> fruitList = new QtListModel<>(db.fetchAll());

    {
        // This block listens for changes in the list. It's not used for anything, but is
        // just for demonstrative purposes:
        fruitList.onSizeChanged(() -> System.out.println("List size changed to: " + fruitList.size()));
    }

    // Exposed method to QML that attempts to add a new string to the list.
    // Validation is performed before insertion:
    // - If the input string is blank, it emits a 'blankFound' signal to QML.
    // - If the item already exists in the list, it emits a 'duplicateFound' signal with the duplicated item.
    // - Otherwise, it adds the item to the list.
    public void addString(String item) {
        if (item.isBlank()) {
            qmlCallback.blankFound();
            return;
        }
        if (fruitList.contains(item)) {
            qmlCallback.duplicateFound(item);
            return;
        }
        fruitList.appendItem(item);
    }

    // Exposed method to QML to update an existing entry at a specific index.
    // Before updating:
    // - If the new value already exists elsewhere in the list, it emits a 'duplicateFound' signal.
    // - Otherwise, it updates the list at the given index.
    public void update(int index, String updatedValue) {
        if (fruitList.contains(updatedValue)) {
            qmlCallback.duplicateFound(updatedValue);
            return;
        }
        fruitList.updateItemAt(index, updatedValue);
    }

    // Exposed method to QML to remove an item at the specified index.
    // After successful removal, it emits an 'erased' signal to notify QML of the change.
    public void remove(int index) {
        fruitList.removeItemAt(index);
        qmlCallback.erased();
    }
}
