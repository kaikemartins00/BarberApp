package com.kaikeMartins.barberapp;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class PhoneMask implements TextWatcher {

    private final EditText editText;
    private boolean isUpdating;

    public PhoneMask(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {

        if (isUpdating) {
            isUpdating = false;
            return;
        }

        String numbers = s.toString().replaceAll("[^\\d]", "");

        StringBuilder formatted = new StringBuilder();

        if (numbers.length() > 0)
            formatted.append("(");

        if (numbers.length() >= 2)
            formatted.append(numbers.substring(0,2)).append(") ");
        else
            formatted.append(numbers);

        if (numbers.length() > 2) {

            if (numbers.length() <= 7) {

                formatted.append(numbers.substring(2));

            } else {

                formatted.append(numbers.substring(2,7))
                        .append("-")
                        .append(numbers.substring(7));

            }

        }

        isUpdating = true;
        editText.setText(formatted.toString());
        editText.setSelection(editText.getText().length());

    }

}