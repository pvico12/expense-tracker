from flask import Blueprint, request, jsonify
from db import db_session
from http_models import RegistrationRequest, LoginRequest
from models import User
import utils

authRouter = Blueprint('auth', __name__)

@authRouter.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    registrationRequest = RegistrationRequest()
    errors = registrationRequest.validate(data)
    if errors:
        return jsonify(errors), 400

    hashed_password = utils.hash_password(data['password'])
    new_user = User(
        username=data['username'],
        password=hashed_password,
        firstname=data['firstname'],
        lastname=data['lastname']
    )
    db_session.add(new_user)
    db_session.commit()

    return jsonify({'message': 'User created successfully'}), 201

@authRouter.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    loginRequest = LoginRequest()
    errors = loginRequest.validate(data)
    if errors:
        return jsonify(errors), 400

    username = data.get('username')
    password = data.get('password')

    user = db_session.query(User).filter_by(username=username).first()
    if not user or user.password != utils.hash_password(password):
        return jsonify({'error': 'Invalid username or password'}), 401

    return jsonify({'message': 'Login successful'}), 200