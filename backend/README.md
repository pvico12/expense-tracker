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
For the database, we are using Postgres. Here you have 2 options:
1. Connect to a local Postgres instance on your machine
2. Connect to our remote development database hosted in the cloud using UW CSC infrastructure

#### Local
In this section, we will be setting up a local Postgres instance. Using this instance, we will create a connection from the Flask backend to our local instance.

##### Installation
Follow this [guide](https://www.prisma.io/dataguide/postgresql/setting-up-a-local-postgresql-database) (instructions given for all operating systems)

##### Configuration
Once you have Postgres installed on your own machine, it is time to configure it for connection with the Flask backend.

1. Open Postgres CLI (**psql**)
**Windows**: Open Start Menu, search for **psql**, click to run.
**MacOS**: Assuming the **psql** executable is in your PATH as described in the above article, run `psql -u postgres`
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
\c expense-tracker postgres
GRANT ALL ON SCHEMA public TO expense_tracker_admin;
```

5. Generate your database connection string using your newly created user. It should look something like this.
```
postgresql://expense_tracker_admin:<your-newly-created-password>@localhost:5432/expense-tracker
```

6. Create a .env file in the backend folder and add this line:
```
postgresql://expense_tracker_admin:<your-newly-created-password>@localhost:5432/expense-tracker
```


#### Remote
In this section, we will be connecting to a remote Postgres instance which is hosted on the UW CSC infrastructure associated with Petar Vico's instances.

Since this database server is hosted within the UW network, you need to be connected to the UW network.

If you are on campus, perfect, you don't need to do anything special.

Otherwise, you need to use the UW Campus VPN to remote into the UW networks.

##### On-Campus
You're good, move onto the Database Connection section below.

##### Off-Campus
Refer to the following instructions to set up the Campus VPN [here](https://uwaterloo.atlassian.net/wiki/spaces/ISTKB/pages/262012980/Virtual+Private+Network+VPN).
More specifically:
- [MacOS Install Guide](https://uwaterloo.atlassian.net/wiki/spaces/ISTKB/pages/262012942/How+to+install+and+connect+to+the+VPN+-+Mac+OS)
- [Windows Install Guide](https://uwaterloo.atlassian.net/wiki/spaces/ISTKB/pages/262012949/How+to+Install+AnyConnect+and+Connect+to+the+VPN+-+Windows+OS)
- [Linux Install Guide](https://uwaterloo.atlassian.net/wiki/spaces/ISTKB/pages/262012938/How+to+install+and+connect+to+the+VPN+-Linux+Ubuntu)
**Tip**: If you are confused what 2nd password is, its basically your preffered 2FA method. If you write "push" it will use Duo mobile as a 2nd factor authentication to log you in. The video tutorials are also good if you get stuck.

##### Database Connection
Now that you are within UW networks, contact Petar for the database connection string, and replace your DATABASE_URL variable in the .env file you created with the one Petar provides you.
