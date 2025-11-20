package com.example.b07project.auth;

import com.example.b07project.DeviceChooserActivity;

public class LoginPresenter implements LoginContract.Presenter {
    private LoginContract.View view;
    private final AuthRepo repo;

    public LoginPresenter(LoginContract.View view, AuthRepo repo) {
        this.view = view;
        this.repo = repo;
    }

    @Override
    public void onLoginClicked() {
        if (view == null) return;
        String email = view.getEmailInput();
        String pass  = view.getPasswordInput();

        if (email == null || email.isEmpty() || pass == null || pass.length() < 6) {
            view.showError("Enter a valid email and a 6+ character password.");
            return;
        }

        view.showLoading(true);
        repo.signIn(email, pass, new AuthRepo.Callback() {
            @Override public void onSuccess() {
                // Fetch role
                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                repo.getUserRole(uid, new AuthRepo.RoleCallback() {
                    @Override
                    public void onRole(String role) {
                        if (view != null) {
                            view.showLoading(false);
                            if ("parent".equals(role)) {
                                view.navigateToDeviceChooser();
                            } else if ("provider".equals(role)) {
                                view.navigateToProviderHome();
                            } else {
                                view.navigateToHome();
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (view != null) {
                            view.showLoading(false);
                            view.showError("Failed to get user role: " + e.getMessage());
                        }
                    }
                });
            }
            @Override public void onError(Exception e) {
                if (view != null) {
                    view.showLoading(false);
                    view.showError(e.getMessage() != null ? e.getMessage() : "Login failed");
                }
            }
        });
    }

    @Override public void onDestroy() { view = null; }
}
