package com.netbrain.autoupdate.apiserver.services;

import java.util.List;

import com.netbrain.autoupdate.apiserver.model.PermissionEntity;

public interface PermissionService {
    List<PermissionEntity> findAllById(List<String> permissionIds);
}
