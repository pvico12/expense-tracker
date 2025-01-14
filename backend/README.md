# Expense Tracker Backend

### Local Development

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
