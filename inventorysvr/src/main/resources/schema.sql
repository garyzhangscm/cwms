DROP TABLE IF EXISTS item_unit_of_measure;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS item_family;


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

CREATE TABLE item_unit_of_measure (
  item_unit_of_measure_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  item_id BIGINT NOT NULL,
  client_id BIGINT,
  supplier_id BIGINT,
  unit_of_measure_id BIGINT NOT NULL,
  quantity INT not null,
  weight double not null,
  length double not null,
  width double not null,
  height double not null,
  foreign key(item_id) references item(item_id));

