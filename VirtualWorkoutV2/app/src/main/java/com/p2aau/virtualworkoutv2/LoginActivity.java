package com.p2aau.virtualworkoutv2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.p2aau.virtualworkoutv2.classes.ExerciseConstant;
import com.p2aau.virtualworkoutv2.openvcall.ui.BaseActivity;

public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

    }

    // In the superclass it asks for the permission, so it gets done at the first page.
    @Override
    protected void initUIandEvent() {

    }

    @Override
    protected void deInitUIandEvent() {

    }

    // Login button
    public void onLoginNextClick(View view){
        // Finds the views and gets the username
        EditText userNameEditText = findViewById(R.id.login_user_name_field);
        String userName = userNameEditText.getText().toString();

        /*
         * Checks first if the user remembered to enter a username, and only username.
         * For the first real implementation, password and the like are minor details and will be left out.
        */
        if(userName.equals(null) || userName.equals("")) {
            // If the username is empty, it will tell the user.
            Toast.makeText(this, "user name cannot be empty", Toast.LENGTH_SHORT).show();
        } else {
            // If it is not empty, it will continue with that username to the next page/activity.
            Intent intent = new Intent(this, MainMenuActivity.class);
            ExerciseConstant.USERNAME = userName; //Adds the username to a constant for the program to work with.
            intent.putExtra("Uniqid", "login");
            startActivity(intent);
        }
    }

    // Sign up button
    public void onSignUpClick(View view){
        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivity(intent);
    }


}