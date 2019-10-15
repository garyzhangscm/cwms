
DROP TABLE IF EXISTS menu;

DROP TABLE IF EXISTS menu_group;

CREATE TABLE menu_group (
  menu_group_id      INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL UNIQUE,
  description   VARCHAR(100) NOT NULL,
  sequence INT NOT NULL
  );



CREATE TABLE menu (
  menu_id      INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  menu_group_id      INT NOT NULL ,
  name   VARCHAR(100) NOT NULL  UNIQUE,
  description   VARCHAR(100) NOT NULL,
  sequence INT NOT NULL,
  FOREIGN KEY (menu_group_id) REFERENCES menu_group(menu_group_id)
  );


INSERT INTO menu_group (name, description, sequence) VALUES ("AUTH", "Authorization Maintenance", 0);
INSERT INTO menu (menu_group_id, name, description, sequence) VALUES (
    (select menu_group_id from menu_group where name = "AUTH"),
    "ROLE", "Role Maintenance", 0);

INSERT INTO menu (menu_group_id, name, description, sequence) VALUES (
    (select menu_group_id from menu_group where name = "AUTH"),
     "USER", "User Maintenance", 1);


INSERT INTO menu_group (name, description, sequence) VALUES ("WH_LAYOUT", "Warehouse Layout", 1);
INSERT INTO menu (menu_group_id, name, description, sequence) VALUES (
    (select menu_group_id from menu_group where name = "WH_LAYOUT"),
     "WAREHOUSE", "Warehouse Maintenance", 0);
INSERT INTO menu (menu_group_id, name, description, sequence) VALUES (
    (select menu_group_id from menu_group where name = "WH_LAYOUT"),
     "LOCATION_GROUP", "Location Group Maintenance", 1);
INSERT INTO menu (menu_group_id, name, description, sequence) VALUES (
    (select menu_group_id from menu_group where name = "WH_LAYOUT"),
    "LOCATION", "Location Maintenance", 2);

INSERT INTO menu_group (name, description, sequence) VALUES ("INVENTORY", "Inventory Management", 2);
INSERT INTO menu (menu_group_id, name, description, sequence) VALUES (
    (select menu_group_id from menu_group where name = "INVENTORY"),
     "INV_DIS", "Inventory Display", 0);
INSERT INTO menu (menu_group_id, name, description, sequence) VALUES (
    (select menu_group_id from menu_group where name = "INVENTORY"),
     "INV_ADJ", "Inventory Adjustment", 1);

