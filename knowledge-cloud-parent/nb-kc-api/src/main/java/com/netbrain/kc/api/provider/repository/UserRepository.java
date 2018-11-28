package com.netbrain.kc.api.provider.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.netbrain.kc.api.model.datamodel.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity>, Serializable {
    @Query(value = "from UserEntity u where u.userName=?1")
    UserEntity findByUsername(String username);
}