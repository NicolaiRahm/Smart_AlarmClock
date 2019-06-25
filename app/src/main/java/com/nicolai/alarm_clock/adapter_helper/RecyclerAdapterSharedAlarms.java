package com.nicolai.alarm_clock.adapter_helper;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nicolai.alarm_clock.ControlCenter;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Nicolai on 30.08.2017.
 */

public class RecyclerAdapterSharedAlarms extends RecyclerView.Adapter<RecyclerAdapterSharedAlarms.WeckerHolder> {

    private static List<WeckerPOJO> mWecker; //TODO warum für if in onClick static?
    protected static int position;
    public int id;
    private static boolean ready;
    public ControlCenter sc;
    private final RecyclerAdapterSharedAlarms.SharedByMeClicked mClickInterface;

    //Interface for clicks
    public interface SharedByMeClicked{
        void itemClicked(int id);
    }

    public static class WeckerHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView name, time, daysView;
        private ImageView formeImg, repeat;
        public ConstraintLayout viewForeground;

        private TextView userName, alarmName, alarmTime;
        private ImageView profileImage;

        private WeakReference<RecyclerAdapterSharedAlarms.SharedByMeClicked> mWeakRefernce;

        public WeckerHolder(View v, RecyclerAdapterSharedAlarms.SharedByMeClicked listener) {
            super(v);
            mWeakRefernce = new WeakReference<>(listener);

            if(v.getId() == R.id.card_view){
                userName = v.findViewById(R.id.message_userName);
                alarmName = v.findViewById(R.id.message_alarmName);
                alarmTime = v.findViewById(R.id.message_alarmTime);
                profileImage = v.findViewById(R.id.CCcircle_image);
            }else{
                ready = false;
                name = (TextView) v.findViewById(R.id.sharedAlarm_username);
                time = (TextView) v.findViewById(R.id.sharedAlarm_time);
                formeImg = v.findViewById(R.id.sharedAlarm_auchfuermich);
                repeat = v.findViewById(R.id.sharedAlarmRepeat);
                daysView = v.findViewById(R.id.sharedAlarmDays);
                viewForeground = v.findViewById(R.id.view_foreground_sharedAlarm);
            }
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mWeakRefernce.get().itemClicked(mWecker.get(getAdapterPosition()).getId());
        }

        public void bindWecker(String nameWecker, int hour, int minute, boolean forme, int weckerID, boolean isRepeat, String daysX) {

            if(weckerID == -10){
                userName.setText(R.string.mockMessageSender);
                alarmName.setText(R.string.mockMessageName);
                alarmTime.setText(R.string.mockMessageTime);
                profileImage.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }else{
                //LautsprecherImg wenn der Alarm auch bei mir klingelt
                if(forme){
                    formeImg.setImageAlpha(40);
                    formeImg.setVisibility(View.VISIBLE);
                }else{
                    formeImg.setVisibility(View.INVISIBLE);
                }
                //WeeklyRepeat?!
                if(isRepeat){
                    repeat.setImageAlpha(40);
                    repeat.setVisibility(View.VISIBLE);
                }else{
                    repeat.setVisibility(View.INVISIBLE);
                }
                //Name des Weckers
                name.setText(nameWecker);
                //Zeitanzeige
                if(minute < 10){
                    if(hour == 30){
                        time.setVisibility(View.INVISIBLE);
                    }else{
                        time.setText(hour + ":0" + minute);
                    }
                }else{
                    time.setText(hour + ":" + minute);
                }

                showDays(daysX);
            }
        }

        //Visualisierung der Tage wenn da kein Sendername steht
        private void showDays(String days){
            String Mo = "" + itemView.getResources().getString(R.string.Mo).charAt(0);
            String Di = "" + itemView.getResources().getString(R.string.Di).charAt(0);//"<font color='#EE0000'>red</font>";
            String Mi = "" + itemView.getResources().getString(R.string.Mi).charAt(0);
            String Do = "" + itemView.getResources().getString(R.string.Do).charAt(0);
            String Fr = "" + itemView.getResources().getString(R.string.Fr).charAt(0);
            String Sa = "" + itemView.getResources().getString(R.string.Sa).charAt(0);
            String So = "" + itemView.getResources().getString(R.string.So).charAt(0);

            if(days.charAt(0) != 'x'){
                Mo = "<font color='#00cc00'>" + Mo + "</font>";
            }

            if(days.charAt(2) != 'x'){
                Di = "<font color='#00cc00'>" + Di + "</font>";
            }

            if(days.charAt(4) != 'x'){
                Mi = "<font color='#00cc00'>" + Mi + "</font>";
            }

            if(days.charAt(6) != 'x'){
                Do = "<font color='#00cc00'>" + Do + "</font>";
            }

            if(days.charAt(8) != 'x'){
                Fr = "<font color='#00cc00'>" + Fr + "</font>";
            }

            if(days.charAt(10) != 'x'){
                Sa = "<font color='#00cc00'>" + Sa + "</font>";
            }

            if(days.charAt(12) != 'x'){
                So = "<font color='#00cc00'>" + So + "</font>";
            }

            daysView.setText(Html.fromHtml(Mo + " " + Di + " " + Mi + " " + Do + " " + Fr + " " + Sa + " " + So));
        }
    }


    public RecyclerAdapterSharedAlarms(List<WeckerPOJO> wecker, ControlCenter sc, RecyclerAdapterSharedAlarms.SharedByMeClicked mClickInterface) {
        this.sc = sc;
        mWecker = wecker;
        this.mClickInterface = mClickInterface;
    }

    @Override
    public WeckerHolder onCreateViewHolder(ViewGroup parent, int viewType) { //RecyclerAdapter.
        View inflatedView;
        switch (viewType){
            //Mock message
            case 2: inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_rowmessages, parent, false); break;
            //Normaler Wecker
            default: inflatedView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_item_rowshared, parent, false); break;
        }
        return new WeckerHolder(inflatedView, mClickInterface);
    }

    @Override
    public void onBindViewHolder(final RecyclerAdapterSharedAlarms.WeckerHolder holder, final int position) {

        String nameWecker = mWecker.get(position).getName();
        int hour = mWecker.get(position).getHour();
        int minute = mWecker.get(position).getMinute();
        final int id = mWecker.get(position).getId();
        char shared = mWecker.get(position).getShared();
        boolean forme = mWecker.get(position).isFor_me();
        boolean repeat = mWecker.get(position).isWeekly_repeat();
        String days = mWecker.get(position).getDays();

        holder.itemView.setTag(R.id.KEY1, id);
        holder.itemView.setTag(R.id.KEY2, shared);

        holder.bindWecker(nameWecker, hour, minute, forme, id, repeat, days); //TODO binWecker da rein
    }

    @Override
    public int getItemCount() {
        return mWecker.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(mWecker.get(position).getId() == -10){
            //Ist der mock wecker
            return 2;
        }
        return 1;
    }

    public void removeItem(RecyclerView.ViewHolder viewHolder) {
        int pos = viewHolder.getAdapterPosition();
        mWecker.remove(pos);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(pos);
    }

    public void upgrade (List<WeckerPOJO> wecker){
        mWecker = wecker;
        notifyDataSetChanged();
    }



    public void restoreItem(WeckerPOJO deleted, int position) {
        mWecker.add(position, deleted);
        //notify item added by position
        notifyItemInserted(position);
    }

    public void delete(){
        mWecker.remove(1);
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

    }
}