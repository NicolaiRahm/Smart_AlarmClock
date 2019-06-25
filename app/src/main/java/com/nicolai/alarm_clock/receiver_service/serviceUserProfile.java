package com.nicolai.alarm_clock.receiver_service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class serviceUserProfile extends JobIntentService {
    private static final String SENDE_REQ = "sendReq";
    private static final String DELETE_REQ = "deleteReq";

    private static final String EXTRA_STATE = "current_state";
    private static final String EXTRA_ID = "profile_id";

    /**
     * Unique job ID for this service.
     */
    public static final int JOB_ID = 1002;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, serviceUserProfile.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (intent.getAction() != null) {
            final String mCurrent_state = intent.getStringExtra(EXTRA_STATE);
            final String user_id = intent.getStringExtra(EXTRA_ID);
            final String profileName = intent.getStringExtra("profile_name");
            final String profileImage = intent.getStringExtra("profile_image");

            switch (intent.getAction()){
                case SENDE_REQ: sendRequest(mCurrent_state, user_id, profileName, profileImage); break;
                case DELETE_REQ: deleteRequest(true, mCurrent_state, user_id); break;
                default: break;
            }
        }
    }


    private void deleteRequest(final boolean declineReq, final String mCurrent_state, String user_id){

        final String mCurrentUser = FirebaseAuth.getInstance().getUid();
        //Initialisiern der RootDatabase
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

        Map<String, Object> deleteRequestMap = new HashMap<String, Object>();
        //My RequestFolder
        deleteRequestMap.put("Friend_req/" + mCurrentUser + "/" + user_id, null);
        //potential friends folder
        deleteRequestMap.put("Friend_req/" + user_id + "/" + mCurrentUser, null);

        mRootRef.updateChildren(deleteRequestMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                //Fehler aufgetreten beim löschen der Requests
                if(databaseError != null){
                    Toast.makeText(getApplicationContext(), "FAILED", Toast.LENGTH_SHORT).show();
                }else{
                    if(mCurrent_state.equals("req_sent")){//Anfrage abbrechen
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.AnfrageCanceld), Toast.LENGTH_SHORT).show();
                    }else{
                        if(declineReq){//Anfrage ablehnen
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.AnfrageAbgelehnt), Toast.LENGTH_SHORT).show();
                        }else{//Anfrage bestätigen
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Friends), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }

    private void sendRequest(final String mCurrent_state, final String user_id, final String displayName, final String profileImage){

        final String mCurrentUser = FirebaseAuth.getInstance().getUid();
        //Initialisiern der RootDatabase
        final DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

        // ------------------------------ NOT FRIENDS STATE ------------------------
        if(mCurrent_state.equals("not_friends")){

            DatabaseReference myUserObject = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getUid());
            myUserObject.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String myName = dataSnapshot.child("name").getValue(String.class);
                    String myImage = dataSnapshot.child("thumb_image").getValue(String.class);
                    String myNumber = dataSnapshot.child("mobile_number").getValue(String.class);

                    Map<String, Object> requestMap = new HashMap<String, Object>();
                    //My Request folder
                    requestMap.put("Friend_req/" + mCurrentUser + "/" + user_id + "/request_type", "sent");
                    requestMap.put("Friend_req/" + mCurrentUser + "/" + user_id + "/name", displayName);
                    requestMap.put("Friend_req/" + mCurrentUser + "/" + user_id + "/image", profileImage);

                    //His/her request folder
                    requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser + "/request_type", "received");
                    requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser + "/name", myName);
                    requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser + "/number", myNumber);
                    requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser + "/image", myImage);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError != null){
                                Toast.makeText(getApplicationContext(), "FAILED", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.AnfrageVersendet), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        // ---------------------------------------- CANCEL REQUEST STATE -----------------------
        if(mCurrent_state.equals("req_sent")){
            deleteRequest(false, mCurrent_state, user_id);
        }
        //------------------------------------ REQUEST RECEIVED STATE ---------------------
        if(mCurrent_state.equals("req_received")){
            final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

            //Name and date for FriendsFolder
            HashMap<String, String> friendship = new HashMap<>();
            if(FirebaseAuth.getInstance().getCurrentUser().getDisplayName() != null){
                friendship.put("name", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            }else{
                friendship.put("name", "");
            }

            //Name and date for My FriendsFolder
            HashMap<String, String> friendship2 = new HashMap<>();
            friendship2.put("name", displayName);

            Map<String, Object> friendsMap = new HashMap<String, Object>();
            //My Friends folder
            friendsMap.put("Friends/" + mCurrentUser + "/" + user_id, friendship2);
            //His/her Friends folder
            friendsMap.put("Friends/" + user_id + "/" + mCurrentUser, friendship);

            mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null){
                        Toast.makeText(getApplicationContext(), "FAILED", Toast.LENGTH_SHORT).show();
                    }else{
                        //Delete Request
                        deleteRequest(false, mCurrent_state, user_id);
                    }
                }
            });
        }

        //----------------------------------------------- FRIENDS STATE (unfriend?) --------------

        if(mCurrent_state.equals("friends")){

            Map<String, Object> deleteFriendMap = new HashMap<String, Object>();
            //My Friends folder
            deleteFriendMap.put("Friends/" + mCurrentUser + "/" + user_id, null);
            //friends folder
            deleteFriendMap.put("Friends/" + user_id + "/" + mCurrentUser, null);
            //My Trusted folder
            deleteFriendMap.put("Trusted/" + mCurrentUser + "/" +  user_id, null);
            //his Trusted folder
            deleteFriendMap.put("Trusted/" + user_id + "/" + mCurrentUser, null);

            mRootRef.updateChildren(deleteFriendMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null){
                        String error = databaseError.getMessage();
                        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.Unfriended, displayName), Toast.LENGTH_SHORT).show();

                    }
                }
            });
        }
    }

}
