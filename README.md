# personal-projects (Earthquake Processor API)

# Overview
Earthquake-processor-api pulls data from the UGS Earthquake public api, and stages it to a staging table. It then kicks off a spring batch job to process and enrich this raw data with nearby population information from WorldPop public api. 
TO DO: set up Databricks for reporting/visulization

# How to run locally 

Needed applications: 
- Docker
- Postman 
- Dbeaver 

Docker Setup: 
- Postgres: local database setup
  - pull latest postgres image
  - run postgres container. You will need to give it these variables: 
    - POSTGRES_USER=user
    - POSTGRES_PASSWORD=<set your own password>>
    - POSTGRES_DB=mydb
  - make sure it is running on port 5432 (5432:5432)
  - should look like this when complete: 
  ![alt text](image.png)
  - you can now connect to it in Dbeaver: 
  ![alt text](image-1.png)
  - once connected, run the following SQL command in the console:
    - CREATE SCHEMA earthquakes;

- Hashi Vault: secret storage setup
  - pull latest vault image
  - run with the following variables: 
    - VAULT_DEV_ROOT_TOKEN_ID=<set your dev root token> (NOTE: update bootstrap.yml to use this token)
  - ensure it is set to run on port 8200 (8200:8200)
  - it should look like this: 
  ![alt text](image-2.png)
  - you can now go to localhost:8200
  ![alt text](image-3.png)
  - create a new secret path (earthquakes/local) as shown above. Set "spring.datasource.password" to your database password
- Keycloak: spring oauth2 security, retrieve token to call secured endpoints
  - pull latest keycloak image (not on docker hub, use docker pull quay.io/keycloak/keycloak:21.1.1)
  - run image 
    - docker run -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:21.1.1 start-dev
  - you can now access Keycloak at localhost:8081, and login with admin credentials
![alt text](image-4.png)
  - create a realm (top left drop down)
  - create a client
    - give client-id
    - turn off temporary 
    - only authentication with client-id and client-secret
    - should look like this: 
![alt text](image-5.png)
  - create realm role
    - user.read 
  - create a user
    - set your password
    - assign realm role (user.read)
  - you can now call the application endpoints from postman, with authentication. 
    - localhost:8080 (where app runs)
    - set following variables in postman: 
      - clientId
      - username 
      - password 
      - grant_type=password

Application running: 
- run mvn clean install 
  - all tests will run
- run application 
    - liquibase should run and create all necessary tables in your local db

# Health Endpoints + Swagger 

health endpoint: localhost:8080/actuator/health 
![alt text](image-6.png)

swagger endpoint: localhost:8080/swagger-ui/index.html
  - get info on controller endpoints to stage data manually, and manually kick of spring batch processing job
![alt text](image-7.png)

# Example Runs: 

Staging table (earthquake_events):
![alt text](image-8.png)

Processed table (processed_earthquakes):
![alt text](image-9.png)

