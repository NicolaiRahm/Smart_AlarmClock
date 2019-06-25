package com.nicolai.alarm_clock.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.hbb20.CountryCodePicker;
import com.nicolai.alarm_clock.R;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class Coutry_code_picker extends DialogFragment {

    private CountryCodePicker countryCodePicker;
    private TextView numberView;
    private View view;
    private String name, number;

    @Override
    public void show(FragmentManager manager, String tag) {

        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        number = getArguments().getString("number");
        name = getArguments().getString("name");
    }

    public interface OnCodeChoosen{
        void sendCode(String code,String number, String name);
    }
    public OnCodeChoosen mOnCodeChoosen;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.country_code_dialog, null);

        countryCodePicker = view.findViewById(R.id.codePicker);
        countryCodePicker.setCountryForNameCode(getResources().getConfiguration().locale.getCountry());

        numberView = view.findViewById(R.id.countryCodeNumber);
        numberView.setText(number);

        return new AlertDialog.Builder(getActivity()).setView(view)//Nicht zwei mal inflaten!!!!!!!!!
                .setTitle(getString(R.string.add_country_code, name))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Wenn nur einmal: intervall 0, aber anzahl 3 genommen
                                mOnCodeChoosen.sendCode(countryCodePicker.getSelectedCountryCode(), number, name);
                            }
                        }
                )
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) { }
                        }
                )
                .create();
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        try{
            if(getTargetFragment() == null){//Kommt von Standardeinstellungen
                mOnCodeChoosen = (OnCodeChoosen) getActivity();
            }else {
                mOnCodeChoosen = (OnCodeChoosen) getTargetFragment();
            }

        }catch (ClassCastException e){

        }
    }
}
