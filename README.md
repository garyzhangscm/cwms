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
An Example of hosting on AWS

