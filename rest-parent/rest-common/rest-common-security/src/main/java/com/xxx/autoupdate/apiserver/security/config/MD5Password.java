package com.xxx.autoupdate.apiserver.security.config;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.xxx.autoupdate.apiserver.util.MD5Util;

public class MD5Password implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return MD5Util.generate((String) rawPassword);
    }
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return MD5Util.verify((String) rawPassword, encodedPassword);
    }
}
