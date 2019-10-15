DROP TABLE IF EXISTS user_info;

CREATE TABLE user_info (
  user_id    INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(100) NOT NULL  UNIQUE,
  password  VARCHAR(100) NOT NULL,
  first_name  VARCHAR(100) NOT NULL,
  last_name  VARCHAR(100) NOT NULL,
  email  VARCHAR(100),
  enabled boolean not null default 0,
  locked boolean not null default 0
);

INSERT INTO user_info (username, password, first_name, last_name, email, enabled, locked) VALUES ("GZHANG", "GZHANG", "Gary", "Zhang", "gzhang1999@gmail.com", 1, 0);

INSERT INTO user_info (username, password, first_name, last_name, email, enabled, locked) VALUES ("RWU", "RWU", "Rainbow", "Wu", "rwu@gmail.com", 1, 0);

INSERT INTO user_info (username, password, first_name, last_name, email, enabled, locked) VALUES ("OZHANG", "OZHANG", "Olivia", "Zhang", "ozhang@gmail.com", 1, 0);

INSERT INTO user_info (username, password, first_name, last_name, email, enabled, locked) VALUES ("JZHANG", "JZHANG", "Japser", "Zhang", "ozhang@gmail.com", 1, 0);