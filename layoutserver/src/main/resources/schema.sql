DROP TABLE IF EXISTS warehouse;
drop table if exists location;
drop table if exists location_group;
drop Table if exists location_group_type;

CREATE TABLE warehouse (
  warehouse_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL unique,
  size   VARCHAR(100) NOT NULL,
  address_country   VARCHAR(100) NOT NULL,
  address_state   VARCHAR(100) NOT NULL,
  address_county   VARCHAR(100) NOT NULL,
  address_city   VARCHAR(100) NOT NULL,
  address_district   VARCHAR(100),
  address_line1   VARCHAR(300) NOT NULL,
  address_line2   VARCHAR(300),
  address_postcode  VARCHAR(10) NOT NULL);


INSERT INTO warehouse (name, size, address_country, address_state, address_county , address_city , address_district, address_line1, address_line2, address_postcode)
     VALUES ("WMOR", "50000 sqft", "U.S", "CA", "Orange", "Orange", "", "2164 N Batavia ST", "", "92865");
-- INSERT INTO warehouse (name, size, address_country, address_state, address_county , address_city , address_district, address_line1, address_line2, address_postcode)
--     VALUES ("SH", "75000 sqft", "China", "Shanghai", "Shanghai", "Shanghai", "Pudong", "68 Yuheng Road", "", "201203");
-- INSERT INTO warehouse (name, size, address_country, address_state, address_county , address_city , address_district, address_line1, address_line2, address_postcode)
--     VALUES ("WMNE", "100000 sqft", "U.S", "NY", "New York", "New York", "", "#1 5th Ave", "", "100001");


CREATE TABLE location_group_type (
  location_group_type_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  four_wall_inventory  boolean not null default 1,
  virtual_locations  boolean not null default 0);

-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("Storage", "Storage Locations", 1, 0);
-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("Receive_Stage", "Receiving Stage", 1, 0);
-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("Receive_Dock", "Receiving Dock", 1, 0);
-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("Shipping_Stage", "Shipping Stage", 1, 0);
-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("Shipping_Dock", "Shipping Dock", 1, 0);
-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("Dispatched", "Dispatched Shipment", 0, 0);
-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("RF", "RF device", 1, 0);
-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("Yard", "Yard", 1, 0);
-- INSERT INTO location_group_type(name, description, four_wall_inventory, virtual_locations) VALUES("PickupDeposit", "Pickup and Deposit", 1, 0);

CREATE TABLE location_group(
  location_group_id   BIGINT  NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL unique,
  description   VARCHAR(100) NOT NULL,
  location_group_type_id BIGINT NOT NULL,
  pickable  boolean not null default 0,
  storable  boolean not null default 0,
  countable  boolean not null default 0,
  foreign key(location_group_type_id) references location_group_type(location_group_type_id));

-- INSERT INTO location_group(name, description, location_group_type_id, pickable, storable, countable)
--     values ("EACH_PICK_1", "Each Picking Area - #1", 1, 1, 1, 1);
-- INSERT INTO location_group(name, description, location_group_type_id, pickable, storable, countable)
--     values ("EACH_PICK_2", "Each Picking Area - #2", 1, 1, 1, 1);



CREATE TABLE location(
  location_id   BIGINT  NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL unique,
  aisle   VARCHAR(100),
  x double,
  y double,
  z double,
  length double,
  width double,
  height double,
  pick_sequence BIGINT,
  putaway_sequence BIGINT,
  count_sequence BIGINT,
  capacity double,
  fill_percentage double,
  current_volume double,
  pending_volume double,
  location_group_id  BIGINT not null,
  enabled boolean not null default 0,
  reserved_code VARCHAR(100),
  foreign key(location_group_id) references location_group(location_group_id));



