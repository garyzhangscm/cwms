DROP TABLE IF EXISTS user_info;

CREATE TABLE user_info (
  user_id    INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(100) NOT NULL  UNIQUE,
  password  VARCHAR(100) NOT NULL,
  first_name  VARCHAR(100) NOT NULL,
  last_name  VARCHAR(100) NOT NULL,
  email  VARCHAR(100)
);

INSERT INTO user_info (username, password, first_name, last_name, email) VALUES ("GZHANG", "GZHANG", "Gary", "Zhang", "gzhang1999@gmail.com");
