-- User

DROP TABLE IF EXISTS user_auth;

CREATE TABLE user_auth (
  user_auth_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(100) NOT NULL  UNIQUE,
  password  VARCHAR(100) NOT NULL,
  enabled boolean not null default 0,
  locked boolean not null default 0,
  email  VARCHAR(100)
);

INSERT INTO user_auth (username, password, enabled, locked, email) VALUES ("GZHANG", "GZHANG",1, 0, "gzhang@gmail.com");

INSERT INTO user_auth (username, password, enabled, locked, email) VALUES ("RWU", "RWU", 1, 0, "rwu@gmail.com");

INSERT INTO user_auth (username, password, enabled, locked, email) VALUES ("OZHANG", "OZHANG", 1, 0, "ozhang@gmail.com");

INSERT INTO user_auth (username, password, enabled, locked, email) VALUES ("JZHANG", "JZHANG", 1, 0 , "jzhang@gmail.com");

