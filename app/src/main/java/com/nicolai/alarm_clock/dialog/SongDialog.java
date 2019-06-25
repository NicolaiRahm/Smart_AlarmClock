package com.nicolai.alarm_clock.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.nicolai.alarm_clock.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * Created by Nicolai on 25.08.2017.
 */

public class SongDialog extends DialogFragment {

    public interface OnSoundTypeChoosen{
        void sendSoundType(int soundType);
    }
    public OnSoundTypeChoosen mOnSoundTypeChoosen;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle saveInstanceState){

        //Builder Klasse für Dialogerzeugung
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.choose_alarmtone)
                .setItems(R.array.song_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(getTargetFragment() != null){
                            if(which == 0){//Ringtone
                                Intent i = new Intent();
                                getTargetFragment().onActivityResult(3, Activity.RESULT_OK, i);
                                dismiss();
                            }else if (which == 1){//Musik
                                Intent i = new Intent();
                                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
                                dismiss();
                            }else{//Spotify
                                Toast.makeText(getTargetFragment().getActivity(), getString(R.string.technischPossible), Toast.LENGTH_LONG).show();
                            }
                        }else {//Kommt von Standardeinstellungen
                            if(which == 0){//Ringtone
                                mOnSoundTypeChoosen.sendSoundType(1);
                                dismiss();
                            }else if (which == 1){//Musik
                                mOnSoundTypeChoosen.sendSoundType(3);
                                dismiss();
                            }else{//Spotify
                                //mOnSoundTypeChoosen.sendSoundType(5);
                                //dismiss();
                                Toast.makeText(getActivity(), getString(R.string.technischPossible), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                })

                //Abbrechen Button
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){}
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        try{
            if(getTargetFragment() == null){//Kommt von Standardeinstellungen
                mOnSoundTypeChoosen = (OnSoundTypeChoosen) getActivity();
            }

        }catch (ClassCastException e){

        }
    }
}
