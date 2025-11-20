package com.example.b07project.auth;

public interface AuthRepo {
    interface Callback { void onSuccess(); void onError(Exception e); }
    interface RoleCallback { void onRole(String role); void onError(Exception e); }
    void signIn(String email, String password, Callback cb);
    void getUserRole(String uid, RoleCallback cb);
}
