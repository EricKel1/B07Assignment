package com.example.b07project.auth;

public interface LoginContract {
    interface View {
        void showLoading(boolean show);
        void showError(String message);
        void navigateToHome();
        void navigateToProviderHome();
        void navigateToDeviceChooser();
        String getEmailInput();
        String getPasswordInput();
    }
    interface Presenter {
        void onLoginClicked();
        void onDestroy();
    }
}
