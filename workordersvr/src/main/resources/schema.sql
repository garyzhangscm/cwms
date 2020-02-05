
drop table if exists bill_of_material_line;
drop table if exists work_order_instruction_template;
drop table if exists bill_of_material;


drop table if exists production_line_activity;
drop table if exists work_order_assignment;
drop table if exists work_order_instruction;
drop table if exists work_order_line;
drop table if exists work_order;
drop table if exists production_line;

CREATE TABLE bill_of_material (
  bill_of_material_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  warehouse_id  BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  expected_quantity BIGINT NOT NULL);



CREATE TABLE bill_of_material_line (
  bill_of_material_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  bill_of_material_id BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  inventory_status_id BIGINT NOT NULL,
  expected_quantity BIGINT NOT NULL,
  foreign key(bill_of_material_id) references bill_of_material(bill_of_material_id));


CREATE TABLE work_order_instruction_template (
  work_order_instruction_template_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sequence  INT NOT NULL,
  bill_of_material_id  BIGINT NOT NULL,
  instruction  VARCHAR(500) NOT NULL,
  foreign key(bill_of_material_id) references bill_of_material(bill_of_material_id));

CREATE TABLE production_line (
  production_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name  VARCHAR(20) NOT NULL,
  warehouse_id  BIGINT NOT NULL,
  inbound_stage_location_id BIGINT NOT NULL,
  outbound_stage_location_id BIGINT NOT NULL,
  production_line_location_id BIGINT NOT NULL);


CREATE TABLE work_order (
  work_order_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  production_line_id  BIGINT,
  warehouse_id  BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  expected_quantity  BIGINT NOT NULL,
  produced_quantity BIGINT NOT NULL,
  foreign key(production_line_id) references production_line(production_line_id));

CREATE TABLE work_order_line (
  work_order_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(20) NOT NULL,
  work_order_id  BIGINT NOT NULL,
  item_id   BIGINT NOT NULL,
  expected_quantity  BIGINT NOT NULL,
  open_quantity BIGINT NOT NULL,
  inprocess_quantity BIGINT NOT NULL,
  consumed_quantity BIGINT NOT NULL,
  inventory_status_id BIGINT NOT NULL,
  foreign key(work_order_id) references work_order(work_order_id));

CREATE TABLE work_order_instruction (
  work_order_instruction_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sequence  INT NOT NULL,
  work_order_id  BIGINT NOT NULL,
  instruction  VARCHAR(500) NOT NULL,
  foreign key(work_order_id) references work_order(work_order_id));

CREATE TABLE work_order_assignment (
  work_order_assignment_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id  BIGINT NOT NULL,
  work_order_id  BIGINT NOT NULL,
  foreign key(work_order_id) references work_order(work_order_id));

CREATE TABLE production_line_activity (
  production_line_activity_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id  BIGINT NOT NULL,
  work_order_id  BIGINT NOT NULL,
  production_line_id BIGINT NOT NULL,
  type  VARCHAR(20) NOT NULL,
  foreign key(work_order_id) references work_order(work_order_id),
  foreign key(production_line_id) references production_line(production_line_id));