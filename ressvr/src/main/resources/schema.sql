-- User

DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role_menu;

DROP TABLE IF EXISTS working_team_user;
DROP TABLE IF EXISTS working_team;

DROP TABLE IF EXISTS user_info;
DROP TABLE IF EXISTS role;

DROP TABLE IF EXISTS menu;
DROP TABLE IF EXISTS menu_sub_group;
DROP TABLE IF EXISTS menu_group;

CREATE TABLE user_info (
  user_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(100) NOT NULL  UNIQUE,
  first_name  VARCHAR(100) NOT NULL,
  last_name  VARCHAR(100) NOT NULL,
  is_admin boolean not null,
  change_password_at_next_logon  boolean not null,
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50)
);




INSERT INTO user_info (username,  first_name, last_name, is_admin, change_password_at_next_logon)
VALUES ("GZHANG",  "Gary", "Zhang", true, false );

-- INSERT INTO user_info (username,  first_name, last_name, is_admin) VALUES ("RWU",  "Rainbow", "Wu", false);

-- INSERT INTO user_info (username,  first_name, last_name, is_admin) VALUES ("OZHANG", "Olivia", "Zhang", false);

-- INSERT INTO user_info (username,  first_name, last_name, is_admin) VALUES ("JZHANG", "Japser", "Zhang", false);



CREATE TABLE role (
  role_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL  UNIQUE,
  description  VARCHAR(100) NOT NULL,
  enabled boolean not null default 0,
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50)
);


-- insert into role (name, description, enabled) values ("Admin","Admin",true);
-- insert into role (name, description, enabled) values ("WarehouseManager","Warehouse Manager",true);
-- insert into role (name, description, enabled) values ("InboundManager","Inbound Manager",true);
-- insert into role (name, description, enabled) values ("OutboundManager","Outbound Manager",true);
-- insert into role (name, description, enabled) values ("InventoryManager","Inventory Manager",true);

CREATE TABLE user_role (
  role_id    BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  foreign key(role_id) references role(role_id),
  foreign key(user_id) references user_info(user_id),
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50)
);



-- Menu / Menu Sub Group / Menu Group
CREATE TABLE menu_group (
  menu_group_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL  UNIQUE,
  text   VARCHAR(100) NOT NULL,
  i18n  VARCHAR(100) NOT NULL,
  group_flag boolean not null default 1,
  hide_in_breadcrumb boolean not null default 0,
  sequence int not null default 0,
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50)
);

CREATE TABLE menu_sub_group (
  menu_sub_group_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  text   VARCHAR(100) NOT NULL ,
  name   VARCHAR(100) NOT NULL  UNIQUE,
  i18n  VARCHAR(100) NOT NULL,
  icon  VARCHAR(100) NOT NULL,
  shortcut_root  boolean,
  menu_group_id BIGINT not null,
  sequence int not null default 0,
  link VARCHAR(100),
  badge int,
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50),
  foreign key(menu_group_id) references menu_group(menu_group_id)
);

CREATE TABLE menu (
  menu_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  text   VARCHAR(100) NOT NULL ,
  name   VARCHAR(100) NOT NULL  UNIQUE,
  i18n  VARCHAR(100) NOT NULL,
  link VARCHAR(100) NOT NULL,
  menu_sub_group_id BIGINT not null,
  sequence int not null default 0,
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50),
  foreign key(menu_sub_group_id) references menu_sub_group(menu_sub_group_id)
);


CREATE TABLE role_menu (
  role_id    BIGINT NOT NULL,
  menu_id    BIGINT NOT NULL,
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50),
  foreign key(role_id) references role(role_id),
  foreign key(menu_id) references menu(menu_id)
);

CREATE TABLE working_team (
  working_team_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL  UNIQUE,
  description  VARCHAR(100) NOT NULL,
  enabled boolean not null default 0,
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50)
);

CREATE TABLE working_team_user (
  working_team_id    BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  created_time date,
  created_by VARCHAR(50),
  last_modified_time date,
  last_modified_by VARCHAR(50),
  foreign key(working_team_id) references working_team(working_team_id),
  foreign key(user_id) references user_info(user_id)
);
