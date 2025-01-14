from flask import Blueprint

test = Blueprint('test', __name__)

@test.route('/')
def home():
    return "Hello, World! - Test Endpoint"

