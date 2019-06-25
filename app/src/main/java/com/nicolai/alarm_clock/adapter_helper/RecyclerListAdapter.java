package com.nicolai.alarm_clock.adapter_helper;

import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerListAdapter extends ListAdapter<WeckerPOJO, RecyclerListAdapter.WeckerHolder> {

    private final RecyclerListAdapter.EditAlarmClock mEditAlarmClock;
    //Für Message by buddies deleted
    private static HashMap<Integer, String> appMessageJasonMap = new HashMap<Integer, String>();

    //Interface for clicks
    public interface EditAlarmClock{
        void itemClicked(int position);
        void onOffButton(int position);

        void byMessageBuddyDeleted(int position);

        void acceptSharedAction(int position);

        void deleteAll();
    }


    //################## Holder

    public static class WeckerHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private TextView name, time, von, status, repeat, alarm, Mo, Di, Mi, Do, Fr, Sa, So, deleted, deleteAll;
        private CheckBox onPause;
        private ImageView repeatImage, sharedImage, circleProfile, messageImage, infoApprove, mockImage;
        protected ConstraintLayout constraintBackgroun;
        private FrameLayout approveCheck;
        private ImageButton check, checkBuddyDeleted;
        private Button noUpdate;
        private Group group;
        private String deleters;
        private WeakReference<RecyclerListAdapter.EditAlarmClock> mWeakRefernce;

        public WeckerHolder(View v, RecyclerListAdapter.EditAlarmClock listener) {
            super(v);
            mWeakRefernce = new WeakReference<>(listener);

            if(v.getId() == R.id.mockWecker){
                mockImage = v.findViewById(R.id.mockImage);
                constraintBackgroun = v.findViewById(R.id.mockWecker);
            }else if(v.getId() == R.id.lastItem){
                deleteAll = v.findViewById(R.id.deleteAll);
                deleteAll.setOnClickListener(this);
            }else{
                name = (TextView) v.findViewById(R.id.nameWecker);
                time = (TextView) v.findViewById(R.id.time);
                von = (TextView) v.findViewById(R.id.vonXY2);
                onPause = (CheckBox) v.findViewById(R.id.onOff);
                repeatImage = (ImageView) v.findViewById(R.id.imageView3);
                repeatImage.setImageAlpha(40);
                sharedImage = (ImageView) v.findViewById(R.id.imageViewShared);
                sharedImage.setImageAlpha(40);
                messageImage = v.findViewById(R.id.itemRowMessage);
                messageImage.setImageAlpha(40);
                constraintBackgroun = v.findViewById(R.id.view_foreground);
                approveCheck = v.findViewById(R.id.approveCheck);
                infoApprove = v.findViewById(R.id.infoApproveCheck);

                group = v.findViewById(R.id.groupSharedAlarm);
                status = v.findViewById(R.id.sharedAlarmStatus);
                repeat = v.findViewById(R.id.sharedAlarm_repeat);
                alarm = v.findViewById(R.id.sharedAlarm_alarm);

                check = v.findViewById(R.id.sharedAlarm_accept);
                noUpdate = v.findViewById(R.id.sharedAlarm_notAccept);
                circleProfile = v.findViewById(R.id.mockImage);

                deleted = v.findViewById(R.id.messageAlarm_deleted);

                checkBuddyDeleted = v.findViewById(R.id.message_buttonBuddyDeleted);

                Mo = v.findViewById(R.id.detailsMo);
                Di = v.findViewById(R.id.detailsDi);
                Mi = v.findViewById(R.id.detailsMi);
                Do = v.findViewById(R.id.detailsDo);
                Fr = v.findViewById(R.id.detailsFr);
                Sa = v.findViewById(R.id.detailsSa);
                So = v.findViewById(R.id.detailsSo);

                //OnClickListeners
                v.setOnClickListener(this);
                onPause.setOnClickListener(this);

                approveCheck.setOnClickListener(this);
                infoApprove.setOnClickListener(this);
                check.setOnClickListener(this);
                //noUpdate.setOnClickListener(this);

                checkBuddyDeleted.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {

            final int vID = v.getId();

            if(getItemViewType() == 2) {
                if (!deleteAll.getText().equals(itemView.getContext().getString(R.string.NoWecker))) {
                    mWeakRefernce.get().deleteAll();
                }
                //OnOff Button
            }else if(vID == onPause.getId()){
                mWeakRefernce.get().onOffButton(getAdapterPosition());

                //General FrameLayout "New", "Updated" ...
            }else if(vID == approveCheck.getId() || vID == infoApprove.getId()){
                if(group.getVisibility() == View.GONE){
                    group.setVisibility(View.VISIBLE);
                    infoApprove.setImageResource(R.drawable.ic_arrow_drop_up_accent_24dp);
                }else{
                    group.setVisibility(View.GONE);
                    infoApprove.setImageResource(R.drawable.ic_arrow_drop_down_accent_24dp);
                }

                //Aenderung besteatigen
            }else if(vID == check.getId()){
                group.setVisibility(View.GONE);
                approveCheck.setVisibility(View.GONE);

                mWeakRefernce.get().acceptSharedAction(getAdapterPosition());

                //Gelesen und deletedFolder aus messages ByMe löschen
            }else if (vID == checkBuddyDeleted.getId()) {
                deleted.setVisibility(View.GONE);
                checkBuddyDeleted.setVisibility(View.GONE);

                mWeakRefernce.get().byMessageBuddyDeleted(getAdapterPosition());

                //Ganze Reihe wurde geklickt
            }else {
                //Damit Mock alarm nicht geclickt werden kann
                if (getItemViewType() != 3) {
                    //WeakReference Irgendwat mit garbageCollector
                    mWeakRefernce.get().itemClicked(getAdapterPosition());
                }
            }
        }

        public void bindWecker(final WeckerPOJO currentWecker) {

            char shared = currentWecker.getShared();
            if(shared != 'b' && shared != 's' && shared != 'u' && shared != 'd'){
                if(getAdapterPosition() == 0){
                    constraintBackgroun.setBackgroundResource(R.drawable.alarm_recycler_background);
                }else if(getItemViewType() != 2){
                    constraintBackgroun.setBackgroundResource(android.R.color.white);
                }
            }else{
                if(getAdapterPosition() == 0){
                    constraintBackgroun.setBackgroundResource(R.drawable.alarm_recycler_backgroun_shared);
                }else {
                    constraintBackgroun.setBackgroundResource(R.color.forMe);
                }
            }

            //Mock alarm
            if(currentWecker.getId() == -9){
                mockImage.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }else if(currentWecker.getId() == -1){
                //Kein Wecker vorhanden
                deleteAll.setText(currentWecker.getName());
            }else if(currentWecker.getId() == -2){
                //Delete all
                deleteAll.setText(currentWecker.getName());
            }else{


                if(currentWecker.isOn_off()){
                    onPause.setChecked(true);
                }else{
                    onPause.setChecked(false);
                }


                //TODO IN ByMessageBuddyDeleted checken -->funkt ja wohl nich richtig wenn das async is Message
                if(!currentWecker.getShared_message_con().isEmpty() && !currentWecker.getShared_message_con().equals("{}")){
                    //Message image visible
                    messageImage.setVisibility(View.VISIBLE);
                    String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference dr = FirebaseDatabase.getInstance().getReference().child("messages").child(current_user_id)
                            .child("ByMe").child("" + currentWecker.getId()).child("deleted");

                    dr.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                deleters = "";
                                //String in HashMap
                                HashMap<String, String> appMessageMap = new HashMap<String, String>();

                                try {
                                    JSONObject json = new JSONObject(currentWecker.getShared_message_con());
                                    JSONArray names = json.names();
                                    for (int i = 0; i < names.length(); i++) {
                                        String key = names.getString(i);
                                        appMessageMap.put(key, json.optString(key));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                //Namen aller Löscher
                                Map<String,Object> users = (Map<String,Object>) dataSnapshot.getValue();
                                //iterate through each user, ignoring their UID
                                for (final Map.Entry<String, Object> entry : users.entrySet()){

                                    //Users Database
                                    DatabaseReference usersDB = FirebaseDatabase.getInstance().getReference().child("users");

                                    usersDB.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists()){
                                                deleters = deleters + dataSnapshot.child(entry.getKey()).child("name").getValue().toString() + ", ";
                                                deleted.setText(itemView.getContext().getResources().getString(R.string.message_deletedByBuddy, deleters));
                                            }else{
                                                deleters = deleters + itemView.getContext().getResources().getString(R.string.user_deleted) + ", ";
                                                deleted.setText(deleters);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                                    //deleten
                                    appMessageMap.remove(entry.getKey());
                                }

                                //Zum abholen in Alarms onClick method
                                appMessageJasonMap.put(currentWecker.getId(), new JSONObject(appMessageMap).toString());

                                //Updaten bei klick auf bySharedBuddy deleted Button

                                deleted.setVisibility(View.VISIBLE);
                                checkBuddyDeleted.setVisibility(View.VISIBLE);
                            }else{
                                if(deleted.getVisibility() == View.VISIBLE){
                                    deleted.setVisibility(View.GONE);
                                    checkBuddyDeleted.setVisibility(View.GONE);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }else{
                    //Wenn SMS verschickt wird oder auch nisch
                    if(currentWecker.getShared_sms_con().isEmpty() || currentWecker.getShared_sms_con().equals("{}")){
                        messageImage.setVisibility(View.GONE);
                    }else {
                        messageImage.setVisibility(View.VISIBLE);
                    }

                    if(deleted.getVisibility() == View.VISIBLE){
                        deleted.setVisibility(View.GONE);
                        checkBuddyDeleted.setVisibility(View.GONE);
                    }
                }

                //SenderImage wenn von Außerhalb
                if(!currentWecker.getThumb_img().isEmpty()){
                    Picasso.get().load(currentWecker.getThumb_img()).networkPolicy(NetworkPolicy.OFFLINE).into(circleProfile, new Callback() {
                        @Override
                        public void onSuccess() {
                            //Wurde offline gefunden
                        }

                        @Override
                        public void onError(Exception e) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                //Muss online geladen werden
                                Picasso.get().load(currentWecker.getThumb_img()).placeholder(R.drawable.ic_account_circle_primary_24dp).into(circleProfile);
                            }else{
                                //Muss online geladen werden
                                Picasso.get().load(currentWecker.getThumb_img()).into(circleProfile);
                            }
                        }
                    });

                    //Default ProfileImage
                }else if (shared == 'b' || shared == 's' || shared == 'u' || shared == 'd'){
                    circleProfile.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                }


                //Wöchentliche Wiederholung
                if(currentWecker.isWeekly_repeat()){
                    repeatImage.setVisibility(View.VISIBLE);
                }
                //Name des Weckers
                name.setText(currentWecker.getName());
                //Zeitanzeige
                if(currentWecker.getMinute() < 10){
                    time.setText(currentWecker.getHour() + ":0" + currentWecker.getMinute());
                }else{
                    time.setText(currentWecker.getHour() + ":" + currentWecker.getMinute());
                }

                //Alles von mir
                if(shared == 'l' || shared == 'v' || shared == 'm' || shared == 't'){//Besnders bei delete vieler hintereinander wichtig
                    if(group.getVisibility() == View.VISIBLE){
                        group.setVisibility(View.GONE);
                    }
                    onPause.setVisibility(View.VISIBLE);
                    approveCheck.setVisibility(View.GONE);
                    circleProfile.setVisibility(View.INVISIBLE);

                    if(currentWecker.getShared() == 'v' || currentWecker.getShared() =='t'){
                        sharedImage.setVisibility(View.VISIBLE);
                    }else {
                        sharedImage.setVisibility(View.GONE);
                    }

                    //Visualisierung der Tage wenn da kein Sendername steht
                    showDays(currentWecker.getDays());
                }


                //Alles von Außerhalb
                if(shared == 'b' || shared == 's' || shared == 'u' || shared == 'd'){
                    if(group.getVisibility() == View.VISIBLE){
                        group.setVisibility(View.GONE);
                    }
                    //Generell
                    von.setText(currentWecker.getVon_xy());
                    circleProfile.setVisibility(View.VISIBLE);

                    //Unterschied zwischen bestätigt und nicht bestätigt
                    if(currentWecker.getShared() == 'b'){
                        if(group.getVisibility() == View.VISIBLE){
                            group.setVisibility(View.GONE);
                        }
                        onPause.setVisibility(View.VISIBLE);
                        approveCheck.setVisibility(View.GONE);
                    }else {
                        onPause.setVisibility(View.GONE);
                        approveCheck.setVisibility(View.VISIBLE);

                        //Vorschau die bei Klick auf den infoButton gezeigt wird
                        fillGroupe(currentWecker.getAnzahl(), currentWecker.getIntervall(), currentWecker.getDays(), currentWecker.isWeekly_repeat());
                    }

                    //Status im FrameLayout setzen
                    switch (shared){
                        case 's': status.setText(itemView.getContext().getResources().getString(R.string.sAlarmNeu)); break;
                        case 'u':
                            //Updates nicht übernehmen button
                            //noUpdate.setVisibility(View.VISIBLE); TODO Vlt doch
                            status.setText(itemView.getContext().getResources().getString(R.string.sAlarmUpdated)); break;
                        case 'd': status.setText(itemView.getContext().getResources().getString(R.string.sAlarmDeleted));break;
                    }
                }
            }
        }

        //Vorschau die bei Klick auf den infoButton gezeigt wird
        private void fillGroupe(int anzahl, long intervall, String days, boolean repeatWeekly){
            //alarm
            if(anzahl == 1){
                String once = itemView.getContext().getResources().getString(R.string.alarmOnce);
                alarm.setText(itemView.getResources().getString(R.string.message_alarmAlarm, once));
            }else{
                String ausdruck = "" + anzahl + " * " + intervall/60000;
                alarm.setText(itemView.getResources().getString(R.string.message_alarmAlarm, ausdruck));
            }

            //repeat
            if(repeatWeekly){
                repeat.setText(itemView.getContext().getResources().getString(R.string.message_alarmRepeatYes));
            }

            //Days
            if(days.charAt(0) != 'x'){
                Mo.setBackground(itemView.getResources().getDrawable(R.drawable.message_alarm_days));
            }else{
                Mo.setBackground(itemView.getResources().getDrawable(R.drawable.search_layout));
            }

            if(days.charAt(2) != 'x'){
                Di.setBackground(itemView.getResources().getDrawable(R.drawable.message_alarm_days));
            }else{
                Di.setBackground(itemView.getResources().getDrawable(R.drawable.search_layout));
            }

            if(days.charAt(4) != 'x'){
                Mi.setBackground(itemView.getResources().getDrawable(R.drawable.message_alarm_days));
            }else{
                Mi.setBackground(itemView.getResources().getDrawable(R.drawable.search_layout));
            }

            if(days.charAt(6) != 'x'){
                Do.setBackground(itemView.getResources().getDrawable(R.drawable.message_alarm_days));
            }else{
                Do.setBackground(itemView.getResources().getDrawable(R.drawable.search_layout));
            }

            if(days.charAt(8) != 'x'){
                Fr.setBackground(itemView.getResources().getDrawable(R.drawable.message_alarm_days));
            }else{
                Fr.setBackground(itemView.getResources().getDrawable(R.drawable.search_layout));
            }

            if(days.charAt(10) != 'x'){
                Sa.setBackground(itemView.getResources().getDrawable(R.drawable.message_alarm_days));
            }else{
                Sa.setBackground(itemView.getResources().getDrawable(R.drawable.search_layout));
            }

            if(days.charAt(12) != 'x'){
                So.setBackground(itemView.getResources().getDrawable(R.drawable.message_alarm_days));
            }else{
                So.setBackground(itemView.getResources().getDrawable(R.drawable.search_layout));
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

            von.setText(Html.fromHtml(Mo + " " + Di + " " + Mi + " " + Do + " " + Fr + " " + Sa + " " + So));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(getItem(position).getId() == -9){
            //Ist der mock wecker
            return 3;
        }else if(position == getItemCount()-1){
            //Ist das letzte Element
            return 2;
        }
        return 1;
    }

    @Override
    public RecyclerListAdapter.WeckerHolder onCreateViewHolder(ViewGroup parent, int viewType) { //RecyclerAdapter.
        View inflatedView;
        switch (viewType){
            //Last item
            case 2: inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_row_last, parent, false); break;
            //Mock wecker
            case 3: inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_mock_wecker, parent, false); break;
            //Normaler Wecker
            default: inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_row, parent, false); break;
        }
        return new RecyclerListAdapter.WeckerHolder(inflatedView, mEditAlarmClock);
    }

    //#################


    public RecyclerListAdapter(RecyclerListAdapter.EditAlarmClock editAlarmClock) {
        super(DIFF_CALLBACK);

        mEditAlarmClock = editAlarmClock;
    }

    public static final DiffUtil.ItemCallback<WeckerPOJO> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WeckerPOJO>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull WeckerPOJO oldWecker, @NonNull WeckerPOJO newWecker) {
                    // User properties may have changed if reloaded from the DB, but ID is fixed
                    return oldWecker.getId() == newWecker.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull WeckerPOJO oldWecker, @NonNull WeckerPOJO newWecker) {
                    // NOTE: if you use equals, your object must properly override Object#equals()
                    // Incorrectly returning false here will result in too many animations.
                    if (oldWecker.getId() == -1 || oldWecker.getId() == -2 || oldWecker.getId() == -9){
                        if(oldWecker.getId() == -2 && !oldWecker.getName().equals(newWecker.getName())){
                            return false;
                        }
                        return true;
                    }
                    return oldWecker.equals(newWecker);
                }
            };

    @Override
    public void onBindViewHolder(@NonNull WeckerHolder holder, int position) {

        WeckerPOJO currentWecker = getItem(position);

        holder.bindWecker(currentWecker);
    }

    public int getID(int position){
        return getItem(position).getId();
    }

    //Damit list element gleich und animation flüssiger läuft
    public void setOnOff(int position, boolean onOff){
        getItem(position).setOn_off(onOff);
    }

    //Für onClick Message deleted by Alarmbuddy
    public String getAppMessageMap(int id){
       return appMessageJasonMap.get(id);
    }
}
