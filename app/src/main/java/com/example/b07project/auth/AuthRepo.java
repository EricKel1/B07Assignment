package com.example.b07project.auth;

public interface AuthRepo {
    interface Callback { void onSuccess(); void onError(Exception e); }
    void signIn(String email, String password, Callback cb);
}
