# BankCLI

A console-based banking application built with Java and PostgreSQL, demonstrating a layered architecture (API → Service → Persistence → Domain) with the DAO design pattern.

## Tech Stack

- Java 17+
- Maven
- PostgreSQL (JDBC via the official `org.postgresql:postgresql` driver)

## Database Schema

```sql
CREATE TABLE IF NOT EXISTS account (
    account_id SERIAL PRIMARY KEY,
    pin VARCHAR(255) NOT NULL,
    balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS transaction (
    transaction_id SERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES account(account_id),
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    related_account_id INTEGER REFERENCES account(account_id),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Tables are created automatically on startup if they don't already exist — no manual schema setup required, as long as the target database itself exists (see Setup below).

## Setup

### 1. Prerequisites

- Java 17+ and Maven installed
- PostgreSQL running locally (or accessible via a host/port you control)

### 2. Create the database

```bash
psql -U your_username -c "CREATE DATABASE bank;"
```

### 3. Configure credentials

Copy the example config and fill in your own values:

```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
```

Edit `db.properties`:

```properties
DB_URL=jdbc:postgresql://localhost:5432/bank
DB_USER=your_username
DB_PASSWORD=your_password
```

> `db.properties` is git-ignored and should never be committed — it contains real credentials.

### 4. Build and run

```bash
mvn clean install
mvn exec:java -Dexec.mainClass="com.bankcli.api.Main"
```

Or run `Main.java` directly from your IDE.

## Usage

On startup, you'll see a welcome menu:

```
1. Login
2. Create new account
3. Exit
```

Create an account first to get an account ID, then log in with that ID and PIN to access the main menu:

```
1. Check balance
2. Deposit
3. Withdraw
4. Transfer
5. Transaction history
6. Logout / Exit
```
