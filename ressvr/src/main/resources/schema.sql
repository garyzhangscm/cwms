-- Menu / Menu Sub Group / Menu Group

DROP TABLE IF EXISTS menu;
DROP TABLE IF EXISTS menu_sub_group;
DROP TABLE IF EXISTS menu_group;
CREATE TABLE menu_group (
  menu_group_id    INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  text   VARCHAR(100) NOT NULL  UNIQUE,
  i18n  VARCHAR(100) NOT NULL,
  group_flag boolean not null default 1,
  hide_in_breadcrumb boolean not null default 0,
  sequence int not null default 0
);
INSERT INTO menu_group (menu_group_id, text, i18n, group_flag, hide_in_breadcrumb, sequence) values (1, "Main", "menu.main", true, true, 0);
INSERT INTO menu_group (menu_group_id, text, i18n, group_flag, hide_in_breadcrumb, sequence) values (2, "Alain", "menu.alain", true, true, 1);
INSERT INTO menu_group (menu_group_id, text, i18n, group_flag, hide_in_breadcrumb, sequence) values (3, "Pro", "menu.pro", true, true, 2);
INSERT INTO menu_group (menu_group_id, text, i18n, group_flag, hide_in_breadcrumb, sequence) values (4, "More", "menu.more", true, true, 3);


CREATE TABLE menu_sub_group (
  menu_sub_group_id    INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  text   VARCHAR(100) NOT NULL  UNIQUE,
  i18n  VARCHAR(100) NOT NULL,
  icon  VARCHAR(100) NOT NULL,
  shortcut_root  boolean,
  menu_group_id int not null,
  sequence int not null default 0,
  link VARCHAR(100),
  badge int,
  foreign key(menu_group_id) references menu_group(menu_group_id)
);
INSERT INTO menu_sub_group (menu_sub_group_id, text, i18n, icon, menu_group_id, sequence) values (1, "Dashboard", "menu.dashboard", "anticon-dashboard", 1, 0);
INSERT INTO menu_sub_group (menu_sub_group_id, text, i18n, icon, menu_group_id, sequence, shortcut_root) values (2, "Shortcut", "menu.shortcut", "anticon-rocket", 1, 1, 1);
INSERT INTO menu_sub_group (menu_sub_group_id, text, i18n, icon, menu_group_id, sequence, link, badge) values (3, "Widgets", "menu.widgets", "anticon-appstore", 1, 2, "/widgets", 2);


CREATE TABLE menu (
  menu_id    INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  text   VARCHAR(100) NOT NULL  UNIQUE,
  i18n  VARCHAR(100) NOT NULL,
  link VARCHAR(100) NOT NULL,
  menu_sub_group_id int not null,
  sequence int not null default 0,
  foreign key(menu_sub_group_id) references menu_sub_group(menu_sub_group_id)
);
INSERT INTO menu (menu_id, text, link, i18n,  menu_sub_group_id, sequence) values (1, "DashboardV1", "/dashboard/v1", "menu.dashboard.v1", 1, 0);
INSERT INTO menu (menu_id, text, link, i18n,  menu_sub_group_id, sequence) values (2, "Analysis", "/dashboard/analysis", "menu.dashboard.analysis", 1, 1);
INSERT INTO menu (menu_id, text, link, i18n,  menu_sub_group_id, sequence) values (3, "Monitor", "/dashboard/monitor", "menu.dashboard.monitor", 1, 2);
INSERT INTO menu (menu_id, text, link, i18n,  menu_sub_group_id, sequence) values (4, "Workplace", "/dashboard/workplace", "menu.dashboard.workplace", 1, 3);
