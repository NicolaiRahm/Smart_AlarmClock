package com.nicolai.alarm_clock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nicolai.alarm_clock.dialog.FillAlertDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.DialogFragment;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity implements FillAlertDialog.OnInfoGiven , BillingClientStateListener, PurchasesUpdatedListener {

    private TextView email, userTextView, google2;
    private EditText emailAender, usernameAendern;
    private ImageView circleProfilePicture;
    private Button changeImage;
    private ProgressBar progressBar;
    protected String myNumber, providerId, myUsername, userId, toVerifyNumber;
    private FirebaseUser user;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAlarmsDatabaseReference;
    private StorageReference mImageStore;
    private Bitmap thumb_bitmap;
    private byte[] thumb_bite;
    private ImageButton deleteImgButton;

    private BillingClient mBillingClient;

    private boolean refreshedLogin, foundImage;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    private static final int GALERY_PICK = 100;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    public static final String ADDITIONAL_INFO_NUMBER = "number";
    public static final String ADDITIONAL_INFO_NAME = "name";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        //Response of FirebaseUI
        if(requestCode == 1){
            if(resultCode == RESULT_OK){
                refreshedLogin = true;
            }
        }


        if (requestCode == GALERY_PICK) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                // start cropping activity for pre-acquired image saved on the device
                CropImage.activity(imageUri)
                        .setAspectRatio(1,1)
                        .setMinCropWindowSize(500,500)
                        .start(this);

            } else if(resultCode == RESULT_CANCELED){

            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                //Hide image
                circleProfilePicture.setVisibility(View.INVISIBLE);
                //Start progressbar
                progressBar.setVisibility(View.VISIBLE);

                //Uri vom gecroopten Image
                final Uri resultUri = result.getUri();
                //In CircleImage
                Drawable newImg = null;
                try {
                    InputStream inputStream = getContentResolver().openInputStream(resultUri);
                    newImg = Drawable.createFromStream(inputStream, resultUri.toString());
                } catch (FileNotFoundException e) {

                }

                //Save to local storage
                try {
                    saveToInternalStorage(MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri));
                }
                catch (Exception e) {
                    //handle exception
                }


                //Uri to File
                File thumb_filePath = new File(resultUri.getPath());

                //Compress image for thumb
                try {
                    thumb_bitmap = new Compressor(this)
                    .setMaxHeight(200)
                    .setMaxWidth(200)
                    .setQuality(100)
                    .compressToBitmap(thumb_filePath);
                } catch(IOException ie) {
                    ie.printStackTrace();
                }

                //Upload bitMap
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
                thumb_bite = baos.toByteArray();

                uploadBitMap(newImg);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Aktuelles Userobjekt + Uid
        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user.getUid();


        //Initialisierung SharedPreferences plus Editor
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        //Database instance
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //Referenc auf mein userObject in database
        mAlarmsDatabaseReference = mFirebaseDatabase.getReference().child("users").child(userId);

        //My Username
        if(user.getDisplayName() != null && !user.getDisplayName().isEmpty()){
            myUsername = user.getDisplayName();
        }

        userTextView = (TextView) findViewById(R.id.usernameView);
        userTextView.setText(myUsername);

        //My Email
        if(user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()){
            myNumber = user.getPhoneNumber();
        }

        email = (TextView) findViewById(R.id.emailView);
        email.setText(getString(R.string.EmailVar, myNumber));

        //Delete button
        deleteImgButton = findViewById(R.id.deleteImgButton);

        //Progressbar
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);

        //Profile Picture
        circleProfilePicture = (ImageView) findViewById(R.id.circleImageView);
        //Lade das Image aus dem Gerätspeicher
        loadImageFromStorage();


        mAlarmsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(user.getDisplayName() == null || user.getDisplayName().isEmpty()){
                    if(dataSnapshot.child("name").exists()){
                        myUsername = dataSnapshot.child("name").getValue().toString();
                        userTextView.setText(myUsername);
                    }
                }

                if(user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()){
                    if(dataSnapshot.child("mobile_number").exists()){
                        myNumber = dataSnapshot.child("mobile_number").getValue().toString();
                        email.setText(getString(R.string.EmailVar, myNumber));
                    }
                }

                if(!foundImage){
                    //Image mit Picasso library laden
                    if(dataSnapshot.child("thumb_image").getValue() != null
                            && !dataSnapshot.child("thumb_image").getValue().toString().isEmpty()){

                        final String firebase_image_url = dataSnapshot.child("thumb_image").getValue().toString();
                        //Load image with picasso
                        Picasso.get().load(firebase_image_url).networkPolicy(NetworkPolicy.OFFLINE).into(circleProfilePicture, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(Exception e) {
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                    //Muss online geladen werden
                                    Picasso.get().load(firebase_image_url).placeholder(R.drawable.ic_account_circle_primary_24dp).into(circleProfilePicture);
                                }else{
                                    //Muss online geladen werden
                                    Picasso.get().load(firebase_image_url).into(circleProfilePicture);
                                }
                            }
                        });

                        deleteImgButton.setVisibility(View.VISIBLE);

                    }else {
                        circleProfilePicture.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SettingsActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
                circleProfilePicture.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }
        });

        //Button zum Bildändern
        changeImage = (Button) findViewById(R.id.changeImage);
        changeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALERY_PICK);

                // start picker to get image for cropping and then use the image in cropping activity
                /*CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(SettingsActivity.this);*/
            }
        });

        //Storage root reference für Profilepicture
        mImageStore = FirebaseStorage.getInstance().getReference();

        //Initialisierung Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.Profil));


        //ProviderID
        for (UserInfo profile : user.getProviderData()) {
            // Id of the provider (ex: google.com)
            providerId = profile.getProviderId();
        }

        //Set Up?????????????????????????????????ß
        if(getIntent().getAction() != null && getIntent().getAction().equals("SetUp")){
            //Hat der google user die voll version schon bezahlt und jetzt einen neuen Account?
            mBillingClient = BillingClient.newBuilder(getApplicationContext()).setListener(this).build();
            mBillingClient.startConnection(this);
            //Weiter gehts in onSetupFinished Listener von mBillingClient

            //Delete Account gone --> Start guide und Datenschutz visible
            Group deleteAccount = findViewById(R.id.deletAccountGroup);
            deleteAccount.setVisibility(View.GONE);

            Group startGuide = findViewById(R.id.guideGroupe);
            startGuide.setVisibility(View.VISIBLE);

            //Für main
            mEditor.putBoolean("new", false);
            mEditor.apply();

            String providerId = "";
            //ProviderID
            for (UserInfo profile : user.getProviderData()) {
                // Id of the provider (ex: google.com)
                providerId = profile.getProviderId();
            }

            //Welcher provider --> ist phone number oder username schon hinzugefügt?
            if(providerId.equals("google.com")){
                //Ask for phone_number
                DatabaseReference number = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("mobile_number");
                number.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists() || dataSnapshot.getValue() == null || dataSnapshot.getValue().toString().isEmpty()){
                            fillAlert(ADDITIONAL_INFO_NUMBER);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }else{
                //Ask for "emergancy user name"
                DatabaseReference number = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("name");
                number.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists() || dataSnapshot.getValue() == null || dataSnapshot.getValue().toString().isEmpty()){
                            fillAlert(ADDITIONAL_INFO_NAME);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }
    }

    public void startGuideButton(View view){
        startActivity(new Intent(this, Details_Guide.class));
    }
    public void viewPrivacyPolicy(View view){
        startActivity(new Intent(this, Legal.class));
    }

    //Show alertdialog to enter mobile number or user name
    public void fillAlert(String what){
        DialogFragment additionalInfo = new FillAlertDialog();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("type", what);
        additionalInfo.setArguments(args);

        additionalInfo.show(getSupportFragmentManager(),what);
    }

    @Override
    public void sendInfo(String info, String type) {
        if(type.equals(ADDITIONAL_INFO_NAME)){
            //Upload user name
            FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                    .child("name").setValue(info);

            userTextView.setText(info);

            Toast.makeText(SettingsActivity.this, getString(R.string.greating, info), Toast.LENGTH_SHORT).show();
        }else {
            if(info.startsWith("+")){
                FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                        .child("mobile_number").setValue(info.replace(" ", ""));

                email.setText(info.replace(" ", ""));
            }else{
                Toast.makeText(this, getString(R.string.start_with_country_code), Toast.LENGTH_LONG).show();
                fillAlert(ADDITIONAL_INFO_NUMBER);
            }
        }
    }

    //Number aendern
    public void aendern (View view){

        if(providerId.equals("google.com") || providerId.equals("password") || refreshedLogin){
            //EditText initialisieren
            if(emailAender == null){
                emailAender = (EditText) findViewById(R.id.nummerAendern);
                emailAender.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            boolean validNumber = true;
                            String text = emailAender.getText().toString().replace(" ", "");

                            //Wenn das eingegebene eine nummer sein soll
                            if(mVerificationId == null && !text.startsWith("+")){
                                validNumber = false;
                                emailAender.setVisibility(View.GONE);
                                Toast.makeText(SettingsActivity.this, getString(R.string.start_with_country_code), Toast.LENGTH_LONG).show();
                            }

                            if(validNumber){
                                //Speichern der neuen Nummer
                                saveNumber(text);
                                emailAender.setText(null);
                                emailAender.setVisibility(View.GONE);
                                return true;
                            }

                            // hide virtual keyboard
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(emailAender.getWindowToken(), 0);
                        }
                        return false;
                    }
                });
            }


            if(emailAender.getVisibility() == View.GONE){
                emailAender.setVisibility(View.VISIBLE);

                if(providerId.equals("phone")){
                    email.setText(getString(R.string.message_cost_disclaimer));
                }

            }else{
                emailAender.setVisibility(View.GONE);

                if(providerId.equals("phone")){
                    email.setText(getString(R.string.EmailVar, myNumber));
                }
            }
        }else{
            new AlertDialog.Builder(this)
                    .setMessage(R.string.relogin_number)
                    .setPositiveButton(R.string.weiter, (dialog, id) -> signIn())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    //Username aendern
    public void aendernUsername (View view){
        //EditText initialisieren
        if(usernameAendern == null){
            usernameAendern = (EditText) findViewById(R.id.usernameAendern);
            usernameAendern.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        //Speichern der neuen Nummer
                        saveUsername(usernameAendern.getText().toString());
                        usernameAendern.setText(null);
                        usernameAendern.setVisibility(View.GONE);
                        return true;
                    }
                    return false;
                }
            });
        }

        //Is email changeable?
        if(!providerId.equals("google.com")){
            if(usernameAendern.getVisibility() == View.GONE){
                usernameAendern.setVisibility(View.VISIBLE);
            }else{
                usernameAendern.setVisibility(View.GONE);
            }
        }else{
            google2 = findViewById(R.id.google2);
            if(google2.getVisibility() == View.GONE){
                google2.setVisibility(View.VISIBLE);
            }else{
                google2.setVisibility(View.GONE);
            }
        }
    }

    //AlertDialog zum Account löschen bestätigen
    public void aendernAccount (View view){
        if(refreshedLogin){
            new AlertDialog.Builder(this)
                    .setMessage(R.string.wirklichLoeschen)
                    .setPositiveButton(R.string.Ja, (dialog, id) -> deleteAccount())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }else{
            new AlertDialog.Builder(this)
                    .setMessage(R.string.relogin_delete_account)
                    .setPositiveButton(R.string.weiter, (dialog, id) -> signIn())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }



    //Hochladen der neuen nummer
    private void saveNumber(final String newNumber){

        //Verify / save new number
        if(mVerificationId == null){
            toVerifyNumber = newNumber;



            //Is loged in by phone number?
            if(providerId.equals("phone")){
                PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // This callback will be invoked in two situations:
                        // 1 - Instant verification. In some cases the phone number can be instantly
                        //     verified without needing to send or enter a verification code.
                        // 2 - Auto-retrieval. On some devices Google Play services can automatically
                        //     detect the incoming verification SMS and perform verification without
                        //     user action.

                        user.updatePhoneNumber(credential)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    email.setText(getString(R.string.EmailVar, newNumber));

                                    mVerificationId = null;
                                    mResendToken = null;

                                    emailAender.setVisibility(View.GONE);
                                    emailAender.setHint(R.string.emailAendern);

                                    Toast.makeText(SettingsActivity.this, getString(R.string.updated), Toast.LENGTH_SHORT).show();
                                }
                            });
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        // This callback is invoked in an invalid request for verification is made,
                        // for instance if the the phone number format is not valid.


                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Invalid request
                            // ...
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            // The SMS quota for the project has been exceeded
                            // ...
                        }

                        //Message to the user
                        mVerificationId = null;
                        mResendToken = null;

                        emailAender.setVisibility(View.GONE);
                        emailAender.setHint(R.string.emailAendern);

                        Toast.makeText(SettingsActivity.this, getString(R.string.updated), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId,
                                           PhoneAuthProvider.ForceResendingToken token) {
                        // The SMS verification code has been sent to the provided phone number, we
                        // now need to ask the user to enter the code and then construct a credential
                        // by combining the code with a verification ID.


                        // Save verification ID and resending token so we can use them later
                        mVerificationId = verificationId;
                        mResendToken = token;

                        emailAender.setVisibility(View.VISIBLE);
                        emailAender.setHint(R.string.verification_code);
                    }
                };


                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        newNumber,        // Phone number to verify
                        60,                 // Timeout duration
                        TimeUnit.SECONDS,   // Unit of timeout
                        this,               // Activity (for callback binding)
                        mCallbacks);        // OnVerificationStateChangedCallbacks
            }else{
                email.setText(getString(R.string.EmailVar, newNumber));
            }

            //In database
            mAlarmsDatabaseReference.child("mobile_number").setValue(newNumber).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        if (mVerificationId == null){
                            Toast.makeText(SettingsActivity.this, getString(R.string.updated), Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        if (mVerificationId == null){
                            Toast.makeText(SettingsActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }else {
            //Check code
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, newNumber);

            user.updatePhoneNumber(credential)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                email.setText(getString(R.string.EmailVar, toVerifyNumber));

                                mVerificationId = null;
                                mResendToken = null;

                                emailAender.setVisibility(View.GONE);
                                emailAender.setHint(R.string.emailAendern);
                            }
                        }
                    });
        }
    }

    //Hochladen des neuen usernames
    private void saveUsername(final String newUsername){
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            userTextView.setText(newUsername);
                        }
                    }
                });

        //In database
        mAlarmsDatabaseReference.child("name").setValue(newUsername).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(SettingsActivity.this, getString(R.string.updated), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(SettingsActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //Account löschen
    private void deleteAccount(){
        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            //damit wenn neuer Account erstellt wird token wieder gesendet wird
                            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                            mEditor.putBoolean(getString(R.string.sharedTokenSend), false);
                            mEditor.apply();

                            //Damit in Scrolling registriert wird das nicht mehr eingelogt
                            Intent i = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(i);
                        }
                    }
                });
    }


    private void saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("TuckTack", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile_image.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Initialisierung SharedPreferences plus Editor
        mEditor.putString(getString(R.string.sharedFullProfileImage), directory.getAbsolutePath());
        mEditor.apply();
    }

    private void loadImageFromStorage() {
        String path = mSharedPreferences.getString(getString(R.string.sharedFullProfileImage), "");
        if(!path.isEmpty()){
            try {
                File f= new File(path, "profile_image.jpg");
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
                foundImage = true;
                circleProfilePicture.setImageBitmap(b);
                deleteImgButton.setVisibility(View.VISIBLE);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }
    }





    //Starts signIn Flow damit neu eingelogt wird
    private void signIn(){
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build());


        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        //.setLogo(R.drawable.my_great_logo)
                        //.setTheme(R.style.MySuperAppTheme)
                        .build(),
                1);
    }

    //upload BitMap
    private void uploadBitMap(final Drawable newImgFull){
        final StorageReference thumb_file = mImageStore.child("profile_img").child(userId);

        final UploadTask uploadTask = thumb_file.putBytes(thumb_bite);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                //Url laden
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //In database
                        mAlarmsDatabaseReference.child("thumb_image").setValue(uri.toString());

                        Toast.makeText(SettingsActivity.this, getResources().getString(R.string.ProfilbildUpdate), Toast.LENGTH_SHORT).show();
                        //Progressbar beende + Bild wieder anzeigen
                        progressBar.setVisibility(View.GONE);
                        if(newImgFull != null){
                            circleProfilePicture.setImageDrawable(newImgFull);
                        }
                        circleProfilePicture.setVisibility(View.VISIBLE);
                        deleteImgButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SettingsActivity.this, getResources().getString(R.string.error, e.toString()), Toast.LENGTH_SHORT).show();
                //Progressbar beende + Bild wieder anzeigen
                progressBar.setVisibility(View.GONE);
                circleProfilePicture.setVisibility(View.VISIBLE);
            }
        });
    }

    //Profilbild löschen
    public void deleteImg(View view){
        circleProfilePicture.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        final StorageReference thumb_file = mImageStore.child("profile_img").child(userId);

        // Delete the file
        thumb_file.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //In database
                mAlarmsDatabaseReference.child("thumb_image").setValue("");

                Toast.makeText(SettingsActivity.this, getResources().getString(R.string.img_deleted), Toast.LENGTH_SHORT).show();
                //Progressbar beende + Bild wieder anzeigen
                progressBar.setVisibility(View.GONE);
                circleProfilePicture.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                circleProfilePicture.setVisibility(View.VISIBLE);

                //Button wieder verstecken
                deleteImgButton.setVisibility(View.GONE);

                //aus SharedPref löschen
                mEditor.putString(getString(R.string.sharedFullProfileImage), "");
                mEditor.apply();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(SettingsActivity.this, getResources().getString(R.string.error, exception.toString()), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                circleProfilePicture.setVisibility(View.VISIBLE);
            }
        });
    }


    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    }

    @Override
    public void onBillingSetupFinished(int responseCode) {
        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);

        if(purchasesResult.getPurchasesList() != null && !purchasesResult.getPurchasesList().isEmpty()){
            for (Purchase purchase : purchasesResult.getPurchasesList()) {
                if(purchase.getPackageName().equals(getPackageName())){
                    DatabaseReference fullVersion = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                            .child("full_version");
                    fullVersion.setValue("payed");
                }
            }
        }
    }

    @Override
    public void onBillingServiceDisconnected() {

    }
}
