package com.nicolai.alarm_clock.adapter_helper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nicolai.alarm_clock.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Nicolai on 30.08.2017.
 */

public class MockRecyclerAdapter_Receiver extends RecyclerView.Adapter<MockRecyclerAdapter_Receiver.ReceiverHolder> {

    public static class ReceiverHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView userImage;
        TextView userName, sendOn, status;

        public ReceiverHolder(View v) {
            super(v);

            userImage = v.findViewById(R.id.CCcircle_image);
            userName = v.findViewById(R.id.sharedAlarm_empfaengerName);
            sendOn = v.findViewById(R.id.sharedAlarm_empfängerSendOn);
            status = v.findViewById(R.id.sharedAlarm_empfaengerStatus);
        }

        @Override
        public void onClick(View v) {

        }

        public void bindWecker() {
            userImage.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            userName.setText(R.string.mockMessageSender);
            status.setText(R.string.status_aufgestanden);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
            calendar.set(Calendar.HOUR_OF_DAY, 7);
            calendar.set(Calendar.MINUTE, 30);
            long dateInMillis = calendar.getTimeInMillis() - 7*24*60*60*1000+(5*75*1000);
            DateFormat df2 = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
            String dateS = df2.format(dateInMillis);

            sendOn.setText(dateS);
        }
    }


    public MockRecyclerAdapter_Receiver(boolean b) {
    }

    @Override
    public ReceiverHolder onCreateViewHolder(ViewGroup parent, int viewType) { //RecyclerAdapter.
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row_shared_recyclerview, parent, false);
        return new ReceiverHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(final MockRecyclerAdapter_Receiver.ReceiverHolder holder, final int position) {
        holder.bindWecker();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }
}
