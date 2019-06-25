package com.nicolai.alarm_clock;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.adapter_helper.MessagesRecyclerAdapter;
import com.nicolai.alarm_clock.dialog.UserProfileDialog;
import com.nicolai.alarm_clock.pojos.FriendRequestPOJO;
import com.nicolai.alarm_clock.pojos.ShareContactsPOJO;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FriendsFragment extends Fragment implements DeleteMessageContact {

    private DatabaseReference mUsersDatabase, mFriendsDatabase, mRequestsDatabase, mTrusted;
    private RecyclerView mFriendsList, mFriendsReqList;
    private FirebaseAuth mAuth;
    private String mCurrent_user_name;
    private static String mCurrent_user_id;
    private View mainView;

    private RequestCount mInterfaceReqCount;

    private FirebaseRecyclerAdapter<Friends, FriendsViewHolder> friendsRecyclerViewAdapter;
    private FirebaseRecyclerAdapter<FriendRequestPOJO, RequestViewHolder> requestsRecyclerViewAdapter;

    public interface RequestCount {
        public void setRequestCount(int count);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mInterfaceReqCount = (RequestCount) context;
    }

    public FriendsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_friends, container, false);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrent_user_id);
        mRequestsDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req").child(mCurrent_user_id);
        mTrusted = FirebaseDatabase.getInstance().getReference().child("Trusted").child(mCurrent_user_id);

        //RecyclerView für Friends
        mFriendsList = (RecyclerView) mainView.findViewById(R.id.friends_list);
        //mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        //RecyclerView für FriendRequests
        mFriendsReqList = mainView.findViewById(R.id.friendsReq_list);
        mFriendsReqList.setLayoutManager(new LinearLayoutManager(getContext()));


        //Inflate the Layout for this fragment
        return mainView;
    }



    public void FriendsListAdaplter(){
        FirebaseRecyclerOptions<Friends> options =
                new FirebaseRecyclerOptions.Builder<Friends>()
                        .setQuery(mFriendsDatabase, Friends.class)
                        .build();

        friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, int position, @NonNull final Friends model) {

                //Bin values to viewObject
                //holder.setDate(model.getDate());

                //id of the (clicked) user
                final String list_user_id = getRef(holder.getAdapterPosition()).getKey();

                //trustedStar ByMe
                mTrusted.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(list_user_id)) {
                            holder.trusted(true);
                        } else {
                            holder.trusted(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //Query users Database
                mUsersDatabase.child(list_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("name")) {
                            String userName = dataSnapshot.child("name").getValue().toString();
                            String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();
                            String number = dataSnapshot.child("mobile_number").getValue().toString();

                            if (!number.isEmpty() && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                String contactName = getContactDisplayNameByNumber(number);
                                if (!contactName.equals("?")) {
                                    userName = contactName;
                                }
                            }

                            holder.setUser(userName, thumbImage);
                        } else {//Wenn user gelöscht wurde
                            mFriendsDatabase.child(list_user_id).removeValue();
                            //Auch aus Trusted
                            mTrusted.child(list_user_id).removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        UserProfileDialog profileDialog = UserProfileDialog.newInstance(list_user_id, holder.getName(), holder.getImageUrl());
                        profileDialog.show(fm, "profileDialog");
                    }
                });

                //Trust a friend
                holder.trustedStar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTrusted.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    if (dataSnapshot.hasChild(list_user_id)) {
                                        mTrusted.child(list_user_id).setValue(null);
                                        holder.trusted(false);
                                    } else {
                                        mTrusted.child(list_user_id).setValue(true);
                                        holder.trusted(true);
                                        trustedDialog();
                                    }
                                } else {//Der erste trusted Buddy
                                    mTrusted.child(list_user_id).setValue(true);
                                    holder.trusted(true);
                                    trustedDialog();
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });
            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.user_single_layout, parent, false);
                return new FriendsViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                if (friendsRecyclerViewAdapter.getItemCount() == 0) {
                    //Daten an Adapterklasse
                    List<ShareContactsPOJO> messagesContacts = new ArrayList<>();
                    messagesContacts.add(new ShareContactsPOJO("", getString(R.string.keinKontakt), false, false));

                    //Adapter initialisieren
                    MessagesRecyclerAdapter mAdapter = new MessagesRecyclerAdapter(messagesContacts, getActivity(), FriendsFragment.this);
                    mFriendsList.setAdapter(mAdapter);
                }else{
                    mFriendsList.setAdapter(friendsRecyclerViewAdapter);
                }

                //Buddy ist nicht auf TuckTack. Lad ihn eine
                if (friendsRecyclerViewAdapter.getItemCount() <= 2) {
                    //Initialisierung SharedPreferences plus Editor
                    SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

                    androidx.constraintlayout.widget.Group groupeFriends = mainView.findViewById(R.id.NoFriensdYetGroupe);
                    groupeFriends.setVisibility(View.VISIBLE);

                    //Hat schon Vollversion oder nisch
                    if(mSharedPreferences.getBoolean(getString(R.string.sharedFullVersion), false)){
                        TextView text = mainView.findViewById(R.id.ffText);
                        text.setText(R.string.noFriendsYetButFullVersion);
                    }

                    ImageButton shareActivity = mainView.findViewById(R.id.inviteFriendsButton);
                    shareActivity.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(getActivity(), Invite.class);
                            startActivity(i);
                        }
                    });

                }else{
                    ConstraintLayout constraintLayout = mainView.findViewById(R.id.clFriendsFragment);
                    ConstraintSet constraintSet = new ConstraintSet();
                    constraintSet.clone(constraintLayout);

                    constraintSet.connect(mFriendsList.getId(), ConstraintSet.BOTTOM, R.id.clFriendsFragment, ConstraintSet.BOTTOM, 0);
                    constraintSet.applyTo(constraintLayout);
                }
            }

            @Override
            public void onError(DatabaseError e) {

            }
        };

        //Firebase Ui in gang setzen
        friendsRecyclerViewAdapter.startListening();
    }

    public void FriendsReqListAdaplter(){
        FirebaseRecyclerOptions<FriendRequestPOJO> options =
                new FirebaseRecyclerOptions.Builder<FriendRequestPOJO>()
                        .setQuery(mRequestsDatabase, FriendRequestPOJO.class)
                        .build();

        requestsRecyclerViewAdapter = new FirebaseRecyclerAdapter<FriendRequestPOJO, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull final FriendRequestPOJO model) {

                //Bin values to viewObject
                //holder.setDate(model.getDate());

                //id of the (clicked) user
                final String list_user_id = getRef(position).getKey();

                String name = model.getName();
                final String number = model.getNumber();
                final String image = model.getImage();

                if(number != null && !number.isEmpty() && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                    String contactName = getContactDisplayNameByNumber(number);
                    if(!contactName.equals("?")){
                        name = contactName;
                    }
                }

                holder.setName(name);
                holder.setThumbImage(image);

                holder.info.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        UserProfileDialog profileDialog = UserProfileDialog.newInstance(list_user_id, holder.getName(), holder.getImage_url());
                        profileDialog.show(fm, "profileDialog");
                    }
                });
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.user_request_layout, parent, false);
                return new RequestViewHolder(view);
            }


            @Override
            public void onDataChanged() {
                //Wenn Anfrage von mir / an mich an MainActivity schocken ums in die Toolbar zu dingsen
                mInterfaceReqCount.setRequestCount(requestsRecyclerViewAdapter.getItemCount());
            }

            @Override
            public void onError(DatabaseError e) {
                //Toast.makeText(AllUsers.this, getString(R.string.error, e.toString()), Toast.LENGTH_SHORT).show();
            }
        };

        //Firebase Ui in gang setzen
        mFriendsReqList.setAdapter(requestsRecyclerViewAdapter);
        requestsRecyclerViewAdapter.startListening();
    }

    //Für RecyclerAdpater wenn noch keine Freunde
    @Override
    public void onDeleted(String id, boolean inApp, boolean trueShared) {

    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private ImageButton trustedStar;
        private DatabaseReference mFriendsDatabase;
        private TextView userName;
        private ImageView thumb_image;
        private String image_url, name;

        public FriendsViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            trustedStar = mView.findViewById(R.id.trustedStar);
            thumb_image = mView.findViewById(R.id.CCcircle_image);
            userName = mView.findViewById(R.id.user_username);
            mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrent_user_id);
        }

        /*public void setDate(String date) {
            TextView friendsDate = mView.findViewById(R.id.user_username);
            friendsDate.setText(date);
        }*/

        public void setUser(String user_name, final String bitmapUrl){

            image_url = bitmapUrl;
            name = user_name;

            //Füllen der views
            userName.setText(name);

            //Image mit Picasso library laden
            if(!bitmapUrl.isEmpty()){
                Picasso.get().load(bitmapUrl).networkPolicy(NetworkPolicy.OFFLINE).into(thumb_image, new Callback() {
                    @Override
                    public void onSuccess() {
                        //Wurde offline gefunden
                    }

                    @Override
                    public void onError(Exception e) {
                        //Placholder erst ab version
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            //Muss online geladen werden
                            Picasso.get().load(bitmapUrl).placeholder(R.drawable.ic_account_circle_primary_24dp).into(thumb_image);
                        }else{
                            //Muss online geladen werden
                            Picasso.get().load(bitmapUrl).into(thumb_image);
                        }
                    }
                });
            }else{
                thumb_image.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }
        }

        //Getters to show profile
        public String getImageUrl(){
            return image_url;
        }

        public String getName(){
            return name;
        }

        public void trusted(boolean trusted){
            if(trusted){
                trustedStar.setImageResource(android.R.drawable.btn_star_big_on);
            }else{
                trustedStar.setImageResource(android.R.drawable.btn_star_big_off);
            }
        }
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private ImageView info;
        private String name, image_url;

        private DatabaseReference mRootRef;

        public RequestViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            info = mView.findViewById(R.id.requestInfo);
            mRootRef = FirebaseDatabase.getInstance().getReference();
        }

        /*public void setDate(String date) {
            TextView friendsDate = mView.findViewById(R.id.user_username);
            friendsDate.setText(date);
        }*/

        public void setName(String user_name){
            name = user_name;

            TextView userName = mView.findViewById(R.id.user_username);
            userName.setText(name);
        }

        public void setThumbImage(String bitmapUrl){
            image_url = bitmapUrl;

            ImageView thumb_image = mView.findViewById(R.id.CCcircle_image);

            //Image mit Picasso library laden
            if(!bitmapUrl.isEmpty()){//TODO PLACEHOLDER: load().placeholder
                Picasso.get().load(bitmapUrl).into(thumb_image);
            }else{
                thumb_image.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }
        }

        public String getName() {
            return name;
        }

        public String getImage_url() {
            return image_url;
        }

    }

    //Show dialog when user is about to trust someone
    private void trustedDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
        .setTitle(R.string.autoDownload)
        .setMessage(R.string.autoDownloadExplanation)
        .setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        /*.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        holder.trusted(false);
                    }
                });

        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                holder.trusted(false);
            }
        });*/

        alertDialogBuilder.show();
    }

    //TODO vlt Async
    //Get contact name for number
    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    @Override
    public void onStart() {
        super.onStart();

        FriendsListAdaplter();
        FriendsReqListAdaplter();
    }

    @Override
    public void onStop(){
        super.onStop();
        //FirebaseListener ausschalten
        friendsRecyclerViewAdapter.stopListening();
        requestsRecyclerViewAdapter.stopListening();
    }
}



