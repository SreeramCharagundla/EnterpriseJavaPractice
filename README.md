# EnterpriseJavaPractice

Order Management System — EJB + JMS + ActiveMQ Artemis + WildFly + SQL Server

This repository is a complete, working enterprise-style Java EE / Jakarta EE example demonstrating EJBs, MDBs, JMS messaging, JPA persistence, and a small frontend to exercise the REST API.

## Table of contents

- [Quick summary](#quick-summary)
- [Architecture overview](#architecture-overview)
- [Project structure](#project-structure)
- [Technologies](#technologies)
- [Database schema](#database-schema)
- [Setup](#setup)
  - [Run SQL Server (Docker)](#run-sql-server-docker)
  - [WildFly configuration](#wildfly-configuration)
- [Build](#build)
- [Deploy](#deploy)
- [REST API](#rest-api)
- [Frontend](#frontend)
- [JMS / MDB processing](#jms--mdb-processing)
- [Testing and tools](#testing-and-tools)
- [Future enhancements](#future-enhancements)
- [Author](#author)

## Quick summary

- The web module (`ordermanagement-web`) exposes a JAX-RS API and a simple HTML/JS UI.
- The EJB module (`ordermanagement-ejb`) contains business logic, a JMS message producer, JPA entities, and the MDB consumer.
- An EAR module (`ordermanagement-ear`) packages the application for WildFly deployment.

## Architecture overview

1. Client submits an order via the frontend.
2. REST endpoint (stateless) persists the order and enqueues its ID on a JMS queue (`OrderQueue`).
3. A Message-Driven Bean (MDB) listens on the queue, processes orders asynchronously, and updates the order status and processed timestamp.

## Project structure

Top-level modules (Maven multi-module):

- `ordermanagement-ejb/` — Business logic, JMS producer, MDB, JPA entities
- `ordermanagement-web/` — REST API and frontend (static HTML + JS)
- `ordermanagement-ear/` — EAR packaging for WildFly deployment
- `pom.xml` — Parent POM

## Technologies

- Java 17 (Temurin)
- Jakarta EE 10 (EJB, JAX-RS, JPA)
- JMS 2.0 with ActiveMQ Artemis (WildFly's messaging)
- WildFly 38
- JPA (Hibernate)
- SQL Server (JDBC driver)
- Maven 3 (build)

## Database schema

Database: `OrderManagement`

Table: `Orders`

| Column        | Type         | Notes                                   |
| ------------- | ------------ | --------------------------------------- |
| id            | BIGINT       | PK, IDENTITY (auto-increment)           |
| customer_name | VARCHAR(100) |                                         |
| product_name  | VARCHAR(100) |                                         |
| quantity      | INT          |                                         |
| status        | VARCHAR(20)  | NEW / PROCESSED / CANCELLED / ERROR_JMS |
| created_at    | DATETIME2    | default SYSUTCDATETIME()                |
| processed_at  | DATETIME2    | NULL, set by MDB on processing          |

## Setup

Prerequisites

- Docker (for SQL Server) or a running SQL Server instance
- WildFly (standalone full profile for JMS and full Jakarta EE features)

### Run SQL Server (Docker)

Example (Azure SQL Edge):

```bash
docker run -e "ACCEPT_EULA=Y" \
   -e "SA_PASSWORD=YourStrong!Passw0rd1" \
   -p 1401:1433 \
   --name sqlserver2022 \
   -d mcr.microsoft.com/azure-sql-edge
```

Create the database and table (run these statements in your SQL client):

```sql
CREATE DATABASE OrderManagement;
GO
USE OrderManagement;

CREATE TABLE Orders (
   id BIGINT IDENTITY(1,1) PRIMARY KEY,
   customer_name VARCHAR(100),
   product_name VARCHAR(100),
   quantity INT,
   status VARCHAR(20),
   created_at DATETIME2 DEFAULT SYSUTCDATETIME(),
   processed_at DATETIME2 NULL
);
```

### WildFly configuration

Start WildFly with the full profile (example):

```bash
./bin/standalone.sh -c standalone-full.xml
```

Install the Microsoft SQL Server JDBC driver as a module (adjust path/version):

```bash
./bin/jboss-cli.sh --connect --command="module add \
   --name=com.microsoft.sqlserver \
   --resources=/path/to/mssql-jdbc-12.6.1.jre17.jar \
   --dependencies=javax.api,javax.transaction.api"
```

Add the driver and a datasource via CLI or the admin console. Example CLI snippets (adapt line breaks):

```cli
/subsystem=datasources/jdbc-driver=mssql:add(
   driver-name=mssql,
   driver-module-name=com.microsoft.sqlserver,
   driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
)

/subsystem=datasources/data-source=OrderDS:add(
   jndi-name=java:/jdbc/OrderDS,
   driver-name=mssql,
   connection-url="jdbc:sqlserver://localhost:1401;databaseName=OrderManagement;encrypt=false;trustServerCertificate=true",
   user-name=sa,
   password=YourStrong!Passw0rd1,
   enabled=true
)

/subsystem=messaging-activemq/server=default/jms-queue=OrderQueue:add(entries=["java:/jms/queue/OrderQueue"])
```

Ensure the JNDI names and credentials match the `persistence.xml` and code configuration in the repo.

## Build

From the project root run:

```bash
mvn clean install
```

This produces an EAR artifact under:

`ordermanagement-ear/target/ordermanagement-ear-0.0.1-SNAPSHOT.ear`

## Deploy

Open the WildFly admin console at `http://localhost:9990` and upload the EAR under Deployments, or use the management CLI to deploy the generated EAR.

## REST API

Base path (example, adjust context path if different):

`http://localhost:8080/ordermanagement-web/api`

Endpoints

- GET /orders — Get all orders
- GET /orders/{id} — Get order by ID
- POST /orders — Create order (JSON)

Example create payload:

```json
{
  "customerName": "John Doe",
  "productName": "Laptop",
  "quantity": 2
}
```

- PUT /orders/{id} — Update order (JSON)
- DELETE /orders/{id} — Delete order

## Frontend

The simple frontend lives in:

- `ordermanagement-web/src/main/webapp/index.html`
- `ordermanagement-web/src/main/webapp/js/app.js`

It provides an order creation form and a table showing orders (ID, customer, product, quantity, status, created_at, processed_at).

## JMS / MDB processing

- New orders are enqueued to `java:/jms/queue/OrderQueue`.
- `OrderProcessingMDB` listens to the queue and processes orders asynchronously.
- On success MDB sets `status = PROCESSED` and `processedAt = LocalDateTime.now()`.

## Testing and tools

- cURL example:

```bash
curl -i http://localhost:8080/ordermanagement-web/api/orders
```

- A Postman collection (if available) can be used to exercise CRUD operations.

## Future enhancements

- Order search + filtering
- Customer management module
- Dashboard charts and analytics
- WebSocket live updates
- Authentication & JWT security
- Docker Compose for the full stack (WildFly + SQL Server + Artemis)

## Author

Sreeram Charagundla — Java | Distributed Systems | Microservices | Message-Driven Architectures
