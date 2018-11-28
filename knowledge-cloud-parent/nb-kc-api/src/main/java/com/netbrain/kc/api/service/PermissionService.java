package com.netbrain.kc.api.service;

import java.util.List;

import com.netbrain.kc.api.model.datamodel.PermissionEntity;

public interface PermissionService {
    List<PermissionEntity> findAllById(List<String> permissionIds);
}
