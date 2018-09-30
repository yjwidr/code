--t_users
insert into t_users(id, user_name,login_name,password,role_id,create_user_id,update_user_id) values(10000,'admin','admin','987f99b9b87480694924a97dc3769150747419058c51460c',10000,10000,10000);
--t_roles
insert into t_roles(id, name,create_user_id,update_user_id) values(10000,'Admin',10000,10000);
insert into t_roles(id, name,create_user_id,update_user_id) values(20000,'License User',10000,10000);
insert into t_roles(id, name,create_user_id,update_user_id) values(30000,'Common User',10000,10000);
--t_permissions
insert into t_permissions(id, name) values(10000,'Download');
insert into t_permissions(id, name) values(20000,'Upload');
insert into t_permissions(id, name) values(30000,'Publish');
insert into t_permissions(id, name) values(40000,'Push Upgrade');
insert into t_permissions(id, name) values(50000,'Edit');
insert into t_permissions(id, name) values(60000,'Disable');
insert into t_permissions(id, name) values(70000,'Delete');
--t_role_permission
insert into t_role_permissions(id, role_id,permission_id) values(10000,10000,10000);
insert into t_role_permissions(id, role_id,permission_id) values(20000,10000,20000);
insert into t_role_permissions(id, role_id,permission_id) values(30000,10000,30000);
insert into t_role_permissions(id, role_id,permission_id) values(40000,10000,40000);
insert into t_role_permissions(id, role_id,permission_id) values(50000,10000,50000);
insert into t_role_permissions(id, role_id,permission_id) values(60000,10000,60000);
insert into t_role_permissions(id, role_id,permission_id) values(70000,10000,70000);
insert into t_role_permissions(id, role_id,permission_id) values(80000,20000,10000);
insert into t_role_permissions(id, role_id,permission_id) values(90000,30000,10000);

--t_software_versions
insert into t_software_versions(version, create_user_id, name, update_user_id) VALUES ('7.1a', 10000, '7.1a', 10000);
insert into t_software_versions(version, create_user_id, name, update_user_id) VALUES ('7.1b', 10000, '7.1b', 10000);

--t_white_list
INSERT INTO t_white_list(id, content_version_id, license_id) VALUES ('1','1','1');
INSERT INTO t_white_list(id, content_version_id, license_id) VALUES ('2','1','2');
INSERT INTO t_white_list(id, content_version_id, license_id) VALUES ('3','1','4');