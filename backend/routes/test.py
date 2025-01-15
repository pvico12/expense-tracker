from flask import Blueprint
from db import db_session

test = Blueprint('test', __name__)

@test.route('/users')
def home():
    # get all users from User table
    from models import User
    users = User.query.all()
    
    return {'users': [user.name for user in users]}

