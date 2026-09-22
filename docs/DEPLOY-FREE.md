# Free cloud demo deployment

QuickBite keeps the original six-service microservice architecture for the college project, but the repository also contains a single-service deployment backend for a low-cost/free demo.

## Live demo architecture
- React frontend is built into the Spring Boot application.
- Auth, restaurant, order and payment logic run inside one Spring Boot process.
- PostgreSQL is one shared database for the deployment build.
- Eureka and the API Gateway remain in the repository for the original microservice demonstration.

This avoids needing six cloud services just to show the application.

## Koyeb + PostgreSQL

Koyeb currently provides one free web service per organization and one free PostgreSQL database. The free web instance is 512 MB RAM / 0.1 vCPU and scales to zero after one hour without traffic; the free database is limited to 5 active hours per month, so this route is intended for a college demo rather than an always-on production system.

### 1. Create the PostgreSQL database
Create a Koyeb PostgreSQL Database Service and select the free instance. Copy its PostgreSQL connection string from Connection Details.

### 2. Create the web service
Create a Web Service from this GitHub repository:
- Repository: SIVA-RAMA-NAGESWARA-RAO/quickbite-delivery
- Branch: main
- Builder: Dockerfile
- Dockerfile: Dockerfile.koyeb
- Exposed port: 8080
- Instance: free

### 3. Add environment variables
- DATABASE_URL = the PostgreSQL JDBC URL, for example jdbc:postgresql://HOST/koyebdb?user=USER&password=PASSWORD&sslmode=require
- JWT_SECRET = a long random secret
- NOTIFICATIONS_EMAIL_ENABLED = false

Do not commit the real database password or JWT secret to GitHub.

### 4. Deploy
Koyeb will build Dockerfile.koyeb. That Dockerfile builds the React frontend, packages the four business services into one Spring Boot deployment application, and starts it on port 8080.

After deployment, open the public .koyeb.app URL. The frontend and API use the same origin.

## Demo limitation
The original restaurant image upload code writes to local disk. Koyeb's free service does not provide persistent volumes, so uploaded images can disappear after a redeploy/restart. For the college demo, use seeded/sample image URLs or treat uploads as a demonstration feature only.