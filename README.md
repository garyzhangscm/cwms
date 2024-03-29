# Goal
This is an onmichannel warehouse management system aimed to better assist the company to manage their warehouse in a easy, simple, efficient and modern way.
It is built in the mind of modern warehouse management philosophy
- High Visibility
  - Management Dashboard to show overall status
  - Dashboard to show progress of each process
  - 3PL visibility for each customer
- System driven process: System calculate the best candidate for pick, movement, put away location, inventory consolidation, etc and drive the floor worker to complete the tasks in sequence
  - Higher picking efficiency
  - Higher location utilization
  - Lower laber requirement
- Tracibility: System should record all the activities for tracibility
  - You can get KPI information out of the history data
  - Spot the error when there's inventory discrepancy
  - Analysis the data to find places for improvement

# System Infrastructure
The system is designed based on the Micro-Service methodology. It is built with the following technical
- Front-end and Back-end Separation
  - Front End: Angular based web client, host by NGINX
  - Back End: Spring boot
- Containerization with Docker, host by Kubernetes
 
### An Example of hosting on AWS

![aws-host-CWMS](https://github.com/garyzhangscm/cwms/assets/24829203/023f1be8-6de3-4a46-af86-582175c3d9d6)

# Build Your Own WMS
You can check out all docker container from https://hub.docker.com/repositories/garyzhangscm and build system on your own kubernetes environment, or AWS.

You can check out the database script and kubernetes yaml file from https://github.com/garyzhangscm/cwms-setup 
- yaml: the 3 yaml file to setup the server
- db: please download the v1.15 and then run all upgrade script to match with the server version that specified in the yaml file


Sample yaml files(WMS V1.15) is provided.
- framework.yaml: Inlucdes ZUUL, Kafka & ZooKeeper, Database, Adminer(web client for the database), Redis
- frontend-app.yaml: Front End Web client HOST by NGINX
- backend-app.yaml: Back End micro-service. please change the APP keys/tokens if you will need to use google / USPS / quickbook / shipEngine API
you can always upgrade by downloading new docker container version. just make sure all container's version match and match with the database's version

if you have any question, please contact garyzhangscm@gmail.com

# Test Environment
Claytech Software LLC has a test environment that host on AWS. please email aws@claytechs.com if you just want to try the software but don't want to install the environment
