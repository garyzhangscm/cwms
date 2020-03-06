DROP TABLE if exists integration_client;
DROP TABLE if exists integration_customer;
DROP TABLE if exists integration_supplier;

DROP TABLE if exists integration_item_unit_of_measure;
DROP TABLE if exists integration_item_package_type;
DROP TABLE if exists integration_item;

DROP TABLE if exists integration_item_family;


CREATE TABLE integration_customer (
  integration_customer_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
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
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL);

-- insert into integration_customer
--   (name, description, contactor_firstname, contactor_lastname, address_country,  address_state, address_county, address_city, address_district, address_line1 ,
--   address_line2, address_postcode, status, insert_time)
--  values ("CST-A", "Customer A", "Alex", "Andrew", "USA", "CA", "Orange", "Orange", "", "2164 N Batavia Street", "", "92865", "2", now())


CREATE TABLE integration_client (
  integration_client_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
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
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL);


CREATE TABLE integration_supplier (
  integration_supplier_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
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
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL);


CREATE TABLE integration_item_family (
  integration_item_family_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  warehouse_id   VARCHAR(100) NOT NULL,
  warehouse_name   VARCHAR(100) NOT NULL,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL);

CREATE TABLE integration_item (
  integration_item_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  client_id   BIGINT,
  client_name   VARCHAR(100) NOT NULL,
  integration_item_family_id   BIGINT,
  unit_cost   DOUBLE,
  warehouse_id   BIGINT,
  warehouse_name   VARCHAR(100) ,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL,
  foreign key(integration_item_family_id) references integration_item_family(integration_item_family_id));


CREATE TABLE integration_item_package_type (
  integration_item_package_type_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  client_id   BIGINT,
  client_name   VARCHAR(100),
  supplier_id   BIGINT,
  supplier_name   VARCHAR(100),
  warehouse_id   BIGINT,
  warehouse_name   VARCHAR(100),
  integration_item_id BIGINT,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL,
  foreign key(integration_item_id) references integration_item(integration_item_id));


CREATE TABLE integration_item_unit_of_measure (
  integration_item_unit_of_measure_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  item_package_type_id   BIGINT,
  item_package_type_name   VARCHAR(100),
  unit_of_measure_id   BIGINT,
  unit_of_measure_name   VARCHAR(100),
  quantity   BIGINT,
  weight   DOUBLE,
  length   DOUBLE,
  width   DOUBLE,
  height   DOUBLE,
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  integration_item_package_type_id BIGINT,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL,
  foreign key(integration_item_package_type_id) references integration_item_package_type(integration_item_package_type_id));

-- insert into integration_item_unit_of_measure
-- (item_name, item_package_type_name, unit_of_measure_name, quantity, weight, length, width, height, warehouse_name, status, insert_time)
--  values
--  ("6420704", "1X6X12", "PCS", 10, 1.1, 2.2, 3.3, 4.4, "WMEC", "2", now())