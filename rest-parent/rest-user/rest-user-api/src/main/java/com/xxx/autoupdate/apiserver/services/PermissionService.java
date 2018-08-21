package com.xxx.autoupdate.apiserver.services;

import java.util.List;

import com.xxx.autoupdate.apiserver.model.PermissionEntity;

public interface PermissionService {
    List<PermissionEntity> findAllById(List<String> permissionIds);
}
