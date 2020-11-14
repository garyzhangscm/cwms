DROP TABLE IF EXISTS client;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS unit_of_measure;
DROP TABLE IF EXISTS reason_code;
drop table if exists system_controlled_number;
drop table if exists policy;

drop table if exists carrier_service_level;
drop table if exists carrier;

drop table if exists work_task;


CREATE TABLE client (
  client_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  name   VARCHAR(100) NOT NULL unique,
  description   VARCHAR(100) NOT NULL,
  contactor_firstname   VARCHAR(100) NOT NULL,
  contactor_lastname   VARCHAR(100) NOT NULL,
  address_country   VARCHAR(100) NOT NULL,
  address_state   VARCHAR(100) NOT NULL,
  address_county   VARCHAR(100) NOT NULL,
  address_city   VARCHAR(100) NOT NULL,
  address_district   VARCHAR(100),
  address_line1   VARCHAR(300) NOT NULL,
  address_line2   VARCHAR(300),
  address_postcode  VARCHAR(10) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


-- INSERT INTO client (name, description, contactor_firstname, contactor_lastname, address_country, address_state, address_county , address_city , address_district, address_line1, address_line2, address_postcode)
--     VALUES ("Papermart", "Papermart Orange HD","Gary","Zhang", "U.S", "CA", "Orange", "Orange", "", "2164 N Batavia ST", "", "92865");
-- INSERT INTO client (name, description, contactor_firstname, contactor_lastname, address_country, address_state, address_county , address_city , address_district, address_line1, address_line2, address_postcode)
--     VALUES ("Walmart", "Walmart U.S","Kent","Lee", "U.S", "CA", "Orange", "Orange", "", "1234 Tustin Ave", "", "92868");
-- INSERT INTO client (name, description, contactor_firstname, contactor_lastname, address_country, address_state, address_county , address_city , address_district, address_line1, address_line2, address_postcode)
--     VALUES ("Target", "Target U.S","Alain","Huang", "U.S", "CA", "Chino", "Chino", "", "135 Grand Ave", "", "91715");


CREATE TABLE unit_of_measure (
  unit_of_measure_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  name   VARCHAR(100) NOT NULL unique,
  description   VARCHAR(100) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE supplier (
  supplier_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  name   VARCHAR(100) NOT NULL unique,
  description   VARCHAR(100) NOT NULL,
  contactor_firstname   VARCHAR(100) NOT NULL,
  contactor_lastname   VARCHAR(100) NOT NULL,
  address_country   VARCHAR(100) NOT NULL,
  address_state   VARCHAR(100) NOT NULL,
  address_county   VARCHAR(100) NOT NULL,
  address_city   VARCHAR(100) NOT NULL,
  address_district   VARCHAR(100),
  address_line1   VARCHAR(300) NOT NULL,
  address_line2   VARCHAR(300),
  address_postcode  VARCHAR(10) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE customer (
  customer_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  name   VARCHAR(100) NOT NULL unique,
  description   VARCHAR(100) NOT NULL,
  contactor_firstname   VARCHAR(100) NOT NULL,
  contactor_lastname   VARCHAR(100) NOT NULL,
  address_country   VARCHAR(100) NOT NULL,
  address_state   VARCHAR(100) NOT NULL,
  address_county   VARCHAR(100) NOT NULL,
  address_city   VARCHAR(100) NOT NULL,
  address_district   VARCHAR(100),
  address_line1   VARCHAR(300) NOT NULL,
  address_line2   VARCHAR(300),
  address_postcode  VARCHAR(10) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));

-- INSERT INTO unit_of_measure (name, description) VALUES ("EA", "Each");
-- INSERT INTO unit_of_measure (name, description) VALUES ("PK", "Pack");
-- INSERT INTO unit_of_measure (name, description) VALUES ("CS", "Case");
-- INSERT INTO unit_of_measure (name, description) VALUES ("PL", "Pallet");


CREATE TABLE reason_code (
  reason_code_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  name   VARCHAR(100) NOT NULL unique,
  description   VARCHAR(100) NOT NULL,
  type   int NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE system_controlled_number (
  system_controlled_number_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  variable   VARCHAR(100) NOT NULL unique,
  prefix   VARCHAR(10) NOT NULL,
  postfix   VARCHAR(10) NOT NULL,
  length   int NOT NULL,
  current_number   VARCHAR(100) NOT NULL,
  rollover boolean not null default 0,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));

CREATE TABLE policy (
  policy_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  policy_key   VARCHAR(100) NOT NULL unique,
  policy_value   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE carrier (
  carrier_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  name   VARCHAR(100) NOT NULL unique,
  description   VARCHAR(100) NOT NULL,
  contactor_firstname   VARCHAR(100) NOT NULL,
  contactor_lastname   VARCHAR(100) NOT NULL,
  address_country   VARCHAR(100) NOT NULL,
  address_state   VARCHAR(100) NOT NULL,
  address_county   VARCHAR(100) NOT NULL,
  address_city   VARCHAR(100) NOT NULL,
  address_district   VARCHAR(100),
  address_line1   VARCHAR(300) NOT NULL,
  address_line2   VARCHAR(300),
  address_postcode  VARCHAR(10) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE carrier_service_level (
  carrier_service_level_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  type   VARCHAR(20) NOT NULL,
  carrier_id   BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(carrier_id) references carrier(carrier_id));



CREATE TABLE work_task (
   work_task_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   warehouse_id BIGINT NOT NULL,
   number VARCHAR(20) NOT NULL,
   type VARCHAR(50) NOT NULL,
   status VARCHAR(20) NOT NULL,
   source_location_id BIGINT NOT NULL,
   destination_location_id BIGINT,
   inventory_id BIGINT,
   assigned_user_id BIGINT,
   assigned_role_id BIGINT,
   assigned_working_team_id BIGINT,
   current_user_id BIGINT,
   complete_user_id BIGINT,
   start_time BIGINT,
   complete_time BIGINT,
   created_time DATETIME,
   created_by VARCHAR(50),
   last_modified_time DATETIME,
   last_modified_by VARCHAR(50));