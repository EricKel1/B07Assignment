package com.example.b07project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.b07project.models.ChildDraft;
import com.example.b07project.repository.SignupRepository;

import java.util.ArrayList;
import java.util.List;

public class ParentSignupViewModel extends ViewModel {

    // Data for UI
    public final MutableLiveData<String> parentName = new MutableLiveData<>();
    public final MutableLiveData<String> email = new MutableLiveData<>();
    public final MutableLiveData<String> password = new MutableLiveData<>();
    public final MutableLiveData<Integer> childrenCount = new MutableLiveData<>();
    public final List<ChildDraft> children = new ArrayList<>();

    // State for Signup Process
    public enum SignupState { IDLE, LOADING, SUCCESS, ERROR }

    private final MutableLiveData<SignupState> signupState = new MutableLiveData<>(SignupState.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final SignupRepository signupRepository;

    public ParentSignupViewModel() {
        this.signupRepository = new SignupRepository();
    }

    public LiveData<SignupState> getSignupState() {
        return signupState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void createParentAccount() {
        signupState.setValue(SignupState.LOADING);

        signupRepository.createParentAccount(
                email.getValue(),
                password.getValue(),
                parentName.getValue(),
                children,
                new SignupRepository.OnSignupCompleteListener() {
                    @Override
                    public void onSuccess() {
                        signupState.setValue(SignupState.SUCCESS);
                    }

                    @Override
                    public void onFailure(String message) {
                        errorMessage.setValue(message);
                        signupState.setValue(SignupState.ERROR);
                    }
                }
        );
    }
}
