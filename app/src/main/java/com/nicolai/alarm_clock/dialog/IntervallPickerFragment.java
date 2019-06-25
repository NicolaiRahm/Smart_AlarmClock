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

import com.nicolai.alarm_clock.R;

import androidx.fragment.app.DialogFragment;

public class IntervallPickerFragment extends DialogFragment {

    private NumberPicker np;
    private NumberPicker np2;
    private View view;

    public interface OnIntervallChoosen{
        void sendIntervall(int anzahl, long intervall);
    }
    public OnIntervallChoosen mOnIntervallChoosen;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.fragment_intervall_picker, null);

        //Initialisierung SharedPreferences plus Editor
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int standartAnzahl = mSharedPreferences.getInt(getString(R.string.sharedAnzahl), 1);
        long standartIntervall = mSharedPreferences.getLong(getString(R.string.sharedIntervall), 0);
        //set up number picker
        np = view.findViewById(R.id.np);
        np.setMinValue(1);
        np.setMaxValue(5);
        np.setWrapSelectorWheel(false);
        np.setValue(standartAnzahl);

        //set up number picker2
        np2 = view.findViewById(R.id.np2);
        np2.setMinValue(0);
        np2.setMaxValue(10);
        np2.setValue((int) (standartIntervall / 60000));

        return new AlertDialog.Builder(getActivity()).setView(view)//Nicht zwei mal inflaten!!!!!!!!!
                .setTitle(R.string.Alarmintervall_Auswählen)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Intervall daten an Host schicken

                                //Wenn nur einmal: intervall 0, aber anzahl 3 genommen
                                if(np.getValue() == 1 || np2.getValue() == 0){
                                    mOnIntervallChoosen.sendIntervall(1, 0);
                                }else{
                                    mOnIntervallChoosen.sendIntervall(np.getValue(), np2.getValue() * 60000);
                                }
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
                mOnIntervallChoosen = (OnIntervallChoosen) getActivity();
            }else {
                mOnIntervallChoosen = (OnIntervallChoosen) getTargetFragment();
            }

        }catch (ClassCastException e){

        }
    }
}