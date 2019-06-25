package com.nicolai.alarm_clock.adapter_helper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.pojos.Timer_POJO;
import com.nicolai.alarm_clock.util.TimeUtil;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerView_TimerAdapter extends ListAdapter<Timer_POJO, RecyclerView_TimerAdapter.TimerHolder> {

    private final RecyclerView_TimerAdapter.EditTimer mEditTimer;

    //Interface for clicks
    public interface EditTimer{
        void itemClicked(int position);
        void onOffButton(int position);
    }


    //################## Holder

    public static class TimerHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private TextView time, name, point, endsAt;
        private ImageView timerStatus, newOne;
        protected ConstraintLayout constraintBackgroun;
        private WeakReference<EditTimer> mWeakRefernce;

        public TimerHolder(View v, RecyclerView_TimerAdapter.EditTimer listener) {
            super(v);
            mWeakRefernce = new WeakReference<>(listener);

            if(v.getId() == R.id.timerNewLayout){
                newOne = v.findViewById(R.id.newOne);
            }else{
                time = v.findViewById(R.id.time);
                point = v.findViewById(R.id.point);
                name = v.findViewById(R.id.name);
                timerStatus = v.findViewById(R.id.timerStatusIcon);
                endsAt = v.findViewById(R.id.endsIn);
                constraintBackgroun = v.findViewById(R.id.view_foreground);

                //OnClickListeners
                timerStatus.setOnClickListener(this);
            }


            //OnClickListeners
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            final int vID = v.getId();

            if(getItemViewType() == 1){
                mWeakRefernce.get().itemClicked(getAdapterPosition());
            }else{
                //OnOff Button
                if(vID == timerStatus.getId()){
                    mWeakRefernce.get().onOffButton(getAdapterPosition());

                    //Whole row was clicked
                }else {
                    mWeakRefernce.get().itemClicked(getAdapterPosition());
                }
            }
        }

        public void bindWecker(final Timer_POJO currentTimer) {

            if(currentTimer.getId() == -10){

            }else{
                time.setText(TimeUtil.timerTimeView(itemView.getContext(), currentTimer.getMillisLeft()));
                if(currentTimer.getName().isEmpty() || !currentTimer.getState().isRunning()){
                    point.setVisibility(View.GONE);
                }else{
                    point.setVisibility(View.VISIBLE);
                }
                name.setText(currentTimer.getName());
                timerStatus.setImageResource(currentTimer.getState().isRunning() ? R.drawable.ic_timer_accent_24dp : R.drawable.ic_timer_off_dam_24dp);
                endsAt.setText(TimeUtil.endsAt(itemView.getContext(), currentTimer.getEnds()));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return 1;
        }
        return 2;
    }

    @Override
    public TimerHolder onCreateViewHolder(ViewGroup parent, int viewType) { //RecyclerAdapter.
        View inflatedView;
        switch (viewType){
            //Last item
            case 1: inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_new_row, parent, false); break;
            default: inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_itme_row, parent, false); break;
        }
        return new TimerHolder(inflatedView, mEditTimer);
    }

    //#################


    public RecyclerView_TimerAdapter(RecyclerView_TimerAdapter.EditTimer editAlarmClock) {
        super(DIFF_CALLBACK);

        mEditTimer = editAlarmClock;
    }

    public static final DiffUtil.ItemCallback<Timer_POJO> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Timer_POJO>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull Timer_POJO oldTimer, @NonNull Timer_POJO newTimer) {
                    // User properties may have changed if reloaded from the DB, but ID is fixed
                    return oldTimer.getId() == newTimer.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull Timer_POJO oldTimer, @NonNull Timer_POJO newTimer) {
                    // NOTE: if you use equals, your object must properly override Object#equals()
                    // Incorrectly returning false here will result in too many animations.
                    return oldTimer.equals(newTimer);
                }
            };

    @Override
    public void onBindViewHolder(@NonNull TimerHolder holder, int position) {

        Timer_POJO currentTimer = getItem(position);
        holder.bindWecker(currentTimer);
    }



    //Costum methods
    public int getID(int position){
        return getItem(position).getId();
    }
}
