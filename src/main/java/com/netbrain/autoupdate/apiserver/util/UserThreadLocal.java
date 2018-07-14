package com.netbrain.autoupdate.apiserver.util;

import com.netbrain.autoupdate.apiserver.model.UserEntity;

public class UserThreadLocal {

    private UserThreadLocal() {

    }

    private static final ThreadLocal<UserEntity> LOCAL = new ThreadLocal<UserEntity>();

    public static void set(UserEntity user) {
        LOCAL.set(user);
    }

    public static UserEntity get() {
        return LOCAL.get();
    }
}