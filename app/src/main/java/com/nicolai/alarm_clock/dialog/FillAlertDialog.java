package com.nicolai.alarm_clock.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.nicolai.alarm_clock.MainActivity;
import com.nicolai.alarm_clock.R;

import androidx.fragment.app.DialogFragment;

public class FillAlertDialog extends DialogFragment {

    private EditText editText;
    private View view;
    private String type, title, disclaimer;

    public interface OnInfoGiven{
        void sendInfo(String info, String type);
    }
    public FillAlertDialog.OnInfoGiven mOnInfoGiven;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.fill_alert_dialog, null);

        //get Type
        if(getArguments() != null){
            type = getArguments().getString("type");
        }else {
            type = MainActivity.ADDITIONAL_INFO_NUMBER;
        }

        //Initialisierung edit text
        editText = view.findViewById(R.id.editAdditionalInfo);

        if(type.equals(MainActivity.ADDITIONAL_INFO_NAME)){
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setHint(R.string.provide_name);
            title = getString(R.string.name_title);
            disclaimer = getString(R.string.provide_name_disclaimer);
        }else{
            title = getString(R.string.number_title);
            disclaimer = getString(R.string.provide_number_disclaimer);
        }

        TextView textView = view.findViewById(R.id.aiDisclaimer);
        textView.setText(disclaimer);

        //Erstellung des Alertdialogs
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setView(view)//Nicht zwei mal inflaten!!!!!!!!!
                .setTitle(title)
                .setPositiveButton(R.string.speichern, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Intervall daten an Host schicken
                                mOnInfoGiven.sendInfo(editText.getText().toString(), type);
                            }
                        }
                )
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) { }
                        }
                )
                .create();

        //EditText onClick listener
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mOnInfoGiven.sendInfo(editText.getText().toString(), type);

                    // hide virtual keyboard
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                    //dismiss alertdialog
                    alertDialog.dismiss();

                    return true;
                }
                return false;
            }
        });

        return alertDialog;
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        try{
            mOnInfoGiven = (FillAlertDialog.OnInfoGiven) getActivity();

        }catch (ClassCastException e){

        }
    }
}
