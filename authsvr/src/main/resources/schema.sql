-- User

DROP TABLE IF EXISTS user_auth;

CREATE TABLE user_auth (
  user_auth_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  username   VARCHAR(100) NOT NULL ,
  password  VARCHAR(100) NOT NULL,
  enabled boolean not null default 0,
  locked boolean not null default 0,
  email  VARCHAR(100)
);

INSERT INTO user_auth (company_id, username, password, enabled, locked, email) VALUES (1, "GZHANG", "GZHANG",1, 0, "gzhang@gmail.com");

INSERT INTO user_auth (company_id, username, password, enabled, locked, email) VALUES (1, "RWU", "RWU", 1, 0, "rwu@gmail.com");

INSERT INTO user_auth (company_id, username, password, enabled, locked, email) VALUES (1, "OZHANG", "OZHANG", 1, 0, "ozhang@gmail.com");

INSERT INTO user_auth (company_id, username, password, enabled, locked, email) VALUES (1, "JZHANG", "JZHANG", 1, 0 , "jzhang@gmail.com");

INSERT INTO user_auth (company_id, username, password, enabled, locked, email) VALUES (2, "GZHANG", "GZHANG",1, 0, "gzhang@gmail.com");
INSERT INTO user_auth (company_id, username, password, enabled, locked, email) VALUES (2, "JYEH", "JYEH", 1, 0 , "jyeh@gmail.com");

