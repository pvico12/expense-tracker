import sys
import os

from flask import Flask, request, jsonify

# import blueprint routes
routes_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'routes')
sys.path.append(routes_dir)
from routes.test import test

app = Flask(__name__)

# register blueprint routes
app.register_blueprint(test, url_prefix='/test')


@app.route('/')
def home():    
    return "Hello, World!"

@app.route('/healthcheck')
def healthcheck():    
    return jsonify({"status": "healthy"}), 200


if __name__ == '__main__':
    app.run(debug=True)