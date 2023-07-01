package com.example.shopngo;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shopngo.facerecognition.FaceRecognitionActivity;
import com.example.shopngo.facerecognition.RecognitionObject;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActiviy extends AppCompatActivity {

    TextView name, email, password, phone, confirmpassword, login, uploadPhotoBtn;
    Button signup;

    FirebaseAuth outh;

    ActivityResultLauncher<Intent> FaceRecognitionActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    Log.e("TAGTAG", "onActivityResult: " + RecognitionObject.recognition.toString());

                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        outh = FirebaseAuth.getInstance();

        name = findViewById(R.id.signupnametxt);
        email = findViewById(R.id.signupemailtxt);
        password = findViewById(R.id.signuppasswordtxt);
        confirmpassword = findViewById(R.id.signupconfirmpasswordtxt);
        phone = findViewById(R.id.signupphonetxt);
        login = findViewById(R.id.signuplogintxt);
        signup = findViewById(R.id.signupsignupbtn);
        uploadPhotoBtn = findViewById(R.id.uploadPhotoBtn);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nameString = name.getText().toString();
                String emailString = email.getText().toString();
                String passwordString = password.getText().toString();
                String confirmpasswordString = confirmpassword.getText().toString();
                String phoneString = phone.getText().toString();
                //
                if (!passwordString.equals(confirmpasswordString)) {
                    confirmpassword.setError("Password Does not Match");
                    return;
                }

                register(emailString, passwordString);
                Register_user_data(nameString, emailString, phoneString);
            }
        });

        uploadPhotoBtn.setOnClickListener((view) -> {

            FaceRecognitionActivityResultLauncher.launch(new Intent(SignupActiviy.this, FaceRecognitionActivity.class));
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignupActiviy.this, LoginActivity.class));
            }
        });

    }

    private void register(String username, String password) {
        outh.createUserWithEmailAndPassword(username, password).addOnCompleteListener(SignupActiviy.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(SignupActiviy.this, "Register User successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActiviy.this, QRCodeActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignupActiviy.this, "Registration Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void Register_user_data(String name, String email, String phone) {
        String username = outh.getCurrentUser().getUid();


        FirebaseDatabase.getInstance().getReference().child(username).child("name").setValue(name);
        FirebaseDatabase.getInstance().getReference().child(username).child("email").setValue(email);
        FirebaseDatabase.getInstance().getReference().child(username).child("phone").setValue(phone);

        FirebaseDatabase.getInstance().getReference().child(username).child("face_recognition").setValue(RecognitionObject.INSTANCE.recognitiontostring());
    }
}