DROP TABLE if exists receipt_line;
drop table if exists receipt;
drop table if exists putaway_configuration;


CREATE TABLE receipt (
  receipt_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  client_id   BIGINT,
  warehouse_id   BIGINT,
  status   int not null,
  allow_unexpected_item boolean not null,
  supplier_id   BIGINT);

CREATE TABLE receipt_line(
  receipt_line_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  number   VARCHAR(100) NOT NULL,
  item_id BIGINT,
  warehouse_id   BIGINT,
  expected_quantity   BIGINT NOT NULL,
  received_quantity   BIGINT NOT NULL,
  receipt_id BIGINT,
  over_receiving_quantity  BIGINT NOT NULL,
  over_receiving_percent  DOUBLE NOT NULL,
  foreign key(receipt_id) references receipt(receipt_id));



CREATE TABLE  putaway_configuration(
    putaway_configuration_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sequence INT NOT NULL unique,
    item_id BIGINT,
    warehouse_id   BIGINT,
    item_family_id BIGINT,
    inventory_status_id BIGINT,
    location_id BIGINT,
    location_group_id BIGINT,
    location_group_type_id BIGINT,
    strategies VARCHAR(100) NOT NULL
);
