drop table if exists shipment_line;
DROP TABLE if exists shipment;
DROP TABLE if exists wave;

drop table if exists outbound_order_line;
DROP TABLE if exists outbound_order;


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
  client_id  BIGINT
  );

CREATE TABLE outbound_order_line(
  outbound_order_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  item_id BIGINT  NOT NULL,
  expected_quantity   BIGINT NOT NULL,
  open_quantity   BIGINT NOT NULL,
  inprocess_quantity   BIGINT NOT NULL,
  shipped_quantity   BIGINT NOT NULL,
  inventory_status_id   BIGINT NOT NULL,
  outbound_order_id BIGINT  NOT NULL,
  foreign key(outbound_order_id) references outbound_order(outbound_order_id));



CREATE TABLE shipment(
  shipment_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  outbound_order_id BIGINT  NOT NULL,
  status   VARCHAR(20) NOT NULL,
  foreign key(outbound_order_id) references outbound_order(outbound_order_id));


CREATE TABLE wave(
  wave_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL);

CREATE TABLE shipment_line(
  shipment_line_id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  shipment_id BIGINT  NOT NULL,
  wave_id BIGINT  NOT NULL,
  outbound_order_line_id BIGINT  NOT NULL,
  quantity BIGINT  NOT NULL,
  shipped_quantity BIGINT  NOT NULL,
  foreign key(shipment_id) references shipment(shipment_id),
  foreign key(outbound_order_line_id) references outbound_order_line(outbound_order_line_id),
  foreign key(wave_id) references wave(wave_id));



