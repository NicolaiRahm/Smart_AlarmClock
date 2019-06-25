package com.nicolai.alarm_clock.dialog;

/**
 * Created by Nicolai on 09.03.2018.
 */

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.nicolai.alarm_clock.R;

import androidx.fragment.app.DialogFragment;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener{
    private int hourBegin;
    private int minuteBegin;

    public interface OnTimeChoosen{
        void sendTime(int hour, int minute);
    }

    public OnTimeChoosen mOnTimeChoosen;

    public static TimePickerFragment newInstance(int hour, int minute) {
        TimePickerFragment fragment = new TimePickerFragment();
        Bundle args = new Bundle();
        args.putInt("hour", hour);
        args.putInt("minute", minute);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            hourBegin = getArguments().getInt("hour");
            minuteBegin = getArguments().getInt("minute");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        //Create and return a new instance of TimePickerDialog
        return new TimePickerDialog(getActivity(), R.style.TimeDialogTheme,this, hourBegin, minuteBegin,
                DateFormat.is24HourFormat(getActivity()));

    }

    //onTimeSet() callback method
    public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfPick){
        mOnTimeChoosen.sendTime(hourOfDay, minuteOfPick);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mOnTimeChoosen.sendTime(hourBegin, minuteBegin);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        if(getTargetFragment() != null){
            mOnTimeChoosen = (OnTimeChoosen) getTargetFragment();
        }
    }
}