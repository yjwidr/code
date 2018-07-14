package com.netbrain.autoupdate.apiserver.services;

import java.util.List;

public interface RolePermissionService {
    List<String> findByRoleId(String roleId);
}
