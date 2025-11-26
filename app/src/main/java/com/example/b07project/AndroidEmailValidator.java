package com.example.b07project;

public class AndroidEmailValidator implements EmailValidator {
    @Override
    public boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}

