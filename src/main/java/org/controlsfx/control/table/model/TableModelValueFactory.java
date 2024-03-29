/**
 * Copyright (c) 2014, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.controlsfx.control.table.model;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import javafx.scene.control.TableColumn.CellDataFeatures;

/**
 *
 */
//not public as not ready for 8.20.7
class TableModelValueFactory<S, T> implements Callback<CellDataFeatures<TableModelRow<S>, T>, ObservableValue<T>> {
    @SuppressWarnings("unused")
    private final JavaFXTableModel<S> _tableModel;
    private final int _columnIndex;

    public TableModelValueFactory(JavaFXTableModel<S> tableModel, int columnIndex) {
        _tableModel = tableModel;
        _columnIndex = columnIndex;
    }

    @SuppressWarnings("unchecked")
    @Override public ObservableValue<T> call(CellDataFeatures<TableModelRow<S>, T> cdf) {
        TableModelRow<S> row = cdf.getValue();
        T valueAt = (T) row.get(_columnIndex);
        return valueAt instanceof ObservableValue ? ((ObservableValue<T>) valueAt) : new ReadOnlyObjectWrapper<>(valueAt);
    }
}
