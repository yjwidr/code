package com.netbrain.kc.api.service;

import java.util.List;

public interface RolePermissionService {
    List<String> findByRoleId(String roleId);
}
