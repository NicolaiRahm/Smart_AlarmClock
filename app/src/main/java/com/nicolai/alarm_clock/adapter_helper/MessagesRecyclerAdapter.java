package com.nicolai.alarm_clock.adapter_helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.DeleteMessageContact;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.pojos.ShareContactsPOJO;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Nicolai on 30.08.2017.
 */

public class MessagesRecyclerAdapter extends RecyclerView.Adapter<MessagesRecyclerAdapter.MessagesHolder> {

    private static List<ShareContactsPOJO> mContacts; //TODO warum für if in onClick static?
    protected static int position;
    private Context context;
    public int id;
    private static boolean ready;
    private DeleteMessageContact deleteInterface;

    public static class MessagesHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView name, fake;
        private ImageView deleteImage;
        private ImageView profileThumb;
        private ImageButton star;
        private DatabaseReference mUsersDatabase;

        public MessagesHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.user_username);
            fake = v.findViewById(R.id.userFakeText);
            deleteImage = v.findViewById(R.id.deleteMessageContact);
            star = v.findViewById(R.id.trustedStar);
            profileThumb = v.findViewById(R.id.CCcircle_image);

            mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");

            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mContacts.get(0).getName().equals("Noch kein Kontakt ausgewählt!")){
                //TODO Toast
            }else{
                position = getAdapterPosition();
                String id = mContacts.get(position).getID();
            }
        }

        public void bindMessages(String ID, String userName, boolean inApp) {
            //TrustedStar hier nicht anzeigen
            star.setVisibility(View.INVISIBLE);

            //Delete Image nur anzeigen wenn das nicht die Defaultanzeige ist
            if(!ID.isEmpty()){
                deleteImage.setVisibility(View.VISIBLE);
            }else{//Wenn keiner in Auswahlactivity ausgewählt wurde
                deleteImage.setVisibility(View.GONE);
            }

            //Query users Database
            if(inApp){
                mUsersDatabase.child(ID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //String userName = dataSnapshot.child("name").getValue().toString();
                        final String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();
                        if(!thumbImage.isEmpty()){
                            Picasso.get().load(thumbImage).networkPolicy(NetworkPolicy.OFFLINE).into(profileThumb, new Callback() {
                                @Override
                                public void onSuccess() {
                                    //Wurde offline gefunden
                                }

                                @Override
                                public void onError(Exception e) {
                                    //Muss online geladen werden
                                    Picasso.get().load(thumbImage).into(profileThumb);
                                }
                            });

                            //DefaultImageAnzeigen
                        }else{
                            profileThumb.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        profileThumb.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                    }
                });
            }else if(!ID.isEmpty()){
                profileThumb.setImageResource(R.drawable.ic_sms_black_24dp);
            }

            //User namen || name aus Kontaktbuch / wenn defaultanzeige den entsprechenden Text
            if(!userName.equals(itemView.getContext().getResources().getString(R.string.keinKontakt))){
                name.setText(userName);
                name.setBackgroundResource(android.R.color.transparent);
                fake.setBackgroundResource(android.R.color.transparent);
            }else{
                profileThumb.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                name.setText("");
                name.setBackgroundResource(R.drawable.empty_contact);
                fake.setBackgroundResource(R.drawable.empty_contact);
            }

        }
    }


    public MessagesRecyclerAdapter(List<ShareContactsPOJO> contact, Context context, DeleteMessageContact deleteInterface) {
        mContacts = contact;
        this.context = context;
        this.deleteInterface = deleteInterface;
    }

    @Override
    public MessagesHolder onCreateViewHolder(ViewGroup parent, int viewType) { //RecyclerAdapter.
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_single_layout, parent, false);
        return new MessagesHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(final MessagesRecyclerAdapter.MessagesHolder holder, final int position) {

        holder.deleteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int pos = holder.getAdapterPosition();

                //Aus Map in AlarmClock entfernen
                boolean appMessage = mContacts.get(pos).isInApp();
                String id = mContacts.get(pos).getID();
                boolean trueShared = mContacts.get(pos).isTrueShared();
                deleteInterface.onDeleted(id, appMessage, trueShared);

                mContacts.remove(pos);
                notifyItemRemoved(pos);
            }
        });

        String Username = mContacts.get(position).getName();
        String id = mContacts.get(position).getID();
        boolean inApp = mContacts.get(position).isInApp();

        holder.bindMessages(id, Username, inApp);
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    public void update (List<ShareContactsPOJO> contacts){
        mContacts = contacts;
        notifyDataSetChanged();
    }
}
