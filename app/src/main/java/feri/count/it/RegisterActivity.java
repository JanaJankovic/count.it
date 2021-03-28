package feri.count.it;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import feri.count.datalib.User;

public class RegisterActivity extends AppCompatActivity {
    public static final String TAG = RegisterActivity.class.getSimpleName();
    private Button buttonBack;
    private Button buttonRegisterUser;
    private EditText edtEmail;
    private EditText edtUsername;
    private EditText edtPassword;

    private static final int RC_SIGN_IN = 100;
    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private DatabaseReference db;

    private ArrayList<User> listOfUsers = new ArrayList<>();
    private ChildEventListener userDataListener;

    private void bindGui() {
        buttonBack = (Button) findViewById(R.id.buttonBack);
        buttonRegisterUser = (Button) findViewById(R.id.buttonRegisterUser);

        edtEmail = (EditText) findViewById(R.id.edtEmail);
        edtUsername = (EditText) findViewById(R.id.edtEmailLog);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
    }

    private void initData() {
        db = FirebaseDatabase.getInstance().getReference();
        initUserDataListener();
        db.child(User.COLLECTION).addChildEventListener(userDataListener);
    }

    private void initUserDataListener() {
        userDataListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                User data = dataSnapshot.getValue(User.class);
                data.setId(dataSnapshot.getKey());

                listOfUsers.add(data);
                Log.i(TAG, "Add:"+dataSnapshot.getKey()+" "+data);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                User data = dataSnapshot.getValue(User.class);
                data.setId(dataSnapshot.getKey());

                //listOfUsers.put(dataSnapshot.getKey(), data);
                for(int i = 0; i < listOfUsers.size(); i++) {
                    if (data.getId().equals(listOfUsers.get(i).getId())) {
                        listOfUsers.set(i,data);
                        break;
                    }
                }

                Log.i(TAG, "Changed:"+dataSnapshot.getKey()+" "+data);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.i(TAG, "Removed:"+dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        transparentStatusAndNavigation();
        getSupportActionBar().hide();

        bindGui();
        initData();

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToLoginActivity(0);
            }
        });

        buttonRegisterUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerNewUser(v);
            }
        });


        mAuth = FirebaseAuth.getInstance();
    }

    private void transparentStatusAndNavigation() {
        //make full transparent statusBar
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, true);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    private void setWindowFlag(final int bits, boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @Override
    public void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.PhoneBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build()
            );

            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);

        }
        else updateUI(currentUser);
    }

    private void updateUI(FirebaseUser currentUser) {
        Log.i("TAG","User:" + currentUser.getEmail());
        db = FirebaseDatabase.getInstance().getReference();

        initUserDataListener();
        db.child(User.COLLECTION).addChildEventListener(userDataListener);

        //initDataCarListener();
        //mDatabase.child(MyDataCars.COLLECTION).addChildEventListener(myDataCarListener);
    }



    public void returnToLoginActivity(int wasUserRegistered) {
        Intent intent = new Intent();

        setResult(wasUserRegistered == 1 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    private boolean doesUserAlreadyExistInDb(User user) {
        for(int i = 0; i < listOfUsers.size(); i++) {
            if (user.getEmail().equals(listOfUsers.get(i).getEmail()) || user.getUsername().equals(listOfUsers.get(i).getUsername())) {
                return true;
            }
        }

        return false;
    }

    public void registerNewUser(View view) {
        String email = this.edtEmail.getText().toString(),
                username = this.edtUsername.getText().toString(),
                password = this.edtPassword.getText().toString();
        Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE),
                passwordPattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$");

        if (email.isEmpty()) {
            Toast.makeText(getBaseContext(), "e-mail is required input field", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.isEmpty()) {
            Toast.makeText(getBaseContext(), "username is required input field", Toast.LENGTH_SHORT).show();
            return;
        }

        if(password.isEmpty()) {
            Toast.makeText(getBaseContext(), "password is required input field", Toast.LENGTH_SHORT).show();
            return;
        }

        if(! (emailPattern.matcher(email)).matches()) {
            Toast.makeText(getBaseContext(), "Vnesite validni e-mail naslov", Toast.LENGTH_SHORT).show();
            return;
        }

        if(! (passwordPattern.matcher(password)).matches()) {
            Toast.makeText(getBaseContext(), "Vnesite geslo z 8 do 20 znakov, vsaj 1 števko, vsaj 1 veliko črko, vsaj 1 malo črko in vsaj 1 specijalni znac (brez razmakov)", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(email, username, password);
        user.setId(UUID.randomUUID().toString());

        if(doesUserAlreadyExistInDb(user)) {
            Toast.makeText(getBaseContext(), "Uporabnik z uporabniškim imenom ali e-naslovom že obstaja v bazi podatkov", Toast.LENGTH_SHORT).show();
            return;
        }

        Task ref = db.child(User.COLLECTION).child(user.getId()).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                returnToLoginActivity(1);
                Log.i(TAG,"new user successfully added to database");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                returnToLoginActivity(0);
                Log.i(TAG,"user not registered (FAILED!!!)");
            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
