package com.android.example.foersteaarsprojekt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText userEditText;
    private EditText passwordEditText;
    private TextView userDisplay;
    private Spinner spinner;
    private Spinner rolleSpinner;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userEditText = findViewById(R.id.userEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        userDisplay = findViewById(R.id.userDisplay);
        spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.spinner, R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);

        rolleSpinner = findViewById(R.id.spinner2);
        ArrayAdapter<CharSequence> arrayAdapter2 = ArrayAdapter.createFromResource(this, R.array.spinner2, R.layout.support_simple_spinner_dropdown_item);
        rolleSpinner.setAdapter(arrayAdapter2);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public void buttonPress(View view) {
        String email = userEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (spinner.getSelectedItemPosition() == 0) {
            createAccount(email, password);
        } else {
            signIn(email, password);
        }
    }

    public void createAccount(String email, final String password) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Login", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            userDisplay.setText(user.getEmail());
                            Map<String, Object> newUser = new HashMap<>();
                            newUser.put("password", password);
                            if (rolleSpinner.getSelectedItemPosition() == 0) {
                                db.collection("behandlere").document(user.getEmail())
                                        .set(newUser)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d("create", "Data added to behandlere");
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("create", "Data not added to behandlere");
                                    }
                                });
                                moveToMessageActivity("behandlere");
                            } else {
                                db.collection("klienter").document(user.getEmail())
                                        .set(newUser)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d("create", "Data added to klienter");
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("create", "Data not added to klienter");
                                    }
                                });
                                moveToMessageActivity("klienter");
                            }


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Login", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(getApplicationContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("SignIn", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            userDisplay.setText(user.getDisplayName());
                            db.collection("behandlere").document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot documentSnapshot = task.getResult();
                                        if (documentSnapshot.exists()) {
                                            moveToMessageActivity("behandlere");
                                        }
                                        else {
                                            moveToMessageActivity("klienter");
                                        }
                                    }
                                }
                            });

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Sign", "signInWithEmail:failure", task.getException());
                            Toast.makeText(getApplicationContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void moveToMessageActivity(String type) {
        Intent i = new Intent(this, MessageActivity.class);
        i.putExtra("type", type);
        startActivity(i);
    }

    public void signInBehandler(View view) {
        signIn("tghpublic@hotmail.com", "test123");
    }

    public void signInKlient(View view) {
        signIn("test@hotmail.com", "test123");
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userDisplay.setText(currentUser.getDisplayName());
        }
    }
}
