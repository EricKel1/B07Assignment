package com.example.b07project;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ParentSignupViewModel extends ViewModel {
    public final MutableLiveData<String> parentName = new MutableLiveData<>();
    public final MutableLiveData<String> email = new MutableLiveData<>();
    public final MutableLiveData<String> password = new MutableLiveData<>();
    public final MutableLiveData<Integer> childrenCount = new MutableLiveData<>();
    public final List<ChildDraft> children = new ArrayList<>();
}
