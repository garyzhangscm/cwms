
drop table if exists  pick_movement;
drop table if exists pick;
drop  table  if exists cancelled_pick;
drop  table  if exists  cartonization;
drop  table  if exists shipping_cartonization;
drop table if exists  carton;
drop table if exists pick_list;
drop table if exists short_allocation;
drop table if exists short_allocation_configuration;
drop table if exists cancelled_short_allocation;
drop table if exists shipment_line;
DROP TABLE if exists shipment;
DROP TABLE if exists stop;
DROP TABLE if exists trailer;
DROP TABLE if exists wave;

drop table if exists outbound_order_line;
DROP TABLE if exists outbound_order;

drop table if exists pickable_unit_of_measure;
DROP TABLE if exists allocation_configuration;
drop table if exists  shipping_stage_area_configuration;
drop table if exists emergency_replenishment_configuration;

drop table if exists trailer_template;

drop table if exists list_picking_configuration;
drop table if exists cartonization_configuration_group_rule;
drop table if exists cartonization_configuration;

drop table if exists grid_configuration;
drop table  if exists grid_location_configuration;


CREATE TABLE outbound_order (
  outbound_order_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  status  VARCHAR(20) NOT NULL,
  ship_to_customer_id   BIGINT,
  bill_to_customer_id   BIGINT,
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
  stage_location_group_id BIGINT,
  carrier_id BIGINT,
  carrier_service_level_id BIGINT,
  client_id  BIGINT,
  warehouse_id BIGINT not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50)
  );

CREATE TABLE outbound_order_line(
  outbound_order_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  warehouse_id BIGINT not null,
  item_id BIGINT  NOT NULL,
  expected_quantity   BIGINT NOT NULL,
  open_quantity   BIGINT NOT NULL,
  inprocess_quantity   BIGINT NOT NULL,
  production_plan_inprocess_quantity BIGINT NOT NULL,
  production_plan_produced_quantity BIGINT NOT NULL,
  shipped_quantity   BIGINT NOT NULL,
  inventory_status_id   BIGINT NOT NULL,
  outbound_order_id BIGINT  NOT NULL,
  allocation_strategy_type VARCHAR(25),
  carrier_id BIGINT,
  carrier_service_level_id BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(outbound_order_id) references outbound_order(outbound_order_id));


CREATE TABLE trailer(
  trailer_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  driver_first_name VARCHAR(50)  NOT NULL,
  driver_last_name VARCHAR(50) NOT NULL,
  driver_phone VARCHAR(50) NOT NULL,
  license_plate_number VARCHAR(50) NOT NULL,
  number VARCHAR(50) NOT NULL,
  size VARCHAR(50) NOT NULL,
  type VARCHAR(50) NOT NULL,
  carrier_id BIGINT,
  carrier_service_level_id BIGINT,
  location_id BIGINT,
  status VARCHAR(20) NOT NULL,
  enabled boolean not null default 0,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE stop(
  stop_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  trailer_id BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(trailer_id) references trailer(trailer_id));

CREATE TABLE shipment(
  shipment_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  number   VARCHAR(100) NOT NULL,
  status   VARCHAR(20) NOT NULL,
  client_id BIGINT,
  stop_id BIGINT,
  carrier_id BIGINT,
  carrier_service_level_id BIGINT,
  ship_to_customer_id   BIGINT,
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
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(stop_id) references stop(stop_id));


CREATE TABLE wave(
  wave_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  status VARCHAR(20) NOT NULL,
  number   VARCHAR(100) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));

CREATE TABLE shipment_line(
  shipment_line_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  number   VARCHAR(100) NOT NULL,
  shipment_id BIGINT  NOT NULL,
  wave_id BIGINT  NOT NULL,
  outbound_order_line_id BIGINT  NOT NULL,
  quantity BIGINT  NOT NULL,
  open_quantity BIGINT  NOT NULL,
  inprocess_quantity BIGINT  NOT NULL,
  loaded_quantity BIGINT  NOT NULL,
  shipped_quantity BIGINT  NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(shipment_id) references shipment(shipment_id),
  foreign key(outbound_order_line_id) references outbound_order_line(outbound_order_line_id),
  foreign key(wave_id) references wave(wave_id));


CREATE TABLE short_allocation(
  short_allocation_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  item_id BIGINT  NOT NULL,
  shipment_line_id BIGINT,
  work_order_line_id BIGINT,
  quantity BIGINT  NOT NULL,
  open_quantity BIGINT  NOT NULL,
  inprocess_quantity BIGINT  NOT NULL,
  delivered_quantity BIGINT  NOT NULL,
  allocation_count BIGINT not null,
  last_allocation_datetime datetime,
  status VARCHAR(20) not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(shipment_line_id) references shipment_line(shipment_line_id));


CREATE TABLE short_allocation_configuration(
  short_allocation_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  last_modify_datetime datetime,
  last_modify_username VARCHAR(50),
  enabled boolean not null,
  retry_interval BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50)
);

CREATE TABLE cancelled_short_allocation(
  cancelled_short_allocation_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  item_id BIGINT  NOT NULL,
  shipment_line_id BIGINT,
  work_order_line_id BIGINT,
  quantity BIGINT  NOT NULL,
  open_quantity BIGINT  NOT NULL,
  inprocess_quantity BIGINT  NOT NULL,
  delivered_quantity BIGINT  NOT NULL,
  allocation_count BIGINT not null,
  status VARCHAR(20) not null,
  cancelled_quantity BIGINT  NOT NULL,
  cancelled_username VARCHAR(100) not null,
  cancelled_date datetime not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(shipment_line_id) references shipment_line(shipment_line_id));

CREATE TABLE pick_list(
  pick_list_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  number  VARCHAR(20) NOT NULL,
  group_key  VARCHAR(100) NOT NULL,
  status   VARCHAR(20) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE carton(
  carton_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  name  VARCHAR(20) NOT NULL,
  length  double not null ,
  width  double not null ,
  height  double not null ,
  fill_rate  double not null ,
  enabled boolean not null,
  shipping_carton_flag boolean not null,
  picking_carton_flag boolean not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));

CREATE TABLE cartonization(
  cartonization_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  group_key_value  VARCHAR(100) NOT NULL,
  number  VARCHAR(20) NOT NULL,
  carton_id  BIGINT not null,
  pick_list_id  BIGINT ,
  carton_status VARCHAR(20) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(carton_id) references carton(carton_id));

CREATE TABLE shipping_cartonization(
  shipping_cartonization_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  number  VARCHAR(20) NOT NULL,
  carton_id  BIGINT not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(carton_id) references carton(carton_id));

CREATE TABLE grid_configuration(
  grid_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  location_group_id  BIGINT not null,
  pre_assigned_location boolean not null,
  allow_confirm_by_group boolean not null,
  deposit_on_confirm  boolean not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE grid_location_configuration(
  grid_location_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  location_group_id  BIGINT not null,
  location_id  BIGINT not null,
  row_num INT not null,
  column_span INT not null,
  sequence INT not null,
  permanent_lpn_flag boolean not null,
  permanent_lpn VARCHAR(25),
  current_lpn VARCHAR(25),
  pending_quantity BIGINT not null,
  arrived_quantity BIGINT not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));



CREATE TABLE pick(
  pick_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  number   VARCHAR(100) NOT NULL,
  source_location_id BIGINT  NOT NULL,
  destination_location_id BIGINT,
  item_id BIGINT  NOT NULL,
  status VARCHAR(20) not null,
  lpn VARCHAR(100),
  type VARCHAR(30) not null,
  shipment_line_id BIGINT,
  work_order_line_id BIGINT,
  short_allocation_id BIGINT,
  quantity BIGINT  NOT NULL,
  picked_quantity BIGINT  NOT NULL,
  pick_list_id BIGINT,
  inventory_status_id BIGINT NOT NULL,
  cartonization_id   BIGINT,
  unit_of_measure_id BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(shipment_line_id) references shipment_line(shipment_line_id),
  foreign key(pick_list_id) references pick_list(pick_list_id),
  foreign key(short_allocation_id) references short_allocation(short_allocation_id),
  foreign key(cartonization_id) references cartonization(cartonization_id));


CREATE TABLE cancelled_pick(
  cancelled_pick_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  pick_id  BIGINT not null,
  pick_number VARCHAR(100) NOT NULL,
  number   VARCHAR(100) NOT NULL unique,
  warehouse_id BIGINT not null,
  source_location_id BIGINT  NOT NULL,
  destination_location_id BIGINT,
  item_id BIGINT  NOT NULL,
  type VARCHAR(20) not null,
  shipment_line_id BIGINT,
  work_order_line_id BIGINT,
  short_allocation_id BIGINT,
  quantity BIGINT  NOT NULL,
  picked_quantity BIGINT  NOT NULL,
  cancelled_quantity BIGINT  NOT NULL,
  pick_list_id BIGINT,
  inventory_status_id BIGINT NOT NULL,
  cartonization_id   BIGINT,
  cancelled_username VARCHAR(100) not null,
  cancelled_date datetime not null,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(shipment_line_id) references shipment_line(shipment_line_id),
  foreign key(pick_list_id) references pick_list(pick_list_id),
  foreign key(short_allocation_id) references short_allocation(short_allocation_id),
  foreign key(cartonization_id) references cartonization(cartonization_id));

CREATE TABLE pick_movement(
  pick_movement_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  pick_id   BIGINT  NOT NULL,
  location_id BIGINT  NOT NULL,
  sequence int  NOT NULL,
  arrived_quantity BIGINT  NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(pick_id) references pick(pick_id));



CREATE TABLE allocation_configuration(
  allocation_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  type VARCHAR(20)  not null,
  item_id BIGINT,
  item_family_id BIGINT,
  location_id BIGINT,
  location_group_id BIGINT,
  location_group_type_id BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE emergency_replenishment_configuration(
  emergency_replenishment_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  unit_of_measure_id BIGINT  NOT NULL,
  item_id BIGINT,
  item_family_id BIGINT,
  source_location_id BIGINT,
  source_location_group_id BIGINT,
  destination_location_id BIGINT,
  destination_location_group_id BIGINT,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));

create table pickable_unit_of_measure(
  pickable_unit_of_measure_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  unit_of_measure_id BIGINT NOT NULL,
  allocation_configuration_id BIGINT NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
  foreign key(allocation_configuration_id) references allocation_configuration(allocation_configuration_id));

CREATE TABLE shipping_stage_area_configuration(
  shipping_stage_area_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  location_group_id BIGINT NOT NULL,
  location_reserve_strategy VARCHAR(20) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE trailer_template(
  trailer_template_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  driver_first_name VARCHAR(50)  NOT NULL,
  driver_last_name VARCHAR(50) NOT NULL,
  driver_phone VARCHAR(50) NOT NULL,
  license_plate_number VARCHAR(50) NOT NULL,
  number VARCHAR(50) NOT NULL,
  size VARCHAR(50) NOT NULL,
  type VARCHAR(50) NOT NULL,
  enabled boolean not null default 0,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE list_picking_configuration(
  list_picking_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  client_id BIGINT,
  pick_type  VARCHAR(20) NOT NULL,
  group_rule  VARCHAR(20) NOT NULL,
  enabled boolean not null default 0,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));


CREATE TABLE cartonization_configuration(
  cartonization_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  client_id BIGINT,
  pick_type  VARCHAR(20) NOT NULL,
  enabled boolean not null default 0,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50));

create table cartonization_configuration_group_rule (
    cartonization_configuration_id bigint not null,
    group_rule VARCHAR(20) NOT NULL,
  created_time DATETIME,
  created_by VARCHAR(50),
  last_modified_time DATETIME,
  last_modified_by VARCHAR(50),
    primary key (cartonization_configuration_id, group_rule)
);




