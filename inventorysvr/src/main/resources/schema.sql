
drop table if exists cycle_count_batch;
drop table if exists cycle_count_result;
drop table if exists cycle_count_request;
drop table if exists audit_count_result;
drop table if exists audit_count_request;


DROP TABLE if exists inventory_movement;
DROP TABLE if exists inventory;
drop table if exists inventory_status;
DROP TABLE IF EXISTS item_unit_of_measure;
DROP TABLE IF EXISTS item_package_type;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS item_family;


DROP TABLE IF EXISTS movement_path_detail;
DROP TABLE IF EXISTS movement_path;



CREATE TABLE item_family (
  item_family_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL);


-- INSERT INTO item_family (name, description) VALUES ("危险品", "危险品");
-- INSERT INTO item_family (name, description) VALUES ("高价值", "高价值商品");
-- INSERT INTO item_family (name, description) VALUES ("低温", "需要低温（4-10度）保存的商品");
-- INSERT INTO item_family (name, description) VALUES ("冷冻", "需要冷冻（-46度）保存的商品");


CREATE TABLE item(
  item_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  client_id BIGINT,
  item_family_id BIGINT,
  unit_cost double,
  foreign key(item_family_id) references item_family(item_family_id));

CREATE TABLE item_package_type(
  item_package_type_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL,
  item_id BIGINT NOT NULL,
  client_id BIGINT,
  supplier_id BIGINT,
  foreign key(item_id) references item(item_id));

CREATE TABLE item_unit_of_measure (
  item_unit_of_measure_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  item_package_type_id BIGINT NOT NULL,
  unit_of_measure_id BIGINT NOT NULL,
  quantity INT not null,
  weight double not null,
  length double not null,
  width double not null,
  height double not null,
  foreign key(item_package_type_id) references item_package_type(item_package_type_id));

create table inventory_status(
  inventory_status_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL,
  description   VARCHAR(100) NOT NULL
);

CREATE TABLE inventory(
  inventory_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  lpn   VARCHAR(100) NOT NULL,
  location_id    BIGINT NOT NULL,
  pick_id    BIGINT,
  item_id BIGINT not null,
  item_package_type_id BIGINT not null,
  quantity bigint not null,
  inventory_status_id bigint not null,
  virtual_inventory boolean not null default 0,
  receipt_id BIGINT,
  foreign key(item_id) references item(item_id),
  foreign key(item_package_type_id) references item_package_type(item_package_type_id),
  foreign key(inventory_status_id) references inventory_status(inventory_status_id)
);

CREATE TABLE inventory_movement(
  inventory_movement_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  inventory_id    BIGINT NOT NULL,
  location_id    BIGINT not null,
  sequence INT not null,
  foreign key(inventory_id) references inventory(inventory_id)
);


create table cycle_count_batch(
  cycle_count_batch_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id VARCHAR(100) NOT NULL
);
create table cycle_count_request(
  cycle_count_request_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id VARCHAR(100) NOT NULL,
  location_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL
);

create table audit_count_request(
  audit_count_request_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id VARCHAR(100) NOT NULL,
  location_id BIGINT NOT NULL
);

create table cycle_count_result(
  cycle_count_result_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id VARCHAR(100) NOT NULL,
  location_id BIGINT NOT NULL,
  item_id BIGINT,
  quantity int NOT NULL,
  count_quantity int NOT NULL,
  audit_count_request_id BIGINT,
  foreign key(item_id) references item(item_id)
);

create table audit_count_result(
  audit_count_result_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id VARCHAR(100) NOT NULL,
  location_id BIGINT NOT NULL,
  inventory_id BIGINT,
  quantity int not null,
  count_quantity int NOT NULL
);


CREATE TABLE movement_path(
  movement_path_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  from_location_id    BIGINT,
  from_location_group_id    BIGINT,
  to_location_id BIGINT,
  to_location_group_id BIGINT,
  sequence INT not null
);


CREATE TABLE movement_path_detail(
  movement_path_detail_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  movement_path_id    BIGINT NOT NULL,
  hop_location_id    BIGINT,
  hop_location_group_id BIGINT,
  sequence INT not null,
  strategy INT not null,
  foreign key(movement_path_id) references movement_path(movement_path_id)
);
