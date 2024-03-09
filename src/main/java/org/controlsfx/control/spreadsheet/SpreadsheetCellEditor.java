/**
 * Copyright (c) 2013, 2017 ControlsFX
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
package org.controlsfx.control.spreadsheet;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.zssql.common.cutom.datetimepicker.DateTimePicker;
import com.zssql.common.handler.IndexTypeHandler;
import com.zssql.common.utils.CommonUtil;
import com.zssql.domain.dto.ddl.IndexRow;
import impl.org.controlsfx.i18n.Localization;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.util.StringConverter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.SearchableComboBox;

/**
 * 
 * SpreadsheetCellEditor are used by {@link SpreadsheetCellType} and
 * {@link SpreadsheetCell} in order to control how each value will be entered. <br>
 * 
 * <h3>General behavior:</h3> Editors will be displayed if the user double-click
 * in an editable cell ( see {@link SpreadsheetCell#setEditable(boolean)} ). <br>
 * If the user does anything outside the editor, the editor <b> will be forced
 * </b> to try to commit the edition and close itself. If the value is not
 * valid, the editor will cancel the value and close itself. The editor is just
 * here to allow communication between the user and the {@link SpreadsheetView}.
 * It will just be given a value, and it will just give back another one after.
 * The policy regarding validation of a given value is defined in
 * {@link SpreadsheetCellType#match(Object)}.
 * 
 * If the value doesn't meet the requirements when saving the cell, nothing
 * happens and the editor keeps editing. <br>
 * You can abandon a current modification by pressing "esc" key. <br>
 * 
 * You can specify a maximum height to your spreadsheetCellEditor with {@link #getMaxHeight()
 * }. This can be used in order to control the display of your editor. If they
 * should grow or not in a big cell. (for example a {@link TextAreaEditor} want
 * to grow with the cell in order to take full space for display.
 * <br>
 * <h3>Specific behavior:</h3> This class offers some static classes in order to
 * create a {@link SpreadsheetCellEditor}. Here are their properties: <br>
 * 
 * <ul>
 * <li> {@link StringEditor}: Basic {@link TextField}, can accept all data and
 * save it as a string.</li>
 * <li> {@link ListEditor}: Display a {@link ComboBox} with the different values.
 * </li>
 * <li> {@link DoubleEditor}: Display a {@link TextField} which accepts only
 * double value. If the entered value is incorrect, the background will turn red
 * so that the user will know in advance if the data will be saved or not.</li>
 * <li> {@link IntegerEditor}: Display a {@link TextField} which accepts only
 * Integer value. If the entered value is incorrect, the background will turn red
 * so that the user will know in advance if the data will be saved or not.</li>
 * <li> {@link DateEditor}: Display a {@link DatePicker}.</li>
 * <li> {@link ObjectEditor}: Display a {@link TextField} , accept an Object.</li>
 * </ul>
 * 
 * <br>
 * <h3>Creating your editor:</h3> You can of course create your own
 * {@link SpreadsheetCellEditor} for displaying other controls.<br>
 * 
 * You just have to override the four abstract methods. <b>Remember</b> that you
 * will never call those methods directly. They will be called by the
 * {@link SpreadsheetView} when needed.
 * <ul>
 * <li> {@link #startEdit(Object)}: You will configure your control with the
 * given value which is {@link SpreadsheetCell#getItem()} converted to an
 * object. You do not instantiate your control here, you do it in the
 * constructor.</li>
 * <li> {@link #getEditor()}: You will return which control you're using (for
 * display).</li>
 * <li> {@link #getControlValue()}: You will return the value inside your editor
 * in order to submit it for validation.</li>
 * <li> {@link #end()}: When editing is finished, you can properly close your own
 * control.</li>
 * </ul>
 * <br>
 * Keep in mind that you will interact only with {@link #endEdit(boolean)} where
 * a <b>true</b> value means you want to commit, and a <b>false</b> means you
 * want to cancel. The {@link SpreadsheetView} will handle all the rest for you
 * and call your methods at the right moment. <br>
 * 
 * <h3>Use case :</h3> <center><img src="editorScheme.png" alt="Use case of SpreadsheetCellEditor"></center>
 * 
 * <h3>Visual:</h3>
 * <table style="border: 1px solid gray;" summary="Screenshots of various SpreadsheetCellEditor">
 * <tr>
 * <td valign="center" style="text-align:right;"><strong>String</strong></td>
 * <td><center><img src="textEditor.png" alt="Screenshot of SpreadsheetCellEditor.StringEditor"></center></td>
 * </tr>
 * <tr>
 * <td valign="center" style="text-align:right;"><strong>List</strong></td>
 * <td><center><img src="listEditor.png" alt="Screenshot of SpreadsheetCellEditor.ListEditor"></center></td>
 * </tr>
 * <tr>
 * <td valign="center" style="text-align:right;"><strong>Double</strong></td>
 * <td><center><img src="doubleEditor.png" alt="Screenshot of SpreadsheetCellEditor.DoubleEditor"></center></td>
 * </tr>
 * <tr>
 * <td valign="center" style="text-align:right;"><strong>Date</strong></td>
 * <td><center><img src="dateEditor.png" alt="Screenshot of SpreadsheetCellEditor.DateEditor"></center></td>
 * </tr>
 * </table>
 * 
 * 
 * @see SpreadsheetView
 * @see SpreadsheetCell
 * @see SpreadsheetCellType
 */
public abstract class SpreadsheetCellEditor {
    private static final double MAX_EDITOR_HEIGHT = 50.0;
    
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##########"); //$NON-NLS-1$
    SpreadsheetView view;

    /***************************************************************************
     * * Constructor * *
     **************************************************************************/

    /**
     * Construct the SpreadsheetCellEditor.
     * 
     * @param view
     */
    public SpreadsheetCellEditor(SpreadsheetView view) {
        this.view = view;
    }

    /***************************************************************************
     * * Public Final Methods * *
     **************************************************************************/
    /**
     * Whenever you want to stop the edition, you call that method.<br>
     * True means you're trying to commit the value, then
     * {@link SpreadsheetCellType#convertValue(Object)} will be called in order
     * to verify that the value is correct.<br>
     * False means you're trying to cancel the value and it will be follow by
     * {@link #end()}.<br>
     * See SpreadsheetCellEditor description
     * 
     * @param b
     *            true means commit, false means cancel
     */
    public final void endEdit(boolean b) {
        view.getCellsViewSkin().getSpreadsheetCellEditorImpl().endEdit(b);
    }

    /***************************************************************************
     * * Public Abstract Methods * *
     **************************************************************************/
    /**
     * This method will be called when edition start.<br>
     * You will then do all the configuration of your editor.
     * 
     * @param item
     */
    public void startEdit(Object item){
        startEdit(item, null);
    }
    
    /**
     * Does the same as {@link #startEdit(Object) } but you have also
     * the {@link SpreadsheetCell#getFormat() } sent. This is useful when
     * editing Date for example, when you want to display it with the cell
     * format.
     *
     * Also options given by a Spreadsheetcell with {@link SpreadsheetCell#getOptionsForEditor()
     * } are given.
     *
     * @param item
     * @param format
     * @param options
     */
    public abstract void startEdit(Object item, String format, Object... options);

    /**
     * Return the control used for controlling the input. This is called at the
     * beginning in order to display your control in the cell.
     * 
     * @return the control used.
     */
    public abstract Control getEditor();

    /**
     * Return the value within your editor as a string. This will be used by the
     * {@link SpreadsheetCellType#convertValue(Object)} in order to compute
     * whether the value is valid regarding the {@link SpreadsheetCellType}
     * policy.
     * 
     * @return the value within your editor as a string.
     */
    public abstract String getControlValue();

    /**
     * This method will be called at the end of edition.<br>
     * You will be offered the possibility to do the configuration post editing.
     */
    public abstract void end();

    /***************************************************************************
     * * Public Methods * *
     **************************************************************************/
    /**
     * Return the maximum height of the editor. 
     * @return 50 by default.
     */
    public double getMaxHeight(){
        return MAX_EDITOR_HEIGHT;
    }
    
    /**
     * A {@link SpreadsheetCellEditor} for
     * {@link SpreadsheetCellType.ObjectType} typed cells. It displays a
     * {@link TextField} where the user can type different values.
     */
    public static class ObjectEditor extends SpreadsheetCellEditor {

        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final TextField tf;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        /**
        * Constructor for the ObjectEditor..
        * @param view The SpreadsheetView
        */
        public ObjectEditor(SpreadsheetView view) {
            super(view);
            tf = new TextField();
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        @Override
        public void startEdit(Object value, String format, Object... options) {
            if (value instanceof String) {
                tf.setText(value.toString());
            }
            attachEnterEscapeEventHandler();

            tf.requestFocus();
            tf.end();
        }

        @Override
        public String getControlValue() {
            return tf.getText();
        }

        @Override
        public void end() {
            tf.setOnKeyPressed(null);
        }

        @Override
        public TextField getEditor() {
            return tf;
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            tf.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        endEdit(true);
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            });
        }
    }

    /**
     * A {@link SpreadsheetCellEditor} for
     * {@link SpreadsheetCellType.StringType} typed cells. It displays a
     * {@link TextField} where the user can type different values.
     */
    public static class StringEditor extends SpreadsheetCellEditor {
        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final TextField tf;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        /**
         * Constructor for the StringEditor.
         * @param view The SpreadsheetView
         */
        public StringEditor(SpreadsheetView view) {
            super(view);
            tf = new TextField();
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        @Override
        public void startEdit(Object value, String format, Object... options) {

            if (value instanceof String || value == null) {
                tf.setText((String) value);
            }
            attachEnterEscapeEventHandler();

            tf.requestFocus();
            tf.selectAll();
        }

        @Override
        public String getControlValue() {
            return tf.getText();
        }

        @Override
        public void end() {
            tf.setOnKeyPressed(null);
        }

        @Override
        public TextField getEditor() {
            return tf;
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            tf.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        endEdit(true);
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            });
        }
    }

    
    /**
     * A {@link SpreadsheetCellEditor} for
     * {@link SpreadsheetCellType.StringType} typed cells. It displays a
     * {@link TextField} where the user can type different values.
     */
    public static class TextAreaEditor extends SpreadsheetCellEditor {

        /**
         * *************************************************************************
         * * Private Fields * *
         * ************************************************************************
         */
        private final TextArea textArea;

        /**
         * *************************************************************************
         * * Constructor * *
         * ************************************************************************
         */
        /**
         * Constructor for the StringEditor.
         *
         * @param view The SpreadsheetView
         */
        public TextAreaEditor(SpreadsheetView view) {
            super(view);
            textArea = new TextArea();
            textArea.setWrapText(true);
            //The textArea is not respecting the maxHeight if we are not setting the min..
            textArea.minHeightProperty().bind(textArea.maxHeightProperty());
        }

        /**
         * *************************************************************************
         * * Public Methods * *
         * ************************************************************************
         */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            if (value instanceof String || value == null) {
                textArea.setText((String) value);
            }
            attachEnterEscapeEventHandler();

            textArea.requestFocus();
            textArea.selectAll();
        }

        @Override
        public String getControlValue() {
            return textArea.getText();
        }

        @Override
        public void end() {
            textArea.setOnKeyPressed(null);
        }

        @Override
        public TextArea getEditor() {
            return textArea;
        }

        @Override
        public double getMaxHeight() {
            return Double.MAX_VALUE;
        }
        
        /**
         * *************************************************************************
         * * Private Methods * *
         * ************************************************************************
         */
        private void attachEnterEscapeEventHandler() {
            
            textArea.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent keyEvent) {
                    if (keyEvent.getCode() == KeyCode.ENTER) {
                        if (keyEvent.isShiftDown()) {
                            //if shift is down, we insert a new line.
                            textArea.replaceSelection("\n"); //$NON-NLS-1$
                        } else {
                            endEdit(true);
                        }
                    } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }else if(keyEvent.getCode() == KeyCode.TAB){
                        if (keyEvent.isShiftDown()) {
                            //if shift is down, we insert a tab.
                            textArea.replaceSelection("\t"); //$NON-NLS-1$
                            keyEvent.consume();
                        } else {
                            endEdit(true);
                        }
                    }
                }
            });
        }
    }
    
    /**
     * A {@link SpreadsheetCellEditor} for
     * {@link SpreadsheetCellType.DoubleType} typed cells. It displays a
     * {@link TextField} where the user can type different numbers. Only numbers
     * will be stored. <br>
     * Moreover, the {@link TextField} will turn red if the value currently
     * entered if incorrect.
     */
    public static class DoubleEditor extends SpreadsheetCellEditor {

        /***************************************************************************
         * * private Fields * *
         **************************************************************************/
        private final TextField tf;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        /**
         * Constructor for the DoubleEditor.
         * @param view The SpreadsheetView.
         */
        public DoubleEditor(SpreadsheetView view) {
            super(view);
            tf = new TextField() {
                
                @Override
                public void insertText(int index, String text) {
                    String fixedText = fixText(text);
                    super.insertText(index, fixedText);
                }

                @Override
                public void replaceText(int start, int end, String text) {
                    String fixedText = fixText(text);
                    super.replaceText(start, end, fixedText);
                }

                @Override
                public void replaceText(IndexRange range, String text) {
                    replaceText(range.getStart(), range.getEnd(), text);
                }

                private String fixText(String text) {
                    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Localization.getLocale());
                    text = text.replace(' ', '\u00a0');//$NON-NLS-1$
                    return text.replaceAll("\\.", Character.toString(symbols.getDecimalSeparator()));//$NON-NLS-1$
                }
            };
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            if (value instanceof Double) {
                //We want to set the text in its proper form regarding the Locale.
                decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Localization.getLocale()));
                tf.setText(((Double) value).isNaN() ? "" : decimalFormat.format(value)); //$NON-NLS-1$
            } else {
                tf.setText(null);
            }

            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
            attachEnterEscapeEventHandler();

            tf.requestFocus();
            tf.selectAll();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            tf.setOnKeyPressed(null);
        }

        /** {@inheritDoc} */
        @Override
        public TextField getEditor() {
            return tf;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            NumberFormat format = NumberFormat.getInstance(Localization.getLocale());
            ParsePosition parsePosition = new ParsePosition(0);
            if (tf.getText() != null) {
                Number number = format.parse(tf.getText(), parsePosition);
                if (number != null && parsePosition.getIndex() == tf.getText().length()) {
                    return String.valueOf(number.doubleValue());
                }
            }
            return tf.getText();
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            tf.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        try {
                            if (tf.getText().equals("")) { //$NON-NLS-1$
                                endEdit(true);
                            } else {
                                tryParsing();
                                endEdit(true);
                            }
                        } catch (Exception e) {
                        }

                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            });
            
            tf.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    try {
                        if (tf.getText().equals("")) { //$NON-NLS-1$
                            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
                        } else {
                            tryParsing();
                            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
                        }
                    } catch (Exception e) {
                        tf.getStyleClass().add("error"); //$NON-NLS-1$
                    }
                }
            });
        }
        
        private void tryParsing() throws ParseException {
            NumberFormat format = NumberFormat.getNumberInstance(Localization.getLocale());
            ParsePosition parsePosition = new ParsePosition(0);
            format.parse(tf.getText(), parsePosition);
            if (parsePosition.getIndex() != tf.getText().length()) {
                throw new ParseException("Invalid input", parsePosition.getIndex());
            }
        }
    }

    /**
     * A {@link SpreadsheetCellEditor} for
     * {@link SpreadsheetCellType.DoubleType} typed cells. It displays a
     * {@link TextField} where the user can type different numbers. Only numbers
     * will be stored. <br>
     * Moreover, the {@link TextField} will turn red if the value currently
     * entered if incorrect.
     */
    public static class IntegerEditor extends SpreadsheetCellEditor {

        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final TextField tf;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        /**
         * Constructor for the IntegerEditor.
         * @param view the SpreadsheetView
         */
        public IntegerEditor(SpreadsheetView view) {
            super(view);
            tf = new TextField();
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            if (value instanceof Integer) {
                tf.setText(Integer.toString((Integer) value));
            } else {
                tf.setText(null);
            }

            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
            attachEnterEscapeEventHandler();

            tf.requestFocus();
            tf.selectAll();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            tf.setOnKeyPressed(null);
        }

        /** {@inheritDoc} */
        @Override
        public TextField getEditor() {
            return tf;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            return tf.getText();
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            tf.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        try {
                            if (tf.getText().equals("")) { //$NON-NLS-1$
                                endEdit(true);
                            } else {
                                Integer.parseInt(tf.getText());
                                endEdit(true);
                            }
                        } catch (Exception e) {
                        }

                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            });
            tf.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    try {
                        if (tf.getText().equals("")) { //$NON-NLS-1$
                            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
                        } else {
                            Integer.parseInt(tf.getText());
                            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
                        }
                    } catch (Exception e) {
                        tf.getStyleClass().add("error"); //$NON-NLS-1$
                    }
                }
            });
        }
    }

    /**
     * 
     * A {@link SpreadsheetCellEditor} for {@link SpreadsheetCellType.ListType}
     * typed cells. It displays a {@link ComboBox} where the user can choose a
     * value.
     */
    public static class ListEditor<R> extends SpreadsheetCellEditor {
        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final List<String> itemList;
        private final ComboBox<String> cb;
        private String originalValue;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/

        /**
         * Constructor for the ListEditor.
         * @param view The SpreadsheetView
         * @param itemList The items to display in the editor.
         */
        public ListEditor(SpreadsheetView view, final List<String> itemList) {
            super(view);
            this.itemList = itemList;
            cb = new ComboBox<>();
            cb.setVisibleRowCount(5);
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/

        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            if (value instanceof String) {
                originalValue = value.toString();
            } else {
                originalValue = null;
            }
            ObservableList<String> items = FXCollections.observableList(itemList);
            cb.setItems(items);
            cb.setValue(originalValue);

            attachEnterEscapeEventHandler();
            cb.show();
            cb.requestFocus();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            cb.setOnKeyPressed(null);
        }

        /** {@inheritDoc} */
        @Override
        public ComboBox<String> getEditor() {
            return cb;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            return cb.getSelectionModel().getSelectedItem();
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {

            cb.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ESCAPE) {
                        cb.setValue(originalValue);
                        endEdit(false);
                    } else if (t.getCode() == KeyCode.ENTER) {
                        endEdit(true);
                    }
                }
            });
        }
    }

    /**
     *
     * A {@link SpreadsheetCellEditor} for {@link SpreadsheetCellType.FilterListType}
     * typed cells. It displays a {@link SearchableComboBox} where the user can choose a
     * value.
     */
    public static class FilterListEditor<R> extends SpreadsheetCellEditor {
        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final List<String> itemList;
        private SearchableComboBox<String> cb;
        private String originalValue;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/

        /**
         * Constructor for the ListEditor.
         * @param view The SpreadsheetView
         * @param itemList The items to display in the editor.
         */
        public FilterListEditor(SpreadsheetView view, final List<String> itemList) {
            super(view);
            this.itemList = itemList;
            cb = new SearchableComboBox<>();
            cb.setVisibleRowCount(5);
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/

        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            if (value instanceof String) {
                originalValue = value.toString();
            } else {
                originalValue = null;
            }

            ObservableList<String> items = FXCollections.observableList(itemList);
            cb.setItems(items);
            cb.setValue(originalValue);

            attachEnterEscapeEventHandler();
            cb.show();
            // cb.requestFocus();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            /*String selectedItem = cb.getSelectionModel().getSelectedItem();
            if (!StringUtils.equals(selectedItem, originalValue)) {
                view.getCellsViewSkin().getSpreadsheetCellEditorImpl().endEditByForece(true);
            }*/
            cb.setOnKeyPressed(null);
        }

        /** {@inheritDoc} */
        @Override
        public SearchableComboBox<String> getEditor() {
            return cb;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            return cb.getSelectionModel().getSelectedItem();
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {

            cb.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ESCAPE) {
                        cb.setValue(originalValue);
                        endEdit(false);
                    } else if (t.getCode() == KeyCode.ENTER) {
                        endEdit(true);
                    }
                }
            });
            cb.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observableValue, String oldVal, String newVal) {
                    /*if (null != newVal && !StringUtils.equals(oldVal, newVal)) {
                        endEdit(true);
                    }*/
                    view.getCellsViewSkin().getSpreadsheetCellEditorImpl().updateCell(newVal);
                }
            });
        }
    }

    /**
     * A {@link SpreadsheetCellEditor} for {@link SpreadsheetCellType.DateType}
     * typed cells. It displays a {@link DatePicker} where the user can choose a
     * date through a visual calendar.
     */
    public static class DateEditor extends SpreadsheetCellEditor {

        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final DatePicker datePicker;
        private EventHandler<KeyEvent> eh;
        private ChangeListener<LocalDate> cl;
        /**
         * This is needed because "endEdit" will call our "end" method too late
         * when pressing enter, so several "endEdit" will be called. So this
         * prevent that to happen.
         */
        private boolean ending = false;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        /**
         * Constructor for the DateEditor.
         * @param view the SpreadsheetView
         * @param converter A Converter for converting a date to a String.
         */
        public DateEditor(SpreadsheetView view, StringConverter<LocalDate> converter) {
            super(view);
            datePicker = new DatePicker();
            datePicker.setConverter(converter);
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            if (value instanceof LocalDate) {
                datePicker.setValue((LocalDate) value);
            }
            attachEnterEscapeEventHandler();
            datePicker.show();
            datePicker.getEditor().requestFocus();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            if (datePicker.isShowing()) {
                datePicker.hide();
            }
            datePicker.removeEventFilter(KeyEvent.KEY_PRESSED, eh);
            datePicker.valueProperty().removeListener(cl);
        }

        /** {@inheritDoc} */
        @Override
        public DatePicker getEditor() {
            return datePicker;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            return datePicker.getEditor().getText();
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            /**
             * We need to add an EventFilter because otherwise the DatePicker
             * will block "escape" and "enter". But when "enter" is hit, we need
             * to runLater the commit because the value has not yet hit the
             * DatePicker itself.
             */
            eh = new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        ending = true;
                        endEdit(true);
                        ending = false;
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            };

            datePicker.addEventFilter(KeyEvent.KEY_PRESSED, eh);

            cl = new ChangeListener<LocalDate>() {
                @Override
                public void changed(ObservableValue<? extends LocalDate> arg0, LocalDate arg1, LocalDate arg2) {
                    if (!ending)
                        endEdit(true);
                }
            };
            datePicker.valueProperty().addListener(cl);
        }

    }

    public static class StringHandlerEditor extends SpreadsheetCellEditor {
        private final TextField tf;
        private final IndexRow indexRow;
        private final IndexTypeHandler handler;

        public StringHandlerEditor(SpreadsheetView view, IndexRow indexRow, IndexTypeHandler handler) {
            super(view);
            tf = new TextField();
            tf.setEditable(false);
            this.indexRow = indexRow;
            this.handler = handler;
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        @Override
        public void startEdit(Object value, String format, Object... options) {

            if (value instanceof String || value == null) {
                tf.setText((String) value);
            }
            attachEnterEscapeEventHandler();

            handler.handler(indexRow);
            endEdit(false);
        }

        @Override
        public String getControlValue() {
            return tf.getText();
        }

        @Override
        public void end() {
            tf.setOnKeyPressed(null);
        }

        @Override
        public TextField getEditor() {
            return tf;
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            tf.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    handler.handler(indexRow);
                    endEdit(false);
                }
            });
            tf.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 1) {
                    handler.handler(indexRow);
                }
                endEdit(false);
            });
        }
    }

    public static class LableStringEditor extends SpreadsheetCellEditor {
        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final TextField tf;
        private final Label label;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        /**
         * Constructor for the StringEditor.
         * @param view The SpreadsheetView
         */
        public LableStringEditor(SpreadsheetView view, Label label) {
            super(view);
            tf = new TextField();
            this.label = label;
            if (null != label.getText() && !StringUtils.equals(label.getText(), "(NULL)") && !label.isDisable()) {
                tf.setText(label.getText());
            }
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        @Override
        public void startEdit(Object value, String format, Object... options) {

            /*if (value instanceof String || value == null) {
                tf.setText((String) value);
            }*/
            attachEnterEscapeEventHandler();

            tf.requestFocus();
            tf.selectAll();
        }

        @Override
        public String getControlValue() {
            return tf.getText();
        }

        @Override
        public void end() {
            tf.setOnKeyPressed(null);
            label.setText(tf.getText());
        }

        @Override
        public TextField getEditor() {
            return tf;
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            tf.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        endEdit(true);
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            });
        }
    }

    public static class LabelDateEditor extends SpreadsheetCellEditor {

        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final DatePicker datePicker;
        private EventHandler<KeyEvent> eh;
        private ChangeListener<LocalDate> cl;
        /**
         * This is needed because "endEdit" will call our "end" method too late
         * when pressing enter, so several "endEdit" will be called. So this
         * prevent that to happen.
         */
        private boolean ending = false;
        private Label label;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        public LabelDateEditor(SpreadsheetView view, Label label, StringConverter<LocalDate> converter) {
            super(view);
            this.label = label;
            datePicker = new DatePicker();
            datePicker.setEditable(false);
            datePicker.setConverter(converter);
            if (null != label.getText() && !StringUtils.equals(label.getText(), "(NULL)") && !label.isDisable()) {
                datePicker.setValue(LocalDateTimeUtil.parseDate(label.getText()));
            }
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            /*if (value instanceof LocalDate) {
                datePicker.setValue((LocalDate) value);
            }*/
            attachEnterEscapeEventHandler();
            datePicker.show();
            datePicker.getEditor().requestFocus();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            if (datePicker.isShowing()) {
                datePicker.hide();
            }
            datePicker.removeEventFilter(KeyEvent.KEY_PRESSED, eh);
            datePicker.valueProperty().removeListener(cl);
            label.setText(CommonUtil.toString(datePicker.getValue()));
        }

        /** {@inheritDoc} */
        @Override
        public DatePicker getEditor() {
            return datePicker;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            /*System.out.println(datePicker.getEditor().getText());
            System.out.println(datePicker.getValue());
            return datePicker.getEditor().getText();*/
            return datePicker.getConverter().toString(datePicker.getValue());
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            /**
             * We need to add an EventFilter because otherwise the DatePicker
             * will block "escape" and "enter". But when "enter" is hit, we need
             * to runLater the commit because the value has not yet hit the
             * DatePicker itself.
             */
            eh = new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        ending = true;
                        endEdit(true);
                        ending = false;
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            };

            datePicker.addEventFilter(KeyEvent.KEY_PRESSED, eh);

            cl = new ChangeListener<LocalDate>() {
                @Override
                public void changed(ObservableValue<? extends LocalDate> arg0, LocalDate arg1, LocalDate arg2) {
                    if (!ending)
                        endEdit(true);
                }
            };
            datePicker.valueProperty().addListener(cl);
        }
    }

    public static class LabelDateTimeEditor extends SpreadsheetCellEditor {

        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final DateTimePicker dateTimePicker;
        private EventHandler<KeyEvent> eh;
        // private ChangeListener<LocalDateTime> cl;
        /**
         * This is needed because "endEdit" will call our "end" method too late
         * when pressing enter, so several "endEdit" will be called. So this
         * prevent that to happen.
         */
        private boolean ending = false;
        private Label label;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        public LabelDateTimeEditor(SpreadsheetView view, Label label, StringConverter<LocalDateTime> converter) {
            super(view);
            this.label = label;
            dateTimePicker = new DateTimePicker();
            dateTimePicker.setMinutesSelector(true);
            dateTimePicker.setFormat(DateTimePicker.DEFAULT_FORMAT);
            dateTimePicker.setEditable(true);
            if (null != label.getText() && !StringUtils.equals(label.getText(), "(NULL)") && !label.isDisable()) {
                dateTimePicker.setDateTimeValue(LocalDateTimeUtil.parse(label.getText(), "yyyy-MM-dd HH:mm:ss"));
            }
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            /*if (value instanceof LocalDate) {
                datePicker.setValue((LocalDate) value);
            }*/
            attachEnterEscapeEventHandler();
            dateTimePicker.show();
            dateTimePicker.getEditor().requestFocus();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            if (dateTimePicker.isShowing()) {
                dateTimePicker.hide();
            }
            dateTimePicker.removeEventFilter(KeyEvent.KEY_PRESSED, eh);
            // dateTimePicker.dateTimeValueProperty().removeListener(cl);

            label.setText(CommonUtil.toString(dateTimePicker.getDateTimeValue()));
        }

        /** {@inheritDoc} */
        @Override
        public DateTimePicker getEditor() {
            return dateTimePicker;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            LocalDateTime value = dateTimePicker.getDateTimeValue();
            return LocalDateTimeUtil.format(value, "yyyy-MM-dd HH:mm:ss");
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            /**
             * We need to add an EventFilter because otherwise the DatePicker
             * will block "escape" and "enter". But when "enter" is hit, we need
             * to runLater the commit because the value has not yet hit the
             * DatePicker itself.
             */
            eh = new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        ending = true;
                        endEdit(true);
                        ending = false;
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            };

            dateTimePicker.addEventFilter(KeyEvent.KEY_PRESSED, eh);

            /*cl = new ChangeListener<LocalDateTime>() {
                @Override
                public void changed(ObservableValue<? extends LocalDateTime> arg0, LocalDateTime arg1, LocalDateTime arg2) {
                    if (!ending)
                        endEdit(true);
                }
            };
            dateTimePicker.dateTimeValueProperty().addListener(cl);*/
        }
    }

    public static class LabelLongEditor extends SpreadsheetCellEditor {

        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final TextField tf;
        private final Label label;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        /**
         * Constructor for the IntegerEditor.
         * @param view the SpreadsheetView
         */
        public LabelLongEditor(SpreadsheetView view, Label label, boolean isUnsigned) {
            super(view);
            tf = new TextField();
            this.label = label;
            CommonUtil.integerTextField(isUnsigned, tf);
            if (null != label.getText() && !StringUtils.equals(label.getText(), "(NULL)") && !label.isDisable()) {
                tf.setText(label.getText());
            }
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            /*if (value instanceof Integer) {
                tf.setText(Integer.toString((Integer) value));
            } else {
                tf.setText(null);
            }*/

            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
            attachEnterEscapeEventHandler();

            tf.requestFocus();
            tf.selectAll();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            tf.setOnKeyPressed(null);
            label.setText(tf.getText());
        }

        /** {@inheritDoc} */
        @Override
        public TextField getEditor() {
            return tf;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            return tf.getText();
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            tf.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        try {
                            if (null != tf.getText() && tf.getText().equals("")) { //$NON-NLS-1$
                                endEdit(true);
                            } else {
                                // Integer.parseInt(tf.getText());
                                endEdit( true);
                            }
                        } catch (Exception e) {
                        }

                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            });
            tf.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    try {
                        if (null != tf.getText() && tf.getText().equals("")) { //$NON-NLS-1$
                            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
                        } else {
                            // Integer.parseInt(tf.getText());
                            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
                        }
                    } catch (Exception e) {
                        tf.getStyleClass().add("error"); //$NON-NLS-1$
                    }
                }
            });
        }
    }

    public static class LabelDecimalEditor extends SpreadsheetCellEditor {

        /***************************************************************************
         * * private Fields * *
         **************************************************************************/
        private final TextField tf;
        private final Label label;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/
        /**
         * Constructor for the DoubleEditor.
         * @param view The SpreadsheetView.
         */
        public LabelDecimalEditor(SpreadsheetView view, Label label, boolean isUnsigned) {
            super(view);
            this.label = label;
            tf = new TextField();
            CommonUtil.decimalTextField(isUnsigned, tf);
            if (null != label.getText() && !StringUtils.equals(label.getText(), "(NULL)") && !label.isDisable()) {
                tf.setText(label.getText());
            }
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/
        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            /*if (value instanceof Double) {
                //We want to set the text in its proper form regarding the Locale.
                decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Localization.getLocale()));
                tf.setText(((Double) value).isNaN() ? "" : decimalFormat.format(value)); //$NON-NLS-1$
            } else {
                tf.setText(null);
            }*/

            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
            attachEnterEscapeEventHandler();

            tf.requestFocus();
            tf.selectAll();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            tf.setOnKeyPressed(null);
            label.setText(tf.getText());
        }

        /** {@inheritDoc} */
        @Override
        public TextField getEditor() {
            return tf;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            /*NumberFormat format = NumberFormat.getInstance(Localization.getLocale());
            ParsePosition parsePosition = new ParsePosition(0);
            if (tf.getText() != null) {
                Number number = format.parse(tf.getText(), parsePosition);
                if (number != null && parsePosition.getIndex() == tf.getText().length()) {
                    return String.valueOf(number.doubleValue());
                }
            }*/
            return tf.getText();
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {
            tf.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        try {
                            if (tf.getText().equals("")) { //$NON-NLS-1$
                                endEdit(true);
                            } else {
                                new BigDecimal(tf.getText());
                                endEdit(true);
                            }
                        } catch (Exception e) {
                        }

                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    }
                }
            });

            tf.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    try {
                        if (tf.getText().equals("")) { //$NON-NLS-1$
                            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
                        } else {
                            tf.getStyleClass().removeAll("error"); //$NON-NLS-1$
                        }
                    } catch (Exception e) {
                        tf.getStyleClass().add("error"); //$NON-NLS-1$
                    }
                }
            });
        }
    }

    public static class LabelListEditor<R> extends SpreadsheetCellEditor {
        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final ComboBox<String> cb;
        private final Label label;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/

        /**
         * Constructor for the ListEditor.
         * @param view The SpreadsheetView
         * @param itemList The items to display in the editor.
         */
        public LabelListEditor(SpreadsheetView view, final List<String> itemList, Label label) {
            super(view);
            cb = new ComboBox<>();
            ObservableList<String> items = FXCollections.observableList(itemList);
            cb.setItems(items);
            cb.setVisibleRowCount(5);
            this.label = label;
            if (null != label.getText() && !StringUtils.equals(label.getText(), "(NULL)") && !label.isDisable()) {
                cb.setValue(label.getText());
            }
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/

        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            /*if (value instanceof String) {
                originalValue = value.toString();
            } else {
                originalValue = null;
            }
            ObservableList<String> items = FXCollections.observableList(itemList);
            cb.setItems(items);
            cb.setValue(originalValue);*/

            attachEnterEscapeEventHandler();
            cb.show();
            cb.requestFocus();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            cb.setOnKeyPressed(null);
            if (cb.getSelectionModel().getSelectedIndex() < 0) {
                label.setText(null);
            } else {
                label.setText(cb.getSelectionModel().getSelectedItem());
            }
        }

        /** {@inheritDoc} */
        @Override
        public ComboBox<String> getEditor() {
            return cb;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            return cb.getSelectionModel().getSelectedItem();
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {

            cb.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    } else if (t.getCode() == KeyCode.ENTER) {
                        endEdit(true);
                    }
                }
            });
        }
    }

    public static class LabelListCheckEditor<R> extends SpreadsheetCellEditor {
        /***************************************************************************
         * * Private Fields * *
         **************************************************************************/
        private final CheckComboBox<String> cb;
        private final Label label;

        /***************************************************************************
         * * Constructor * *
         **************************************************************************/

        /**
         * Constructor for the ListEditor.
         * @param view The SpreadsheetView
         * @param itemList The items to display in the editor.
         */
        public LabelListCheckEditor(SpreadsheetView view, final List<String> itemList, Label label) {
            super(view);
            cb = new CheckComboBox<>();
            cb.getItems().addAll(itemList);
            this.label = label;
            if (null != label.getText() && !StringUtils.equals(label.getText(), "(NULL)") && !label.isDisable()) {
                List<String> selectVals = CommonUtil.split(label.getText(), ",");
                if (CollectionUtils.isNotEmpty(selectVals)) {
                    for (String selectVal : selectVals) {
                        cb.getCheckModel().check(selectVal);
                    }
                }
            }
        }

        /***************************************************************************
         * * Public Methods * *
         **************************************************************************/

        /** {@inheritDoc} */
        @Override
        public void startEdit(Object value, String format, Object... options) {
            /*if (value instanceof String) {
                originalValue = value.toString();
            } else {
                originalValue = null;
            }
            ObservableList<String> items = FXCollections.observableList(itemList);
            cb.setItems(items);
            cb.setValue(originalValue);*/

            attachEnterEscapeEventHandler();
            cb.show();
            cb.requestFocus();
        }

        /** {@inheritDoc} */
        @Override
        public void end() {
            cb.setOnKeyPressed(null);
            ObservableList<String> checkedItems = cb.getCheckModel().getCheckedItems();
            if (CollectionUtils.isEmpty(checkedItems)) {
                label.setText(null);
            } else {
                label.setText(CommonUtil.join(checkedItems, ","));
            }
        }

        /** {@inheritDoc} */
        @Override
        public CheckComboBox<String> getEditor() {
            return cb;
        }

        /** {@inheritDoc} */
        @Override
        public String getControlValue() {
            ObservableList<String> checkedItems = cb.getCheckModel().getCheckedItems();
            if (CollectionUtils.isEmpty(checkedItems)) {
                return null;
            } else {
                return CommonUtil.join(checkedItems, ",");
            }
        }

        /***************************************************************************
         * * Private Methods * *
         **************************************************************************/

        private void attachEnterEscapeEventHandler() {

            cb.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ESCAPE) {
                        endEdit(false);
                    } else if (t.getCode() == KeyCode.ENTER) {
                        endEdit(true);
                    }
                }
            });
        }
    }
}
