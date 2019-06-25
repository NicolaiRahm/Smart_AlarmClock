package com.nicolai.alarm_clock;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.nicolai.alarm_clock.adapter_helper.RecyclerSearch;
import com.nicolai.alarm_clock.dialog.Coutry_code_picker;
import com.nicolai.alarm_clock.dialog.UserProfileDialog;
import com.nicolai.alarm_clock.pojos.SearchPOJO;

import java.util.ArrayList;
import java.util.List;

public class AllUsers extends AppCompatActivity implements Coutry_code_picker.OnCodeChoosen, RecyclerSearch.UserClicked {

    private RecyclerView mRecyclerView;
    private MaterialSearchView searchView;

    private Group contactsButton, secondGroup;

    private static final int CONTACTS = 435;

    private String [] permissionsContacts = {Manifest.permission.READ_CONTACTS};
    private static final int REQUEST_READ_CONTACTS_PERMISSION = 657;

    private DatabaseReference searchdb;
    private DatabaseReference search_resultdb;
    private ValueEventListener searchListener;
    private RecyclerSearch mAdapter;
    private List<SearchPOJO> searchList = new ArrayList<SearchPOJO>();
    private String currentName;
    private ProgressBar progressBar;

    //Country Code wenn nummer in Kontaktbuch mit 0 beginnt
    @Override
    public void sendCode(String code, String number, String name) {
        //Andere UI anzeigen
        contactsButton.setVisibility(View.GONE);
        secondGroup.setVisibility(View.VISIBLE);
        String numberWithCode = "+" + code + number.substring(1);

        currentName = name;
        progressBar.setVisibility(View.VISIBLE);

        //Suche nach der nummer
        search_resultdb.removeValue();
        searchdb.child("number").setValue(numberWithCode);
        search_resultdb.addValueEventListener(searchListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){
            case MaterialSearchView.REQUEST_VOICE:{
                if(resultCode == RESULT_OK){
                    ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && matches.size() > 0) {
                        String searchWrd = matches.get(0);
                        if (!TextUtils.isEmpty(searchWrd)) {
                            searchView.setQuery(searchWrd, false);
                        }
                    }
                }
                break;
            }

            case CONTACTS:{
                if(resultCode == RESULT_OK){

                    Uri contactData = data.getData();
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        String number = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        number = number.replace(" ", "");
                        String name = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        c.close();

                        if(number.charAt(0) != '+' || (number.charAt(0) == '0' && number.charAt(1) != '0')){
                            DialogFragment newFragment = new Coutry_code_picker();

                            Bundle args = new Bundle();
                            args.putString("number", number);
                            args.putString("name", name);
                            newFragment.setArguments(args);

                            newFragment.show(getSupportFragmentManager(),"CountryCodePicker");
                        }else{
                            //Andere UI anzeigen
                            contactsButton.setVisibility(View.GONE);
                            secondGroup.setVisibility(View.VISIBLE);

                            //Damit genormt gesucht wird
                            if(number.charAt(0) == '0' && number.charAt(1) == '0'){
                                number = "+" + number.substring(2);
                            }

                            currentName = name;
                            progressBar.setVisibility(View.VISIBLE);

                            //Suche nach der nummer
                            search_resultdb.removeValue();
                            searchdb.child("number").setValue(number).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        search_resultdb.addValueEventListener(searchListener);
                                    }else {
                                        Toast.makeText(AllUsers.this, task.getException().toString(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }

                    break;
                }
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbarAllUsers);
        setSupportActionBar(mToolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.AllUsers));

        contactsButton = findViewById(R.id.groupByContacts);
        secondGroup = findViewById(R.id.groupSecondAllUsers);

        mRecyclerView = (RecyclerView) findViewById(R.id.allUsersRecyclerView);
        //mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RecyclerSearch(searchList, this);
        mRecyclerView.setAdapter(mAdapter);

        searchView = findViewById(R.id.search_view);
        searchView.setVoiceSearch(true);

        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if(cm.getActiveNetworkInfo() != null) {
                    currentName = query;
                    progressBar.setVisibility(View.VISIBLE);

                    search_resultdb.removeValue();
                    searchdb.child("name").setValue(query);
                    search_resultdb.addValueEventListener(searchListener);

                    Group group = findViewById(R.id.groupInviteFriends);
                    group.setVisibility(View.GONE);
                }else{
                    Toast.makeText(AllUsers.this, R.string.connection, Toast.LENGTH_LONG).show();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //Do some magic
                return false;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                //Andere UI anzeigen
                contactsButton.setVisibility(View.GONE);
                secondGroup.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSearchViewClosed() {
                //Do some magic
            }

        });


        //Listener for search result
        searchdb = FirebaseDatabase.getInstance().getReference().child("search")
                .child(FirebaseAuth.getInstance().getUid());

        search_resultdb = FirebaseDatabase.getInstance().getReference().child("search_result")
                .child(FirebaseAuth.getInstance().getUid());

        searchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if(dataSnapshot.exists()){
                        if(dataSnapshot.hasChild("nr")){
                            List<SearchPOJO> list = new ArrayList<SearchPOJO>();
                            progressBar.setVisibility(View.GONE);
                            mAdapter.upgrade(list);
                            Toast.makeText(AllUsers.this, R.string.noUser, Toast.LENGTH_LONG).show();

                            Group group = findViewById(R.id.groupInviteFriends);
                            group.setVisibility(View.VISIBLE);
                            ImageButton button = findViewById(R.id.allUsersShareButton);

                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent i = new Intent(AllUsers.this, Invite.class);
                                    startActivity(i);
                                }
                            });

                            //Initialisierung SharedPreferences plus Editor
                            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(AllUsers.this);
                            if(mSharedPreferences.getBoolean(getString(R.string.sharedFullVersion), false)){
                                TextView text = findViewById(R.id.allUsersNoFriendsYet);
                                text.setText(R.string.noFriendsYetButFullVersion);
                            }
                        }else{
                            List<SearchPOJO> list = new ArrayList<SearchPOJO>();
                            for (DataSnapshot idANDimage : dataSnapshot.getChildren()) {
                                String userId = idANDimage.getKey();
                                String image = idANDimage.getValue(String.class);
                                list.add(new SearchPOJO(userId, currentName, image));
                            }

                            progressBar.setVisibility(View.GONE);
                            mAdapter.upgrade(list);
                        }
                    }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
            }
        };

        progressBar = findViewById(R.id.progressSearch);
    }

    //Show profile
    @Override
    public void userClicked(String userId, String name, String image) {
        FragmentManager fm = getSupportFragmentManager();
        UserProfileDialog profileDialog = UserProfileDialog.newInstance(userId, name, image);
        profileDialog.show(fm, "profileDialog");
    }

    //Choose a user from contacts
    public void chooseFromContacts(View view){
        Group group = findViewById(R.id.groupInviteFriends);
        group.setVisibility(View.GONE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED ){

            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm.getActiveNetworkInfo() != null){
                Intent pickContact = new Intent(Intent.ACTION_PICK);
                pickContact.setDataAndType(ContactsContract.Contacts.CONTENT_URI, ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(pickContact, CONTACTS);
            }else{
                Toast.makeText(this, R.string.connection, Toast.LENGTH_LONG).show();
            }
        }else{
            ActivityCompat.requestPermissions(this, permissionsContacts, REQUEST_READ_CONTACTS_PERMISSION);//TODO vlt. mit threads gucken
        }
    }

    //Firebase RecyclerAdapter Ui
    @Override
    protected void onStart(){
        super.onStart();

        //loadUsers();
    }

    @Override
    protected void onStop(){
        super.onStop();
        search_resultdb.removeValue();
        searchdb.removeEventListener(searchListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case REQUEST_READ_CONTACTS_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //TODO umständlich
                    FloatingActionButton fab = findViewById(R.id.fabSearchByContacts);
                    fab.performClick();
                }
                break;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        searchView.setMenuItem(item);

        return true;
    }
}
