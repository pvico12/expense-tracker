# Expense Tracker Backend

## Contribution

### Backend

#### Setup

**Note**: These instructions work for Linux and WSL2, they vary slightly for other operating systems.

Firstly, create a virtual environment with the following command
`python3 -m venv backend-venv`

Then, activate the virtual environment
`source backend-venv/bin/activate`

Finally, install all requirements from the `requirements.txt` file. (venv must be activated)
`pip install -r requirements.txt`

#### Development & Testing

To run the Flask application locally in development mode (hot reload is enabled), use the following command:
`python3 app.py`

For database connection, create a `.env` file in this folder with the following variable:

```
DATABASE_URI=postgresql://<username>:<password>@<database-ip-address>:5432/<database-name>
```

### Database

For the database, we are using Postgres. 

##### Installation

Follow this [guide](https://www.prisma.io/dataguide/postgresql/setting-up-a-local-postgresql-database) (instructions given for all operating systems)

##### Configuration

Once you have Postgres installed on your own machine, it is time to configure it for connection with the Flask backend.

1. Open Postgres CLI (**psql**)
   **Windows**: Open Start Menu, search for **psql**, click to run.
   **MacOS**: Assuming the **psql** executable is in your PATH as described in the above article, run `psql -u postgres`
   **MacOS**: If you are using Homebrew, you can run `brew services start postgresql` to start the server. then run `psql postgres`
   **Linux/WSL2**: Run `sudo -u postgres psql`

2. Create a new user

```
CREATE USER expense_tracker_admin WITH PASSWORD '<some-password>';
```

3. Create a database

```
CREATE DATABASE "expense-tracker";
```

4. Grant user access to database

```
GRANT ALL PRIVILEGES ON DATABASE "expense-tracker" TO expense_tracker_admin;
\c "expense-tracker";
GRANT ALL ON SCHEMA public TO expense_tracker_admin;
```

5. Generate your database connection string using your newly created user. It should look something like this.

```
postgresql://expense_tracker_admin:<your-newly-created-password>@localhost:5432/expense-tracker
```

6. Create a .env file in the backend folder and add this line:

```
postgresql://expense_tracker_admin:<your-newly-created-password>@localhost:5432/expense-tracker
``

