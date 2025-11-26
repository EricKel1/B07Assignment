package com.example.b07project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.util.Patterns;

import com.example.b07project.auth.AuthRepo;
import com.example.b07project.auth.LoginContract;
import com.example.b07project.auth.LoginPresenter;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Pattern;

@RunWith(MockitoJUnitRunner.class)
public class LoginPresenterTest {
    //Mocks to use
    @Mock
    private LoginContract.View mockView;

    @Mock
    private AuthRepo mockRepo;

    @Mock
    private FirebaseAuth mockFirebaseAuth;

    @Mock
    private FirebaseUser mockFirebaseUser;

    @Mock
    private java.util.regex.Matcher mockMatcher;

    private MockedStatic<FirebaseAuth> mockedFirebaseAuth;

    private LoginPresenter presenter;

    //Need tests for the following cases
        /*
        WHen email field is empty; should show the error and stop
        when email is null, should show error and stop.
        when password is less than the proper amount (6 chars) should error and stop
        when password is null, sohuld error and stop

        when email and password are right, loading appears and log in should occur
        when username and password are right, loading appears and log in occurs

        When good log in occurs, should get user role from database (parent, child, provider)
        WHen log in with @test.com, should skip email verification (will remove on launch)
        when child account is opening should skip email verification and check role
        when trying to log in with unverified email, show email verification dialog
        When firebaseauth returns null user (might happen when no wifi?) should have behaviour
                handling to not crash

        When the login fails for whatever reason, should hide the loading circle and display the error
        When the login fails with no exception message, show login failed, something went wrong, etc.

        Test that parents are sent to the device chooser activity
        When users with Provider type log in,  sent to provider home activity.
        test that users with child role (from the device chooser) are sent to their proper home
        when errors during role fetching occur show correct error to user

        Test that ondestroy gets rid of view reference (stop memory leaks)
        Test that if the sign in succeeds AFTER activity is destroyed (async...) no crash
        Test that if sign in fails AFTER activity is destroyed, no crash
        Test if role fetch succeeds after ondestroy, no crash
        test if role fetch fails after ondestroy, no crash.



         */
    @Before
    public void setUpTests(){
        mockedFirebaseAuth = mockStatic(FirebaseAuth.class);
        mockedFirebaseAuth.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);

        presenter = new LoginPresenter(mockView, mockRepo);
    }

    @After
    public void tearDownTests(){
        mockedFirebaseAuth.close();
    }

    //Test
    @Test
    public void onLoginWithEmptyEmailShowsError(){
        //when the email input is ""
        when(mockView.getEmailInput()).thenReturn("");
        when(mockView.getPasswordInput()).thenReturn("password123");

        presenter.onLoginClicked();
        verify(mockView).showError("Enter a valid email/username and a 6+ character password.");
    }

    //Test
    @Test
    public void onLoginWithNullEmailShowsError(){
        when(mockView.getEmailInput()).thenReturn(null);
        when(mockView.getPasswordInput()).thenReturn("123456789");//valid password

        presenter.onLoginClicked();
        verify(mockView).showError("Enter a valid email/username and a 6+ character password.");
    }

    //Test
    @Test
    public void onLoginWithNullPasswordShowsError(){
        when(mockView.getEmailInput()).thenReturn("123@example.com");//valid email
        when(mockView.getPasswordInput()).thenReturn(null);

        presenter.onLoginClicked();
        verify(mockView).showError("Enter a valid email/username and a 6+ character password.");
    }

    //Test
    @Test
    public void onLoginWithShortPasswordShowsError(){
        when(mockView.getEmailInput()).thenReturn("123@example.com");//valid email
        when(mockView.getPasswordInput()).thenReturn("12345");

        presenter.onLoginClicked();
        verify(mockView).showError("Enter a valid email/username and a 6+ character password.");
    }

    //Test
    @Test
    public void onLoginWithValidEmailAndPasswordSignsIn(){
        when(mockView.getEmailInput()).thenReturn("123@example.com");//valid email
        when(mockView.getPasswordInput()).thenReturn("123456789");
        try(MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)){
            mockedPatterns.when(()-> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);
            presenter.onLoginClicked();
            verify(mockRepo).signIn(eq("123@example.com"), eq("123456789"), any(AuthRepo.Callback.class));

        }
    }

    //Test
    @Test
    public void onLoginWithUserNameConvertsToEmail(){
        when(mockView.getEmailInput()).thenReturn("123user");//valid email
        when(mockView.getPasswordInput()).thenReturn("123456789");
        try(MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)){
            mockedPatterns.when(()-> Patterns.EMAIL_ADDRESS.matcher("123user"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(false);
            presenter.onLoginClicked();
            verify(mockRepo).signIn(eq("123user@b07project.local"), eq("123456789"), any(AuthRepo.Callback.class));

        }
    }

    //Test
    @Test
    public void onLoginWithSignInSuccessWIthVerifiedEmailFetchRole(){
        when(mockView.getEmailInput()).thenReturn("123@example.com");//valid email
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(true);
        when(mockFirebaseUser.getEmail()).thenReturn("123@example.com");
        when(mockFirebaseUser.getUid()).thenReturn("user123");

        try(MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)){
            mockedPatterns.when(()-> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();

            verify(mockRepo).getUserRole(eq("user123"), any(AuthRepo.RoleCallback.class));
        }
    }

    //Test
    @Test
    public void onLoginWithSignInSuccessWIthVerificationBypass(){
        when(mockView.getEmailInput()).thenReturn("123@test.com");//valid email
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(false);
        when(mockFirebaseUser.getEmail()).thenReturn("123@test.com");
        when(mockFirebaseUser.getUid()).thenReturn("user123");

        try(MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)){
            mockedPatterns.when(()-> Patterns.EMAIL_ADDRESS.matcher("123@test.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();

            verify(mockRepo).getUserRole(eq("user123"), any(AuthRepo.RoleCallback.class));
        }
    }

    //Test
    @Test
    public void onLoginWithSignInSuccessWIthChildUserWithVerificationBypass(){
        when(mockView.getEmailInput()).thenReturn("child@b07project.local");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(false);
        when(mockFirebaseUser.getEmail()).thenReturn("child@b07project.local");
        when(mockFirebaseUser.getUid()).thenReturn("user123");

        try(MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)){
            mockedPatterns.when(()-> Patterns.EMAIL_ADDRESS.matcher("child@b07project.local"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();

            verify(mockRepo).getUserRole(eq("user123"), any(AuthRepo.RoleCallback.class));
        }
    }

    //Test
    @Test
    public void onLoginWithSignInSuccessWIthUnverifiedEmail_showsDialog(){
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(false);
        when(mockFirebaseUser.getEmail()).thenReturn("123@example.com");

        try(MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)){
            mockedPatterns.when(()-> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();

            verify(this.mockView).showEmailNotVerifiedDialog();
        }
    }

    //Test
    @Test
    public void onLoginWithSignInSuccessWIthNullUserDoesNotCrash() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(null);
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();

            verify(mockRepo, never()).getUserRole(anyString(), any(AuthRepo.RoleCallback.class));
        }

    }

    //Test
    @Test
    public void onLoginWithSignInErrorShowsError() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            Exception exception = new Exception("Authentication failed");
            callbackArgumentCaptor.getValue().onError(exception);

            verify(mockView).showError("Authentication failed");

        }

    }

    //Test
    @Test
    public void onLoginWithSignInErrorWithNullMessageShowDefaultError() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            Exception exception = new Exception();
            callbackArgumentCaptor.getValue().onError(exception);

            verify(mockView).showError("Login failed");

        }

    }

    //Test
    @Test
    public void fetchUserRoleWIthParentRoleNavigatesToDeviceChooser() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(true);
        when(mockFirebaseUser.getEmail()).thenReturn("123@example.com");
        when(mockFirebaseUser.getUid()).thenReturn("user123");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();
            ArgumentCaptor<AuthRepo.RoleCallback> roleCallbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.RoleCallback.class);
            verify(mockRepo).getUserRole(anyString(), roleCallbackArgumentCaptor.capture());
            roleCallbackArgumentCaptor.getValue().onRole("parent");

            verify(mockView).navigateToDeviceChooser();

        }

    }

    //Test
    @Test
    public void fetchUserRoleWIthProviderRoleNavigatesToProviderHome() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(true);
        when(mockFirebaseUser.getEmail()).thenReturn("123@example.com");
        when(mockFirebaseUser.getUid()).thenReturn("user123");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();
            ArgumentCaptor<AuthRepo.RoleCallback> roleCallbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.RoleCallback.class);
            verify(mockRepo).getUserRole(anyString(), roleCallbackArgumentCaptor.capture());
            roleCallbackArgumentCaptor.getValue().onRole("provider");

            verify(mockView).navigateToProviderHome();

        }

    }

    //Test
    @Test
    public void fetchUserRoleOtherRoleNavigatesToHome() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(true);
        when(mockFirebaseUser.getEmail()).thenReturn("123@example.com");
        when(mockFirebaseUser.getUid()).thenReturn("user123");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();
            ArgumentCaptor<AuthRepo.RoleCallback> roleCallbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.RoleCallback.class);
            verify(mockRepo).getUserRole(anyString(), roleCallbackArgumentCaptor.capture());
            roleCallbackArgumentCaptor.getValue().onRole("child");

            verify(mockView).navigateToHome();
        }

    }

    //Test
    @Test
    public void fetchUserRoleErrorShowsError() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(true);
        when(mockFirebaseUser.getEmail()).thenReturn("123@example.com");
        when(mockFirebaseUser.getUid()).thenReturn("user123");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();
            ArgumentCaptor<AuthRepo.RoleCallback> roleCallbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.RoleCallback.class);
            verify(mockRepo).getUserRole(anyString(), roleCallbackArgumentCaptor.capture());
            Exception exception = new Exception("Database error");
            roleCallbackArgumentCaptor.getValue().onError(exception);

            verify(mockView).showError("Failed to get user role: Database error");
        }

    }

    @Test
    public void onDestroySetViewToNull(){
        presenter.onDestroy();
        presenter.onLoginClicked();

        verify(mockView, never()).getEmailInput();
    }

    @Test
    public void signInCallBackSuccessDoesntCrashAfterDestroy(){
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            presenter.onDestroy();

            callbackArgumentCaptor.getValue().onSuccess();
        }

    }

    @Test
    public void signInCallBackSuccessDoesntCrashOnError(){
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            presenter.onDestroy();

            callbackArgumentCaptor.getValue().onError(new Exception("Error"));
        }

    }

    @Test
    public void roleCallbackDoesNotCrashOnRoleAfterDestroy() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(true);
        when(mockFirebaseUser.getEmail()).thenReturn("123@example.com");
        when(mockFirebaseUser.getUid()).thenReturn("user123");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();
            ArgumentCaptor<AuthRepo.RoleCallback> roleCallbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.RoleCallback.class);
            verify(mockRepo).getUserRole(anyString(), roleCallbackArgumentCaptor.capture());
            presenter.onDestroy();

            roleCallbackArgumentCaptor.getValue().onRole("parent");
        }

    }

    @Test
    public void roleCallbackDoesNotCrashOnErrorAfterDestroy() {
        when(mockView.getEmailInput()).thenReturn("123@example.com");
        when(mockView.getPasswordInput()).thenReturn("123456789");
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.isEmailVerified()).thenReturn(true);
        when(mockFirebaseUser.getEmail()).thenReturn("123@example.com");
        when(mockFirebaseUser.getUid()).thenReturn("user123");
        try (MockedStatic<Patterns> mockedPatterns = mockStatic(Patterns.class)) {
            mockedPatterns.when(() -> Patterns.EMAIL_ADDRESS.matcher("123@example.com"))
                    .thenReturn(mockMatcher);
            when(mockMatcher.matches()).thenReturn(true);

            presenter.onLoginClicked();
            ArgumentCaptor<AuthRepo.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.Callback.class);
            verify(mockRepo).signIn(anyString(), anyString(), callbackArgumentCaptor.capture());
            callbackArgumentCaptor.getValue().onSuccess();
            ArgumentCaptor<AuthRepo.RoleCallback> roleCallbackArgumentCaptor = ArgumentCaptor.forClass(AuthRepo.RoleCallback.class);
            verify(mockRepo).getUserRole(anyString(), roleCallbackArgumentCaptor.capture());
            presenter.onDestroy();

            roleCallbackArgumentCaptor.getValue().onError(new Exception("Error"));
        }
    }

    @Test
    public void onLoginWithNullViewDoesNothing(){
        presenter.onDestroy();
        presenter.onLoginClicked();
        verify(mockView, never()).getEmailInput();
    }

}