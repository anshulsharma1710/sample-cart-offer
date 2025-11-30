# Cart Offer Engine – SDET Assignment

This project implements a **Cart Offer Engine** with complete **integration tests**, a **MockServer setup**, and a **test-case suite** as required for the SDET assignment.  
The service allows creating offers, fetching user segments through MockServer, and applying the most suitable discount to a shopping cart.

---

## 🚀 Tech Stack

- **Java 17+** (tested on Java 19)
- **Spring Boot 2.6.14**
- **Maven**
- **Docker + MockServer** (stubbing external user-segment API)
- **JUnit 5** with **TestRestTemplate**
- **Docker Compose** for mock server setup

---

## 📂 Project Structure

sample-cart-offer/
│── src/main/java/com/springboot/ # Application logic
│── src/test/java/com/springboot/ # Integration tests
│ ├── OfferIntegrationTests.java # Full integration test suite (10 tests)
│── mockserver/
│ └── docker-compose.yml # MockServer setup
│── test-cases.xlsx # Fully documented 15 test cases
│── pom.xml
└── README.md

---

# ▶️ How to Run the Project

## 1️⃣ Prerequisites

Make sure you have:

- Java **17 or above**
- Maven
- Docker Desktop (for MockServer)
- Internet (first Maven build)

Verify versions:

```bash
java -version
mvn -version
docker --version

2️⃣ Start the Application

From the project root:
mvn spring-boot:run -DskipTests
Application runs on:

👉 http://localhost:9001

(Tests use a random port, so no conflicts.)

🧩 MockServer Setup (Required for Tests)

Navigate to the MockServer folder:
cd mockserver
docker-compose up -d

MockServer runs on:

Port 1080 → API stubs

Management endpoint: http://localhost:1080/mockserver/expectation
Verify:
curl http://localhost:1080

🧪 Running Tests (Integration Tests)

Go to project root and run:
mvn -U test

What happens?

Each test:

- Starts Spring Boot on a RANDOM_PORT

- Stubs user segments via MockServer

- Adds offers

- Applies offers

- Asserts cart discount results

- Run a specific test:

mvn -Dtest=OfferIntegrationTests -U test

📘 Test Cases Documentation

All 15 test scenarios are documented in:
test-cases.xlsx

Each test case contains:

- Test ID

- Preconditions

- Steps

- Test Data

- Expected Result

- Priority

- Notes

- Automated/Manual indication

🧪 Automated Test Coverage Summary

The following scenarios are automated:

Test Case	Scenario
TC-01	    FLAT amount discount
TC-02	    FLAT percent discount
TC-03	    No segment returned
TC-04	    Segment mismatch
TC-05	    Multiple offers (best/first)
TC-06	    Zero-value offer
TC-07	    Invalid offer type
TC-08	    100% discount
TC-09	    Rounding rules
TC-10	    Offer update lifecycle

All automated tests are located in:
src/test/java/com/springboot/OfferIntegrationTests.java

🛠️ Useful Commands
Start the app:
mvn spring-boot:run -DskipTests

Start MockServer:
cd mockserver
docker-compose up -d

Stop MockServer:
cd mockserver
docker-compose down

Clean and rebuild:
mvn clean install


📦 Deliverables Included

✔ Fully working Spring Boot application

✔ MockServer (Docker-based)

✔ 10 automated integration tests

✔ test-cases.xlsx (15 detailed test cases)

✔ Complete README.md

📌 Notes

Tests require MockServer running before execution.

Some behaviors (e.g., invalid offer type, multi-offer selection) may vary by implementation; tests accept both valid outcomes and document them clearly.

Offers and segments are test-isolated using unique IDs.

🙌 Author

Ayush Sharma
SDET Assignment – 2025

---

If you want:

✅ A **prettier README** with badges (Java, Docker, CI)  
✅ Screenshots section  
✅ Setup scripts for Windows/Mac/Linux  
✅ A short **demo GIF** of running tests  

Just tell me — happy to generate it!
