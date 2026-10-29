import subprocess,concurrent.futures,json
D='/Users/xiangxiangwang/.docker/bin/docker';C='fay-reserve-mysql-20261029'
def sql(s):
 p=subprocess.run([D,'exec','-i',C,'mysql','-uroot','-N','-B'],input=s,text=True,capture_output=True,timeout=20)
 return p.returncode,p.stdout,p.stderr
r=sql('CREATE DATABASE qa; USE qa; CREATE TABLE quantity_allocation_mutex(warehouse_id BIGINT PRIMARY KEY) ENGINE=InnoDB; INSERT INTO quantity_allocation_mutex VALUES(1); CREATE TABLE ledger(id INT PRIMARY KEY,held INT); INSERT INTO ledger VALUES(1,0);');assert r[0]==0,r
old='USE qa; SET innodb_lock_wait_timeout=5; START TRANSACTION; INSERT IGNORE INTO quantity_allocation_mutex VALUES(1); SELECT SLEEP(0.8); SELECT warehouse_id FROM quantity_allocation_mutex WHERE warehouse_id=1 FOR UPDATE; COMMIT;'
with concurrent.futures.ThreadPoolExecutor(2) as pool:before=list(pool.map(sql,[old,old]))
assert any('Deadlock' in r[2] for r in before),before
new='USE qa; SET innodb_lock_wait_timeout=10; START TRANSACTION; INSERT INTO quantity_allocation_mutex VALUES(1) ON DUPLICATE KEY UPDATE warehouse_id=VALUES(warehouse_id); SELECT warehouse_id FROM quantity_allocation_mutex WHERE warehouse_id=1 FOR UPDATE; SELECT SLEEP(0.03); UPDATE ledger SET held=held+1 WHERE id=1 AND held<10; COMMIT;'
with concurrent.futures.ThreadPoolExecutor(8) as pool:after=list(pool.map(sql,[new]*24))
assert all(r[0]==0 for r in after),after
r=sql('USE qa; SELECT held FROM ledger WHERE id=1;');assert r[1].strip()=='10',r
# Concurrent first use of an absent mutex row must serialize too.
new=new.replace('VALUES(1) ON DUPLICATE','VALUES(2) ON DUPLICATE').replace('warehouse_id=1 FOR','warehouse_id=2 FOR')
with concurrent.futures.ThreadPoolExecutor(8) as pool:first=list(pool.map(sql,[new]*16))
assert all(r[0]==0 for r in first),first
print(json.dumps({'mysql8':True,'oldDeadlockReproduced':True,'fixedConcurrentRequests':24,'concurrentFirstUse':16,'deadlocksAfterFix':0,'held':10,'limit':10,'productionAccess':False}))
