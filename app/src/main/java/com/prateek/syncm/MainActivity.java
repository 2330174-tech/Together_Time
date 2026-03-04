package com.prateek.syncm;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // If user is already "logged in", we still want them to pick a name for this session
        // in our simplified flow.
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}