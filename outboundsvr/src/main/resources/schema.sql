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
  shipped_quantity   BIGINT NOT NULL,
  inventory_status_id   BIGINT NOT NULL,
  outbound_order_id BIGINT  NOT NULL,
  foreign key(outbound_order_id) references outbound_order(outbound_order_id));



