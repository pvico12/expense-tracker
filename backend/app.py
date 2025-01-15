import sys
import os
from db import db_session, init_db
from flask import Flask, request, jsonify

# Create Flask app
app = Flask(__name__)

# Import Blueprint Routes
routes_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'routes')
sys.path.append(routes_dir)
from routes.test import test

# Register Blueprint Routes
app.register_blueprint(test, url_prefix='/test')


# =============== Routes ===============
@app.route('/')
def home():    
    return "Hello, World!"

@app.route('/healthcheck')
def healthcheck():    
    return jsonify({"status": "healthy"}), 200


# kill databse connection on app teardown
@app.teardown_appcontext
def shutdown_session(exception=None):
    db_session.remove()


if __name__ == '__main__':
    init_db()
    app.run(debug=True)