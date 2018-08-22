package com.xxx.autoupdate.apiserver.util;

import com.xxx.autoupdate.apiserver.model.UserEntity;

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