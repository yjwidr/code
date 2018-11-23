package com.netbrain.kc.api.model.datamodel;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Entity
@Table(name = "t_role_permissions")
public class RolePermissionEntity implements Serializable {

    /*private static final long serialVersionUID = 1L;*/
    @Id
    @GeneratedValue(generator = "idGenerator")
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    private String id;
    @NotBlank
    private String roleId;
    @NotBlank
    private String permissionId;
}
