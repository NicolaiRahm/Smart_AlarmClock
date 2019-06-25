package com.nicolai.alarm_clock;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.pojos.FB;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChooseFriend extends AppCompatActivity {

    private EditText serachField;
    private ImageButton searchButton;
    private RecyclerView mFriendsSearchList;
    private FloatingActionButton floatingActionButton;
    //private Toolbar mToolbar;

    private DatabaseReference mUsersDatabase, mFriendsDatabase;
    private FirebaseAuth mAuth;
    private String mCurrent_user_id;
    protected static HashMap<String, String> myMap;

    FirebaseRecyclerOptions<Friends> options;

    private FirebaseRecyclerAdapter<Friends, FriendsSearchViewHolder> friendsRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_friend);

        //Share or Message
        TextView type = findViewById(R.id.FirebaseSearch);
        if(getIntent().getAction().equals("Message")){
            type.setText(R.string.ChooseFriendMessage);
        }else{
            type.setText(R.string.ChooseFriendAlarm);
        }

        serachField = findViewById(R.id.search_field);
        searchButton = findViewById(R.id.searchButton);
        mFriendsSearchList = findViewById(R.id.friends_search_list);
            //mFriendsList.setHasFixedSize(true);
            mFriendsSearchList.setLayoutManager(new LinearLayoutManager(this));
        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrent_user_id);
        floatingActionButton = findViewById(R.id.chosseThem);
        //Für ausgewählte Kontakte
        myMap = new HashMap<>();

        /*mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbarChoose);
            setSupportActionBar(mToolbar);
            final ActionBar ab = getSupportActionBar();
            ab.setTitle(getResources().getString(R.string.ChooseFriend));
            ab.setDisplayHomeAsUpEnabled(true);*/

        //Get JSON from intent
        String s = getIntent().getStringExtra("HashMap");
        if(!s.equals("{}")){
            try {
                JSONObject json = new JSONObject(s);
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    myMap.put(key, json.optString(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //Dismiss Keyboard on Activity start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //FloatingActionButton
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("HashMap", myMap);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        //Searchbutton
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchText = serachField.getText().toString();
                displaySearch(searchText);

                //Dismiss Keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(serachField.getWindowToken(), 0);
            }
        });

        //Search button of the keyboard
        serachField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchText = serachField.getText().toString();
                    displaySearch(searchText);

                    //Dismiss Keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(serachField.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });
    }

    private void displaySearch (String searchText){
        if(!searchText.isEmpty()){
            Query firebaseSearchQuery = mFriendsDatabase.orderByChild("name").startAt(searchText).endAt(searchText + "\uf8ff");

            options = new FirebaseRecyclerOptions.Builder<Friends>()
                            .setQuery(firebaseSearchQuery, Friends.class)
                            .build();
        }else{
            options = new FirebaseRecyclerOptions.Builder<Friends>()
                            .setQuery(mFriendsDatabase, Friends.class)
                            .build();
        }

        friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Friends, FriendsSearchViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsSearchViewHolder holder, int position, @NonNull final Friends model) {

                //Bin values to viewObject
                //holder.setDate(model.getDate());

                //id of the (clicked) user
                final String list_user_id = getRef(position).getKey();

                //Query users Database
                mUsersDatabase.child(list_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild("name")){
                            String userName = dataSnapshot.child("name").getValue().toString();
                            String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                            String number = dataSnapshot.child("mobile_number").getValue().toString();

                            if(!number.isEmpty() && ContextCompat.checkSelfPermission(ChooseFriend.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                                String contactName = getContactDisplayNameByNumber(number);
                                if(!contactName.equals("?")){
                                    userName = contactName;
                                }
                            }

                            //TODO wirwar
                            if(!userName.equals(model.getName())){
                                mFriendsDatabase.child(list_user_id).child("name").setValue(userName);
                            }

                            Boolean fullVersion = false;
                            if(dataSnapshot.child(FB.FULL_VERSION).exists()
                                    || (getIntent().getAction().equals("Message") /*&& dataSnapshot.child(FB.FREE_LEFT).exists()*/)){
                                fullVersion = true;
                            }
                            holder.setDetails(userName, thumbImage, fullVersion);
                        }else{
                            mFriendsDatabase.child(list_user_id).removeValue();
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //Schon ausgewählte Kontakte kenntlich machen
                if(myMap.containsKey(list_user_id)){
                    holder.alreadyClicked();
                }

                //Ein Freund mit keiner Vollversion soll ausgewählt werden
                holder.lockedUserImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(ChooseFriend.this, getString(R.string.lockedUser, holder.lockedUserClicked()), Toast.LENGTH_SHORT).show();
                    }
                });

                //OnClick
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        /*Intent userProfile = new Intent(ChooseFriend.this, UserProfile.class);
                        userProfile.putExtra("userId", list_user_id);
                        startActivity(userProfile);*/

                        holder.clicked(list_user_id, model.getName());
                    }
                });
            }

            @NonNull
            @Override
            public FriendsSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.user_single_layout, parent, false);
                return new FriendsSearchViewHolder(view);
            }
        };

        //Firebase Ui in gang setzen
        mFriendsSearchList.setAdapter(friendsRecyclerViewAdapter);
        friendsRecyclerViewAdapter.startListening();
    }

    //TODO vlt Async
    //Get contact name for number
    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = getContentResolver();
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
        displaySearch("");
    }

    @Override
    public void onStop(){
        super.onStop();
        //FirebaseListener ausschalten
        friendsRecyclerViewAdapter.stopListening();
    }

    public static class FriendsSearchViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private ImageView clicked, lockedUserImg;
        private ImageButton star;
        private TextView userName;

        public FriendsSearchViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            clicked = mView.findViewById(R.id.userChoosen);
            star = mView.findViewById(R.id.trustedStar);
            star.setVisibility(View.INVISIBLE);
            lockedUserImg = mView.findViewById(R.id.lockedUserImg);
        }

        public void alreadyClicked(){
            clicked.setVisibility(View.VISIBLE);
        }

        public void clicked(String id, String name){
            if(clicked.getVisibility() == View.VISIBLE){
                clicked.setVisibility(View.GONE);
                myMap.remove(id);
            }else{
                clicked.setVisibility(View.VISIBLE);
                myMap.put(id, name);
            }
        }

        public void setDetails(final String name, final String bitmapUrl, final boolean fullVersion){
            //Wenn der user keine Vollversion hat ist er nicht auswählbar
            if(!fullVersion){
                lockedUserImg.setVisibility(View.VISIBLE);
            }else{
                lockedUserImg.setVisibility(View.GONE);
            }
            //SET NAME
            userName = mView.findViewById(R.id.user_username);
            userName.setText(name);

            //SET IMAGE
            final ImageView thumb_image = mView.findViewById(R.id.CCcircle_image);

            //Image mit Picasso library laden && offline sichern
            if(!bitmapUrl.isEmpty()){//TODO PLACEHOLDER: load().placeholder
                Picasso.get().load(bitmapUrl).networkPolicy(NetworkPolicy.OFFLINE).into(thumb_image, new Callback() {
                    @Override
                    public void onSuccess() {
                        //Wurde offline gefunden
                    }

                    @Override
                    public void onError(Exception e) {
                        //Muss online geladen werden
                        Picasso.get().load(bitmapUrl).into(thumb_image);
                    }
                });
            }else{
                thumb_image.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }
        }

        public String lockedUserClicked(){
            return userName.getText().toString();
        }
    }
}
