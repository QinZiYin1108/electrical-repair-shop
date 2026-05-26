package com.example.backend.service;

import com.example.backend.model.user.UserHomePrivateResponse;
import com.example.backend.model.user.UserHomePublicResponse;

public interface UserHomeService {

    UserHomePublicResponse getPublicHomeData();

    UserHomePrivateResponse getCurrentUserPrivateHomeData();
}

