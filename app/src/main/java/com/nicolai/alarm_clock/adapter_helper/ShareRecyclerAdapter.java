package com.nicolai.alarm_clock.adapter_helper;

import android.content.Context;
import android.os.Build;
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

public class ShareRecyclerAdapter extends RecyclerView.Adapter<ShareRecyclerAdapter.ShareHolder> {

    private static List<ShareContactsPOJO> mContacts; //TODO warum für if in onClick static?
    protected static int position;
    public int id;
    private static boolean ready;
    private DeleteMessageContact deleteInterface;

    public static class ShareHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView name, fake;
        private ImageView deleteImage;
        private ImageView profileThumb;
        private ImageButton star;
        private DatabaseReference mUsersDatabase;
        private Context context;

        public ShareHolder(View v) {
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

        }

        public void bindShares(final String ID, String userName, final int position) {

            //Trustedstar hier nicht anzigen
            star.setVisibility(View.INVISIBLE);

            if(!ID.isEmpty()){
                //Query users Database
                mUsersDatabase.child(ID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
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
                                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                            //Muss online geladen werden
                                            Picasso.get().load(thumbImage).placeholder(R.drawable.ic_account_circle_primary_24dp).into(profileThumb);
                                        }else{
                                            //Muss online geladen werden
                                            Picasso.get().load(thumbImage).into(profileThumb);
                                        }
                                    }
                                });

                                //Sonst default ProfileImage laden
                            }else {
                                profileThumb.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                            }
                        }else {

                            profileThumb.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                            //USER ist nicht mehr in Firebase TODO Vlt früher
                            deleteImage.performClick();
                            //mContacts.remove(position);
                            //notifyItemRemoved(position);


                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                //Deleteimage anzeigen
                deleteImage.setVisibility(View.VISIBLE);

            }else{//Wenn in ChooseActivity keiner ausgewählt wurde
                deleteImage.setVisibility(View.GONE);
                profileThumb.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }
            //User namen || name aus Kontaktbuch / wenn defaultanzeige den entsprechenden Text
            if(!userName.equals(itemView.getContext().getResources().getString(R.string.keinKontakt))){
                name.setText(userName);
                name.setBackground(null);
                fake.setBackground(null);
            }else{
                name.setText("");
                name.setBackground(itemView.getContext().getResources().getDrawable(R.drawable.empty_contact));
                fake.setBackground(itemView.getContext().getResources().getDrawable(R.drawable.empty_contact));
            }
        }
    }

    public ShareRecyclerAdapter(List<ShareContactsPOJO> contact, DeleteMessageContact deleteInterface) {
        mContacts = contact;
        this.deleteInterface = deleteInterface;
    }

    @Override
    public ShareHolder onCreateViewHolder(ViewGroup parent, int viewType) { //RecyclerAdapter.
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_single_layout, parent, false);
        return new ShareHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(final ShareRecyclerAdapter.ShareHolder holder, final int position) {

        holder.deleteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int pos = holder.getAdapterPosition();

                //Aus Map in AlarmClock entfernen
                boolean appMessage = mContacts.get(pos).isInApp();
                String id = mContacts.get(pos).getID();
                boolean trueShared = mContacts.get(pos).isTrueShared();
                deleteInterface.onDeleted(id, appMessage, trueShared); //Musch eigentlich nisch
                //TimeSeter.contactMap.remove(id);

                mContacts.remove(pos);
                notifyItemRemoved(pos);
            }
        });

        String Username = mContacts.get(position).getName();
        String id = mContacts.get(position).getID();

        holder.bindShares(id, Username, position);
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

