package com.nicolai.alarm_clock.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.receiver_service.serviceUserProfile;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import androidx.core.app.JobIntentService;
import androidx.fragment.app.DialogFragment;
import de.hdodenhof.circleimageview.CircleImageView;


public class UserProfileDialog extends DialogFragment {

    CircleImageView profile_image;
    Button sRequest, dRequest;
    private DatabaseReference mRootRef;
    private FirebaseUser mCurrentUser;
    private String user_id, name, image_url;
    private ValueEventListener friendsListener, requestListener;

    protected String mCurrent_state;

    private View view;

    public UserProfileDialog() {
        // Required empty public constructor
    }

    public static UserProfileDialog newInstance(String Id, String name, String image_url) {
        UserProfileDialog profileDialog = new UserProfileDialog();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("id", Id);
        args.putString("name", name);
        args.putString("image_url", image_url);
        profileDialog.setArguments(args);

        return profileDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user_id = getArguments().getString("id");
        name = getArguments().getString("name");
        image_url = getArguments().getString("image_url");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.fragment_user_profile_dialog, null);

        //Initialise xmls
        profile_image = view.findViewById(R.id.profile_image);
        sRequest = view.findViewById(R.id.sendRequest);
        dRequest = view.findViewById(R.id.declineRequest);

        mCurrent_state = "not_friends";

        //Initialisiern der RootDatabase
        mRootRef = FirebaseDatabase.getInstance().getReference();
        //CurrentUser
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        //Userdaten in profile UI
        /*mRootRef.child("users").child(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    profile_image.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                profile_image.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }
        });*/

        //Image
        if(!image_url.isEmpty()){
            Picasso.get().load(image_url).networkPolicy(NetworkPolicy.OFFLINE).into(profile_image, new Callback() {
                @Override
                public void onSuccess() {
                    //Wurde offline gefunden
                }

                @Override
                public void onError(Exception e) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        //Muss online geladen werden
                        Picasso.get().load(image_url).placeholder(R.drawable.ic_account_circle_primary_24dp).into(profile_image);
                    }else{
                        //Muss online geladen werden
                        Picasso.get().load(image_url).into(profile_image);
                    }
                }
            });
        }else{
            profile_image.setImageResource(R.drawable.ic_account_circle_primary_24dp);
        }


        requestListener = new ValueEventListener() {//TODO IST NOCH NICH GANZ FLÜSSIG MIT ZWEI LISTENERN
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(user_id)){
                    String request_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                    //Ich habe von ihm in meinem Folder eine offene Anfrage
                    if(request_type.equals("received")){
                        mCurrent_state = "req_received";
                        sRequest.setText(R.string.sRequestAccept);
                        //Decline Button sichtbar machen
                        dRequest.setVisibility(View.VISIBLE);
                        dRequest.setEnabled(true);
                        //Ich habe ihm eine geschickt
                    }else if(request_type.equals("sent")){
                        mCurrent_state = "req_sent";
                        sRequest.setText(R.string.sRequestCancel);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        //Requeststatus
        mRootRef.child("Friend_req").child(mCurrentUser.getUid()).addValueEventListener(requestListener);



         friendsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(user_id)){
                    mCurrent_state = "friends";
                    sRequest.setText(getResources().getString(R.string.Unfriend, name));
                }else{
                    if(mCurrent_state.equals("friends")){
                        mCurrent_state = "not_friends";
                        sRequest.setText(R.string.sRequest);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        //Are we already friends?
        mRootRef.child("Friends").child(mCurrentUser.getUid()).addValueEventListener(friendsListener);


        //FriendRequestPOJO Button
        sRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), serviceUserProfile.class);
                i.setAction("sendReq");
                i.putExtra("current_state", mCurrent_state);
                i.putExtra("profile_id", user_id);
                i.putExtra("profile_name", name);
                i.putExtra("profile_image", image_url);
                JobIntentService.enqueueWork(getActivity(), serviceUserProfile.class, serviceUserProfile.JOB_ID, i);
                //getActivity().startService(i);

                switch (mCurrent_state) {
                    case "not_friends":
                        mCurrent_state = "req_sent";
                        sRequest.setText(R.string.sRequestCancel);
                        break;
                    case "req_sent":
                        mCurrent_state = "not_friends";
                        sRequest.setText(R.string.sRequest);
                        break;
                    case "req_received":
                        mCurrent_state = "friends";
                        sRequest.setText(getResources().getString(R.string.Unfriend, name));
                        //Anfrage ablehnen Button wieder unsichtbar
                        dRequest.setVisibility(View.GONE);
                        break;
                    case "friends":
                        mCurrent_state = "not_friends";
                        sRequest.setText(R.string.sRequest);
                        break;
                }
            }
        });

        //Decline Button onClickListener
        dRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), serviceUserProfile.class);
                i.setAction("deleteReq");
                i.putExtra("current_state", mCurrent_state);
                i.putExtra("profile_id", user_id);
                JobIntentService.enqueueWork(getActivity(), serviceUserProfile.class, serviceUserProfile.JOB_ID, i);
                //getActivity().startService(i);

                if(mCurrent_state.equals("req_sent")){//Anfrage abbrechen
                    mCurrent_state = "not_friends";
                    sRequest.setText(R.string.sRequest);
                }else{
                    //Anfrage ablehnen
                    mCurrent_state = "not_friends";
                    sRequest.setText(R.string.sRequest);
                    dRequest.setVisibility(View.GONE);
                }
            }
        });

         AlertDialog al = new AlertDialog.Builder(getActivity()).setView(view)//Nicht zwei mal inflaten!!!!!!!!!
                .create();

         al.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
         return al;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        mRootRef.child("Friends").child(mCurrentUser.getUid()).removeEventListener(friendsListener);
        mRootRef.child("Friend_req").child(mCurrentUser.getUid()).removeEventListener(requestListener);
    }
}
