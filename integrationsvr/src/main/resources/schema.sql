DROP TABLE if exists integration_client;
DROP TABLE if exists integration_customer;
DROP TABLE if exists integration_supplier;

DROP TABLE if exists integration_item_unit_of_measure;
DROP TABLE if exists integration_item_package_type;
DROP TABLE if exists integration_item;

DROP TABLE if exists integration_item_family;


DROP TABLE if exists integration_inventory_adjustment_confirmation;
DROP TABLE if exists integration_inventory_attribute_change_confirmation;
DROP TABLE if exists integration_inventory_shippping_confirmation;

DROP TABLE if exists integration_order_line;
DROP TABLE if exists integration_order;

DROP TABLE if exists integration_order_line_confirmation;
DROP TABLE if exists integration_order_confirmation;

DROP TABLE if exists integration_receipt_line;
DROP TABLE if exists integration_receipt;

DROP TABLE if exists integration_receipt_line_confirmation;
DROP TABLE if exists integration_receipt_confirmation;

DROP TABLE if exists integration_shipment_line_confirmation;


DROP TABLE if exists integration_work_order_line_confirmation;
DROP TABLE if exists integration_work_order_confirmation;

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
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

 insert into integration_customer
   (name, description, contactor_firstname, contactor_lastname, address_country,  address_state, address_county, address_city, address_district, address_line1 ,
   address_line2, address_postcode, status, insert_time)
  values ("CST-A", "Customer A", "Alex", "Andrew", "USA", "CA", "Orange", "Orange", "", "2164 N Batavia Street", "", "92865", "PENDING", now());


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
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

 insert into integration_client
   (name, description, contactor_firstname, contactor_lastname, address_country,  address_state, address_county, address_city, address_district, address_line1 ,
   address_line2, address_postcode, status, insert_time)
  values ("CLIENT-A", "Client A", "Alex", "Andrew", "USA", "CA", "Orange", "Orange", "", "2164 N Batavia Street", "", "92865", "PENDING", now());


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
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

 insert into integration_supplier
   (name, description, contactor_firstname, contactor_lastname, address_country,  address_state, address_county, address_city, address_district, address_line1 ,
   address_line2, address_postcode, status, insert_time)
  values ("SUPPLIER-A", "Supplier A", "Alex", "Andrew", "USA", "CA", "Orange", "Orange", "", "2164 N Batavia Street", "", "92865", "PENDING", now());

CREATE TABLE integration_item_family (
  integration_item_family_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  warehouse_id   VARCHAR(100),
  warehouse_name   VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

 insert into integration_item_family
   (name, description,  warehouse_name, status, insert_time)
  values ("ITEM-FAMILY-A", "Item Family A", "WMOR", "PENDING", now());

CREATE TABLE integration_item (
  integration_item_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  client_id   BIGINT,
  client_name   VARCHAR(100),
  integration_item_family_id   BIGINT,
  unit_cost   DOUBLE,
  warehouse_id   BIGINT,
  warehouse_name   VARCHAR(100) ,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL,
  error_message VARCHAR(1000),
  foreign key(integration_item_family_id) references integration_item_family(integration_item_family_id));

 insert into integration_item
   (name, description,  client_name, unit_cost, warehouse_name, status, insert_time)
  values ("ITEM-A", "Item A", "CLIENT-A", 5.0,  "WMOR", "PENDING", now());

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
  error_message VARCHAR(1000),
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
  error_message VARCHAR(1000),
  foreign key(integration_item_package_type_id) references integration_item_package_type(integration_item_package_type_id));
-- insert into integration_item_unit_of_measure
-- (item_name, item_package_type_name, unit_of_measure_name, quantity, weight, length, width, height, warehouse_name, status, insert_time)
--  values
--  ("6420704", "1X6X12", "PCS", 10, 1.1, 2.2, 3.3, 4.4, "WMEC", "2", now())


CREATE TABLE integration_inventory_adjustment_confirmation (
  integration_inventory_adjustment_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  adjust_quantity BIGINT NOT NULL,
  inventory_status_id   BIGINT,
  inventory_status_name   VARCHAR(100),
  client_id   BIGINT,
  client_name   VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

CREATE TABLE integration_inventory_attribute_change_confirmation (
  integration_inventory_attribute_change_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  quantity BIGINT NOT NULL,
  inventory_status_id   BIGINT,
  inventory_status_name   VARCHAR(100),
  client_id   BIGINT,
  client_name   VARCHAR(100),
  attribute_name VARCHAR(100) NOT NULL,
  original_value VARCHAR(100),
  new_value VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

CREATE TABLE integration_inventory_shippping_confirmation (
  integration_inventory_shippping_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  shipment_number VARCHAR(100) NOT NULL,
  shipment_line_number VARCHAR(100) NOT NULL,
  order_number VARCHAR(100) NOT NULL,
  order_line_number VARCHAR(100) NOT NULL,
  lpn VARCHAR(100) NOT NULL,
  quantity   BIGINT NOT NULL,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  order_expected_quantity BIGINT NOT NULL,
  order_shipped_quantity  BIGINT NOT NULL,
  shipment_shipped_quantity BIGINT NOT NULL,
  inventory_status_id   BIGINT,
  inventory_status_name   VARCHAR(100),
  client_id   BIGINT,
  client_name   VARCHAR(100),
  carrier_id   BIGINT,
  carrier_name VARCHAR(100),
  carrier_service_level_id   BIGINT,
  carrier_service_level_name VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

CREATE TABLE integration_order (
  integration_order_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  ship_to_customer_id   BIGINT,
  ship_to_customer_name  VARCHAR(100),
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  bill_to_customer_id   BIGINT,
  bill_to_customer_name  VARCHAR(100),
  ship_to_contactor_firstname   VARCHAR(100),
  ship_to_contactor_lastname   VARCHAR(100),
  ship_to_address_country   VARCHAR(100),
  ship_to_address_state   VARCHAR(100),
  ship_to_address_county   VARCHAR(100),
  ship_to_address_city   VARCHAR(100),
  ship_to_address_district   VARCHAR(100),
  ship_to_address_line1   VARCHAR(100),
  ship_to_address_line2   VARCHAR(100),
  ship_to_address_postcode   VARCHAR(100),
  bill_to_contactor_firstname   VARCHAR(100),
  bill_to_contactor_lastname   VARCHAR(100),
  bill_to_address_country   VARCHAR(100),
  bill_to_address_state   VARCHAR(100),
  bill_to_address_county   VARCHAR(100),
  bill_to_address_city   VARCHAR(100),
  bill_to_address_district   VARCHAR(100),
  bill_to_address_line1   VARCHAR(100),
  bill_to_address_line2   VARCHAR(100),
  bill_to_address_postcode   VARCHAR(100),
  client_id   BIGINT,
  client_name   VARCHAR(100),
  carrier_id   BIGINT,
  carrier_name VARCHAR(100),
  carrier_service_level_id   BIGINT,
  carrier_service_level_name VARCHAR(100),
  stage_location_group_id BIGINT,
  stage_location_group_name VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

CREATE TABLE integration_order_line(
  integration_order_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  expected_quantity   BIGINT NOT NULL,
  inventory_status_id   BIGINT,
  inventory_status_name   VARCHAR(100),
  integration_order_id BIGINT  NOT NULL,
  carrier_id   BIGINT,
  carrier_name VARCHAR(100),
  carrier_service_level_id   BIGINT,
  carrier_service_level_name VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL,
  foreign key(integration_order_id) references integration_order(integration_order_id));


CREATE TABLE integration_order_confirmation(
  integration_order_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

CREATE TABLE integration_order_line_confirmation(
  integration_order_line_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  expected_quantity BIGINT NOT NULL,
  open_quantity  BIGINT NOT NULL,
  inprocess_quantity  BIGINT NOT NULL,
  shipped_quantity BIGINT NOT NULL,
  inventory_status_id   BIGINT,
  inventory_status_name   VARCHAR(100),
  integration_order_confirmation_id BIGINT  NOT NULL,
  carrier_id   BIGINT,
  carrier_name VARCHAR(100),
  carrier_service_level_id   BIGINT,
  carrier_service_level_name VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL,
  error_message VARCHAR(1000),
  foreign key(integration_order_confirmation_id) references integration_order_confirmation(integration_order_confirmation_id));




CREATE TABLE integration_receipt (
  integration_receipt_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  client_id   BIGINT,
  client_name   VARCHAR(100),
  supplier_id  BIGINT,
  supplier_name  VARCHAR(100),
  allow_unexpected_item boolean not null,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

CREATE TABLE integration_receipt_line(
  integration_receipt_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  item_id   BIGINT,
  item_name   VARCHAR(100),
  expected_quantity   BIGINT NOT NULL,
  integration_receipt_id BIGINT,
  over_receiving_quantity  BIGINT NOT NULL,
  over_receiving_percent  DOUBLE NOT NULL,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL,
  error_message VARCHAR(1000),
  foreign key(integration_receipt_id) references integration_receipt(integration_receipt_id));


CREATE TABLE integration_receipt_confirmation (
  integration_receipt_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  client_id   BIGINT,
  client_name   VARCHAR(100),
  supplier_id  BIGINT,
  supplier_name  VARCHAR(100),
  allow_unexpected_item boolean not null,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);


CREATE TABLE integration_receipt_line_confirmation(
  integration_receipt_line_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  item_id   BIGINT,
  item_name   VARCHAR(100),
  expected_quantity   BIGINT NOT NULL,
  received_quantity  BIGINT NOT NULL,
  integration_receipt_confirmation_id BIGINT,
  over_receiving_quantity  BIGINT NOT NULL,
  over_receiving_percent  DOUBLE NOT NULL,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL,
  error_message VARCHAR(1000),
  foreign key(integration_receipt_confirmation_id) references integration_receipt_confirmation(integration_receipt_confirmation_id));



CREATE TABLE integration_shipment_line_confirmation(
  integration_shipment_line_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  shipment_number VARCHAR(100) NOT NULL,
  shipment_line_number VARCHAR(100) NOT NULL,
  order_number VARCHAR(100) NOT NULL,
  order_line_number VARCHAR(100) NOT NULL,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  order_expected_quantity BIGINT NOT NULL,
  order_shipped_quantity BIGINT NOT NULL,
  shipment_shipped_quantity BIGINT NOT NULL,
  inventory_status_id   BIGINT,
  inventory_status_name   VARCHAR(100),
  carrier_id   BIGINT,
  carrier_name VARCHAR(100),
  carrier_service_level_id   BIGINT,
  carrier_service_level_name VARCHAR(100),
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);


CREATE TABLE integration_work_order_confirmation(
  integration_work_order_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  production_line_name VARCHAR(100),
  bill_of_material_name VARCHAR(100),
  item_id   BIGINT,
  item_name   VARCHAR(100),
  warehouse_id  BIGINT,
  warehouse_name   VARCHAR(100),
  expected_quantity BIGINT NOT NULL,
  produced_quantity BIGINT NOT NULL,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  error_message VARCHAR(1000),
  status VARCHAR(10) NOT NULL);

CREATE TABLE integration_work_order_line_confirmation(
  integration_work_order_line_confirmation_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  item_id   BIGINT,
  item_name   VARCHAR(100),
  expected_quantity BIGINT NOT NULL,
  open_quantity  BIGINT NOT NULL,
  inprocess_quantity  BIGINT NOT NULL,
  delivered_quantity BIGINT NOT NULL,
  consumed_quantity BIGINT NOT NULL,
  scrapped_quantity BIGINT NOT NULL,
  returned_quantity BIGINT NOT NULL,
  inventory_status_id   BIGINT,
  inventory_status_name   VARCHAR(100),
  integration_work_order_confirmation_id BIGINT  NOT NULL,
  insert_time  DATETIME  NOT NULL,
  last_update_time  DATETIME,
  status VARCHAR(10) NOT NULL,
  error_message VARCHAR(1000),
  foreign key(integration_work_order_confirmation_id) references integration_work_order_confirmation(integration_work_order_confirmation_id));