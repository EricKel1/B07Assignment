package com.example.b07project.auth;

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
                if (view != null) {
                    view.showLoading(false);
                    view.navigateToHome();
                }
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
