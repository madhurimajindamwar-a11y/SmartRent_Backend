# SmartRent Backend

REST API for the SmartRent room-rental application.

## Technology

Java 21, Spring Boot, Spring Security, Spring Data JPA, and MySQL.

The Maven application is inside the smartrent directory.

## Implemented features

- Tenant and owner registration with BCrypt password hashing.
- JWT authentication and role-based authorization.
- Owner listing creation, editing, availability controls, and deletion.
- Public discovery of verified, available listings.
- Admin listing verification and rejection.
- Property image storage and retrieval.
- Favorites.
- Preference-based property matching.
- Rental request submission, cancellation, acceptance, and rejection.
- Active rental creation and availability updates.
- Ownership checks and conflict handling in service methods.

Property image changes and listing-detail changes require admin review again.

## Database setup

Create a MySQL database named smartrent.

Import the existing SmartRent schema before starting the application. The application uses:

    spring.jpa.hibernate.ddl-auto=validate

It validates the schema; it does not create missing tables.

Current tables:

- users
- properties
- property_images
- favorites
- rental_requests
- rentals

A database export containing account data or photos must not be committed to GitHub.

## Environment variables

- DB_URL: JDBC connection URL. Defaults to jdbc:mysql://localhost:3306/smartrent.
- DB_USERNAME: MySQL username.
- DB_PASSWORD: MySQL password.
- JWT_SECRET: Base64-encoded random signing key containing at least 32 bytes.
- PORT: HTTP listening port. Defaults to 8081.

Generate and store secrets privately. Never commit them or place them in frontend environment variables.

## Local run

Open PowerShell and enter the smartrent directory:

    cd smartrent

Set the environment variables in the same terminal, then run:

    .\mvnw.cmd spring-boot:run

Keep the terminal open while using the local frontend.

## Build and tests

Build the application:

    .\mvnw.cmd package

Tests may require a configured database and environment variables.

Earlier packaging used -DskipTests. A successful package with that flag does not verify the tests.

## Main API routes

Authentication:

- POST /api/auth/register
- POST /api/auth/login

Properties:

- GET /api/properties
- GET /api/properties/search
- GET /api/properties/{id}
- POST /api/properties
- PUT /api/properties/{id}
- DELETE /api/properties/{id}
- GET /api/owner/properties

Photos:

- GET /api/properties/{propertyId}/images
- GET /api/properties/{propertyId}/images/{imageId}/content
- GET /api/manage/properties/{propertyId}/images
- GET /api/manage/properties/{propertyId}/images/{imageId}/content
- POST /api/manage/properties/{propertyId}/images
- DELETE /api/manage/properties/{propertyId}/images/{imageId}

Rental requests and rentals:

- POST /api/rental-requests
- GET /api/me/rental-requests
- GET /api/owner/rental-requests
- PATCH /api/rental-requests/{id}/status
- PATCH /api/rental-requests/{id}/cancel
- GET /api/me/rentals
- GET /api/owner/rentals

Matching:

- POST /api/matches

Protected requests use:

    Authorization: Bearer <access-token>

## Matching model

Only supplied preferences contribute to the score:

- Maximum budget: 40
- Locality: 30
- Room type: 20
- Requested amenities collectively: 10

Maximum budget is a strict candidate filter. Results are ranked by normalized match percentage, then rent and property ID.

## Production deployment

Hosted on Railway with MySQL in the same project.

Backend service root directory:

    /smartrent

Backend URL:

https://smartrentbackend-production.up.railway.app

The deployed backend currently listens on port 8080. Its public domain target port must match the actual listening port.

The database schema and data were imported into the Railway database. The exposed database password was rotated and backend reconnection was verified.

Vercel proxies frontend /api requests to this backend.

## Known scope limits

This version treats each listing as a single rentable unit. It does not manage multiple room inventories, visits, payments, reviews, maps, password change/reset, notifications, or advanced admin management.

## Frontend

Application: https://smart-rent-frontend.vercel.app/

Repository: https://github.com/madhurimajindamwar-a11y/SmartRent_Frontend


## Profile endpoints

- GET /api/me/profile
- PUT /api/me/profile

Profile editing requires the signed-in user's current password.
Only name and email are editable; account ID and role are not editable.

## Maintenance setup and endpoints

The maintenance feature adds the maintenance_requests table.

After importing the base schema, apply:

    smartrent/database/maintenance.sql

Apply it manually to each database before running the updated backend.
The application still uses ddl-auto=validate.

Endpoints:

- GET /api/maintenance
- POST /api/maintenance
- PATCH /api/maintenance/{id}/status

Tenants may submit issues only for their own active rental.
Owners may update issues only for properties they own.
Status transitions are OPEN → IN_PROGRESS → RESOLVED.
Unresolved requests with the same rental and title are blocked.

Submission and the full owner-to-tenant status workflow were verified
manually on the deployed application. Broader automated and permission
testing remains pending.

## Reviews and notifications API

Public:
- GET /api/properties/{propertyId}/reviews

Authenticated tenant:
- GET /api/reviews/properties/{propertyId}/mine
- PUT /api/reviews/properties/{propertyId}
  Body: {"rating":4,"comment":"Your rental experience"}

Authenticated account:
- GET /api/notifications
- PATCH /api/notifications/{id}/read
- PATCH /api/notifications/read-all

Reviews require a qualifying rental record and are limited to one per tenant/property. Notification queries and read updates are restricted to the current account.

Database setup:
Run smartrent/database/reviews-notifications.sql after creating the existing users and properties tables, and before starting the updated backend. Hibernate validates the schema; it does not create these tables. This script contains table definitions only, without credentials or user data.

Notification creation participates in the rental or maintenance transaction. Failed business actions do not commit notifications.
