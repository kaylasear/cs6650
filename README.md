# Ski Resort System

## Tech Stack

* Tomcat Apache 9
* Java 17
* RabbitMQ 5.14
* Redis
* AWS EC2
* AWS Elastic Load-Balancer

## Usage

Scalable distributed cloud-based system that recorded ski lift rides from a mock-up ski resort company.

## Features
- Created a multi-threaded HTTP client that sent 160,000 POST requests using AWS EC2 instances and elastic load-balancer with a throughput of ~1000 requests / second for high-availability.
  - Endpoint: "/skiers/{resortId}/seasons/{seasonId}/days/{dayId}/skiers/{skierId}"
- Implemented a backend system to persist data to a Redis database
  - SkierServlet.java
      - doPost() - Send skier object to database
      - GET/skiers/{skierID}/vertical - Get the ski day vertical for a skier for the specified ski day
      - GET/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
  - ResortServlet.java
      - doPost() - Send a new season for a resort
      - doGet() - Get a list of ski resorts in the database
      - GET/resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers - Get total number of skiers at specified resort, season, day
      - GET/resorts/{resortID}/seasons/ - Get seasons by Resort Id
  - Resort
- Implemented a producer and consumer pattern executed with RabbitMQ

## Project Status

**Finished**


## Credits

**Curriculum from :** <br />
CS6650 Building Scalable Distributed Systems Spring 2022
<br> Professor Ian Gorton
<br><br>
[Northeastern University Khoury College of Computer Science](https://www.khoury.northeastern.edu/)


## License
**Copyright &copy; :** 2022 Kayla Sear

