
drop table if exists  work_order_kpi;

drop table if exists  work_order_line_consume_transaction;
drop table if exists  work_order_by_product_produce_transaction;
drop table if exists  work_order_kpi_transaction;
drop table if exists  work_order_produced_inventory;
drop table if exists  work_order_produce_transaction;

drop table if exists  return_material_request;
drop table if exists  work_order_line_complete_transaction;
drop table if exists  work_order_complete_transaction;


drop table if exists production_line_activity;
drop table if exists work_order_assignment;
drop table if exists work_order_instruction;
drop table if exists work_order_line;
drop table if exists work_order_by_product;
drop table if exists work_order;
drop table if exists production_line;

drop table if exists production_plan_line;
drop table if exists production_plan;

drop table if exists bill_of_material_line;
drop table if exists bill_of_material_by_product;
drop table if exists work_order_instruction_template;
drop table if exists bill_of_material;


CREATE TABLE bill_of_material (
  bill_of_material_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  warehouse_id  BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  expected_quantity BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));



CREATE TABLE bill_of_material_line (
  bill_of_material_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  bill_of_material_id BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  inventory_status_id BIGINT NOT NULL,
  expected_quantity BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(bill_of_material_id) references bill_of_material(bill_of_material_id));


CREATE TABLE production_plan (
  production_plan_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  description  VARCHAR(100),
  warehouse_id  BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));

CREATE TABLE production_plan_line (
  production_plan_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  production_plan_id      BIGINT NOT NULL,
  order_line_id BIGINT,
  warehouse_id  BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  bill_of_material_id BIGINT NOT NULL,
  expected_quantity BIGINT NOT NULL,
  inprocess_quantity BIGINT NOT NULL,
  produced_quantity BIGINT NOT NULL,
  inventory_status_id BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(bill_of_material_id) references bill_of_material(bill_of_material_id),
  foreign key(production_plan_id) references production_plan(production_plan_id));

CREATE TABLE bill_of_material_by_product (
  bill_of_material_by_product_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bill_of_material_id BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  inventory_status_id BIGINT NOT NULL,
  expected_quantity BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(bill_of_material_id) references bill_of_material(bill_of_material_id));

CREATE TABLE work_order_instruction_template (
  work_order_instruction_template_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sequence  INT NOT NULL,
  bill_of_material_id  BIGINT NOT NULL,
  instruction  VARCHAR(500) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(bill_of_material_id) references bill_of_material(bill_of_material_id));

CREATE TABLE production_line (
  production_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name  VARCHAR(20) NOT NULL,
  warehouse_id  BIGINT NOT NULL,
  inbound_stage_location_id BIGINT NOT NULL,
  outbound_stage_location_id BIGINT NOT NULL,
  production_line_location_id BIGINT NOT NULL,
  work_order_exclusive_flag boolean not null,
  enabled boolean not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE work_order (
  work_order_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  production_line_id  BIGINT,
  bill_of_material_id BIGINT,
  production_plan_line_id BIGINT,
  warehouse_id  BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  expected_quantity  BIGINT NOT NULL,
  produced_quantity BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(production_line_id) references production_line(production_line_id),
  foreign key(bill_of_material_id) references bill_of_material(bill_of_material_id),
  foreign key(production_plan_line_id) references production_plan_line(production_plan_line_id));

CREATE TABLE work_order_line (
  work_order_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  work_order_id  BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  expected_quantity  BIGINT NOT NULL,
  open_quantity BIGINT NOT NULL,
  inprocess_quantity BIGINT NOT NULL,
  delivered_quantity BIGINT NOT NULL,
  consumed_quantity BIGINT NOT NULL,
  scrapped_quantity BIGINT NOT NULL,
  returned_quantity BIGINT NOT NULL,
  inventory_status_id BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_id) references work_order(work_order_id));


CREATE TABLE work_order_by_product (
  work_order_by_product_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_id  BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  expected_quantity  BIGINT NOT NULL,
  produced_quantity BIGINT NOT NULL,
  inventory_status_id BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_id) references work_order(work_order_id));

CREATE TABLE work_order_instruction (
  work_order_instruction_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sequence  INT NOT NULL,
  work_order_id  BIGINT NOT NULL,
  instruction  VARCHAR(500) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_id) references work_order(work_order_id));

CREATE TABLE work_order_assignment (
  work_order_assignment_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id  BIGINT NOT NULL,
  work_order_id  BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_id) references work_order(work_order_id));

CREATE TABLE production_line_activity (
  production_line_activity_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id  BIGINT NOT NULL,
  work_order_id  BIGINT NOT NULL,
  production_line_id BIGINT NOT NULL,
  type  VARCHAR(20) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_id) references work_order(work_order_id),
  foreign key(production_line_id) references production_line(production_line_id));


CREATE TABLE work_order_produce_transaction (
  work_order_produce_transaction_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_id  BIGINT NOT NULL,
  consume_by_bom_quantity boolean not null default 1,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_id) references work_order(work_order_id));


CREATE TABLE work_order_line_consume_transaction (
  work_order_line_consume_transaction_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_produce_transaction_id  BIGINT NOT NULL,
  work_order_line_id  BIGINT NOT NULL,
  consumed_quantity  BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_produce_transaction_id) references work_order_produce_transaction(work_order_produce_transaction_id),
  foreign key(work_order_line_id) references work_order_line(work_order_line_id));



CREATE TABLE work_order_produced_inventory (
  work_order_produced_inventory_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_produce_transaction_id  BIGINT NOT NULL,
  lpn  VARCHAR(100) NOT NULL,
  quantity  BIGINT,
  inventory_status_id BIGINT,
  item_package_type_id BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_produce_transaction_id) references work_order_produce_transaction(work_order_produce_transaction_id));



CREATE TABLE work_order_complete_transaction (
  work_order_complete_transaction_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_id  BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_id) references work_order(work_order_id));


CREATE TABLE work_order_line_complete_transaction (
  work_order_line_complete_transaction_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_complete_transaction_id  BIGINT NOT NULL,
  work_order_line_id  BIGINT NOT NULL,
  adjusted_consumed_quantity  BIGINT,
  scrapped_quantity  BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_complete_transaction_id) references work_order_complete_transaction(work_order_complete_transaction_id),
  foreign key(work_order_line_id) references work_order_line(work_order_line_id));


CREATE TABLE return_material_request (
  return_material_request_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_line_complete_transaction_id  BIGINT NOT NULL,
  lpn  VARCHAR(100) NOT NULL,
  quantity  BIGINT,
  inventory_status_id BIGINT,
  item_package_type_id BIGINT,
  location_id BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_line_complete_transaction_id) references work_order_line_complete_transaction(work_order_line_complete_transaction_id));


CREATE TABLE work_order_kpi (
  work_order_kpi_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_id  BIGINT NOT NULL,
  username  VARCHAR(50),
  working_team_name VARCHAR(50),
  kpi_measurement  VARCHAR(20) not null,
  amount double not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_id) references work_order(work_order_id));


CREATE TABLE work_order_by_product_produce_transaction (
  work_order_by_product_produce_transaction_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_complete_transaction_id  BIGINT,
  work_order_produce_transaction_id  BIGINT,
  work_order_by_product_id  BIGINT NOT NULL,lpn  VARCHAR(100) NOT NULL,
  quantity  BIGINT,
  inventory_status_id BIGINT,
  item_package_type_id BIGINT,
  location_id BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_produce_transaction_id) references work_order_produce_transaction(work_order_produce_transaction_id),
  foreign key(work_order_complete_transaction_id) references work_order_complete_transaction(work_order_complete_transaction_id),
  foreign key(work_order_by_product_id) references work_order_by_product(work_order_by_product_id));




CREATE TABLE work_order_kpi_transaction (
  work_order_kpi_transaction_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  work_order_id  BIGINT NOT NULL,
  work_order_complete_transaction_id  BIGINT,
  work_order_produce_transaction_id  BIGINT,
  username  VARCHAR(50),
  type VARCHAR(50),
  working_team_name VARCHAR(50),
  kpi_measurement  VARCHAR(20) not null,
  amount double not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(work_order_produce_transaction_id) references work_order_produce_transaction(work_order_produce_transaction_id),
  foreign key(work_order_complete_transaction_id) references work_order_complete_transaction(work_order_complete_transaction_id),
  foreign key(work_order_id) references work_order(work_order_id));