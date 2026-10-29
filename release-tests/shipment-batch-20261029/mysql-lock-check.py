import subprocess,time,concurrent.futures,json
D='/Users/xiangxiangwang/.docker/bin/docker';C='shipment-lock-mysql-20261029'
def sql(s):
 p=subprocess.run([D,'exec','-i',C,'mysql','-uroot','-N','-B'],input=s,text=True,capture_output=True,timeout=15);return p.returncode,p.stdout,p.stderr
for n in range(45):
 r=sql('SELECT @@port;')
 if r[0]==0 and r[1].strip()=='3306':break
 time.sleep(1)
else:raise RuntimeError('Isolated MySQL did not start')
r=sql('DROP DATABASE IF EXISTS qa;CREATE DATABASE qa;USE qa;CREATE TABLE item(id INT PRIMARY KEY) ENGINE=InnoDB;CREATE TABLE inv_status(id INT PRIMARY KEY) ENGINE=InnoDB;CREATE TABLE inventory(id INT PRIMARY KEY,item_id INT,status_id INT,FOREIGN KEY(item_id) REFERENCES item(id),FOREIGN KEY(status_id) REFERENCES inv_status(id)) ENGINE=InnoDB;INSERT INTO item VALUES(1);INSERT INTO inv_status VALUES(1);INSERT INTO inventory VALUES(1,1,1);');assert r[0]==0,r
prefix='USE qa;SET innodb_lock_wait_timeout=5;START TRANSACTION;'
a=prefix+'SELECT id FROM item WHERE id=1 FOR SHARE;SELECT SLEEP(0.6);INSERT INTO inventory VALUES(2,1,1);COMMIT;'
b=prefix+'SELECT SLEEP(0.1);SELECT id FROM inv_status WHERE id=1 FOR UPDATE;SELECT id FROM item WHERE id=1 FOR UPDATE;COMMIT;'
with concurrent.futures.ThreadPoolExecutor(2) as pool:before=list(pool.map(sql,[a,b]))
assert any('Deadlock' in r[2] for r in before),before
for n in range(6):
 a=prefix+f'SELECT id FROM item WHERE id=1 FOR SHARE;SELECT SLEEP(0.25);INSERT INTO inventory VALUES({n+10},1,1);COMMIT;'
 b=prefix+'SELECT inventory.id FROM inventory WHERE id=1 FOR UPDATE;SELECT i.id,s.id FROM inventory v JOIN item i ON i.id=v.item_id JOIN inv_status s ON s.id=v.status_id WHERE v.id=1;SELECT SLEEP(0.4);COMMIT;'
 with concurrent.futures.ThreadPoolExecutor(2) as pool:after=list(pool.map(sql,[a,b]))
 assert all(r[0]==0 for r in after),after
print(json.dumps({'oldSharedMasterRowDeadlockReproduced':True,'fixedConcurrentPairs':6,'deadlocksAfterNarrowRowLock':0,'productionAccess':False}))
