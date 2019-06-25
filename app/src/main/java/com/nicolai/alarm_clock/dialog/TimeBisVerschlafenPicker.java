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

public class TimeBisVerschlafenPicker extends DialogFragment {

    private NumberPicker np;
    private NumberPicker np2;
    private View view;

    public interface OnTimeBisChoosen{
        void sendTimeBis(int minutes, int seconds);
    }
    public OnTimeBisChoosen mOnTimeBisChoosen;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.fragment_timebis_picker, null);

        //Initialisierung SharedPreferences plus Editor
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int standartSeconds = mSharedPreferences.getInt(getString(R.string.sharedTimeBisVerschlafen), 60);

        int minute = standartSeconds / 60;
        int seconds = standartSeconds - minute * 60;

        //set up number picker
        np = view.findViewById(R.id.npTB);
        np.setMinValue(0);
        np.setMaxValue(5);
        np.setWrapSelectorWheel(false);
        np.setValue(minute);

        //set up number picker2
        np2 = view.findViewById(R.id.npTB2);
        np2.setMinValue(0);
        np2.setMaxValue(59);
        np2.setValue(seconds);

        return new AlertDialog.Builder(getActivity()).setView(view)//Nicht zwei mal inflaten!!!!!!!!!
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Abstand daten an Host schicken
                                mOnTimeBisChoosen.sendTimeBis(np.getValue(), np2.getValue());
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
                mOnTimeBisChoosen = (OnTimeBisChoosen) getActivity();
            }else {
                mOnTimeBisChoosen = (OnTimeBisChoosen) getTargetFragment();
            }

        }catch (ClassCastException e){

        }
    }
}
