
drop table if exists  pick_movement;
drop table if exists pick;
drop table if exists pick_list;
drop table if exists short_allocation;
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


CREATE TABLE outbound_order (
  outbound_order_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
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
  client_id  BIGINT,
  warehouse_id BIGINT not null
  );

CREATE TABLE outbound_order_line(
  outbound_order_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  warehouse_id BIGINT not null,
  item_id BIGINT  NOT NULL,
  expected_quantity   BIGINT NOT NULL,
  open_quantity   BIGINT NOT NULL,
  inprocess_quantity   BIGINT NOT NULL,
  shipped_quantity   BIGINT NOT NULL,
  inventory_status_id   BIGINT NOT NULL,
  outbound_order_id BIGINT  NOT NULL,
  carrier_id BIGINT,
  carrier_service_level_id BIGINT,
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
  enabled boolean not null default 0);


CREATE TABLE stop(
  stop_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  trailer_id BIGINT,
  foreign key(trailer_id) references trailer(trailer_id));

CREATE TABLE shipment(
  shipment_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  number   VARCHAR(100) NOT NULL,
  status   VARCHAR(20) NOT NULL,
  stop_id BIGINT,
  carrier_id BIGINT,
  carrier_service_level_id BIGINT,
  foreign key(stop_id) references stop(stop_id));


CREATE TABLE wave(
  wave_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  status VARCHAR(20) NOT NULL,
  number   VARCHAR(100) NOT NULL);

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
  status VARCHAR(20) not null,
  foreign key(shipment_line_id) references shipment_line(shipment_line_id));

CREATE TABLE pick_list(
  pick_list_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  group_key  VARCHAR(100) NOT NULL,
  status   VARCHAR(20) NOT NULL);


CREATE TABLE pick(
  pick_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  number   VARCHAR(100) NOT NULL,
  source_location_id BIGINT  NOT NULL,
  destination_location_id BIGINT,
  item_id BIGINT  NOT NULL,
  status VARCHAR(20) not null,
  type VARCHAR(20) not null,
  shipment_line_id BIGINT,
  work_order_line_id BIGINT,
  short_allocation_id BIGINT,
  quantity BIGINT  NOT NULL,
  picked_quantity BIGINT  NOT NULL,
  pick_list_id BIGINT,
  foreign key(shipment_line_id) references shipment_line(shipment_line_id),
  foreign key(pick_list_id) references pick_list(pick_list_id),
  foreign key(short_allocation_id) references short_allocation(short_allocation_id));

CREATE TABLE pick_movement(
  pick_movement_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  pick_id   BIGINT  NOT NULL,
  location_id BIGINT  NOT NULL,
  sequence int  NOT NULL,
  foreign key(pick_id) references pick(pick_id));



CREATE TABLE allocation_configuration(
  allocation_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  type int not null,
  item_id BIGINT,
  item_family_id BIGINT,
  location_id BIGINT,
  location_group_id BIGINT,
  location_group_type_id BIGINT,
  allocation_strategy VARCHAR(20) NOT NULL);


CREATE TABLE emergency_replenishment_configuration(
  emergency_replenishment_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  item_id BIGINT,
  item_family_id BIGINT,
  source_location_id BIGINT,
  source_location_group_id BIGINT,
  destination_location_id BIGINT,
  destination_location_group_id BIGINT);

create table pickable_unit_of_measure(
  pickable_unit_of_measure_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  unit_of_measure_id BIGINT NOT NULL,
  allocation_configuration_id BIGINT NOT NULL,
  foreign key(allocation_configuration_id) references allocation_configuration(allocation_configuration_id));

CREATE TABLE shipping_stage_area_configuration(
  shipping_stage_area_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  location_group_id BIGINT NOT NULL,
  location_reserve_strategy VARCHAR(20) NOT NULL);


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
  enabled boolean not null default 0);


CREATE TABLE list_picking_configuration(
  list_picking_configuration_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT not null,
  sequence int  NOT NULL,
  client_id BIGINT,
  pick_type  VARCHAR(20) NOT NULL,
  status  VARCHAR(20) NOT NULL,
  group_rule  VARCHAR(20) NOT NULL,
  enabled boolean not null default 0);


