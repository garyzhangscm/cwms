-- User

DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role_menu;

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
  is_admin boolean not null
);




INSERT INTO user_info (username,  first_name, last_name, is_admin) VALUES ("GZHANG",  "Gary", "Zhang", true);

-- INSERT INTO user_info (username,  first_name, last_name, is_admin) VALUES ("RWU",  "Rainbow", "Wu", false);

-- INSERT INTO user_info (username,  first_name, last_name, is_admin) VALUES ("OZHANG", "Olivia", "Zhang", false);

-- INSERT INTO user_info (username,  first_name, last_name, is_admin) VALUES ("JZHANG", "Japser", "Zhang", false);



CREATE TABLE role (
  role_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL  UNIQUE,
  description  VARCHAR(100) NOT NULL,
  enabled boolean not null default 0
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
  foreign key(user_id) references user_info(user_id)
);



-- Menu / Menu Sub Group / Menu Group
CREATE TABLE menu_group (
  menu_group_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL  UNIQUE,
  text   VARCHAR(100) NOT NULL,
  i18n  VARCHAR(100) NOT NULL,
  group_flag boolean not null default 1,
  hide_in_breadcrumb boolean not null default 0,
  sequence int not null default 0
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
  foreign key(menu_sub_group_id) references menu_sub_group(menu_sub_group_id)
);


CREATE TABLE role_menu (
  role_id    BIGINT NOT NULL,
  menu_id    BIGINT NOT NULL,
  foreign key(role_id) references role(role_id),
  foreign key(menu_id) references menu(menu_id)
);


