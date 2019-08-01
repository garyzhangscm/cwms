DROP TABLE IF EXISTS warehouse;

CREATE TABLE warehouse (
  warehouse_id      INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(100) NOT NULL);


INSERT INTO warehouse (name) VALUES ("SH");
INSERT INTO warehouse (name) VALUES ("LA");
INSERT INTO warehouse (name) VALUES ("NY");