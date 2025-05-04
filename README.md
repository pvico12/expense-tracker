# Expense Tracker

Frontend: Kotlin (Jetpack Compose)
Backend: Python (FastAPI)

## Project Description

The **Expense Tracker** mobile app is designed to help users efficiently manage their personal finances. With this app, users can:

- Log and categorize their expenses.
- Set and monitor budgets for specific categories.
- View financial summaries with visualizations and insights.
- Access their data across multiple devices.

This application aims to provide a user-friendly interface and powerful features that help users manage their financial well-being.

---

## Contribution Guidelines

### Branching Strategy

- **`main`**: The production-ready branch.  
- **`develop`**: The active development branch.  
  - All feature branches should be created from `develop`.  
  - Pull Requests (PRs) must target the `develop` branch.  
  - Changes in `develop` will be tested before being merged into `main` for production deployment.  

### Frontend

- **Technology**: Android (Kotlin + Jetpack Compose)  
- **Task**: Set up frontend Android project.  
  - Refer to the [frontend README](frontend/README.md) for instructions on setting up your machine for frontend development.

### Backend

- **Technology**: Python (Flask Framework)  
- **Task**: Build and maintain the API.  
  - Refer to the [backend README](backend/README.md) for instructions on setting up your machine for backend development.

### Database

- **Technology**: PostgreSQL  
- **Task**: Set up and maintain the database.  
  - **Options for database connection**:  
    1. Local PostgreSQL Database  
    2. Remote Development Database  

  - Refer to the [backend README](backend/README.md) for setup details for both options.
