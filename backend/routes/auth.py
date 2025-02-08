import os
from dotenv import load_dotenv
from flask import Blueprint, request, jsonify
import jwt
from db import db_session
from http_models import RegistrationRequest, LoginRequest, TokenRefreshRequest
from models import User
import utils
import datetime

load_dotenv()
JWT_ACCESS_TOKEN_SECRET = os.getenv('JWT_ACCESS_TOKEN_SECRET')
JWT_REFRESH_TOKEN_SECRET = os.getenv('JWT_REFRESH_TOKEN_SECRET')

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
    
    access_token = jwt.encode({
        'user_id': user.id,
        'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(minutes=10)
    }, JWT_ACCESS_TOKEN_SECRET, algorithm='HS256')

    refresh_token = jwt.encode({
        'user_id': user.id,
        'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=30)
    }, JWT_REFRESH_TOKEN_SECRET, algorithm='HS256')

    return jsonify({
        'message': 'Login successful',
        'access_token': access_token,
        'refresh_token': refresh_token
    }), 200
    
@authRouter.route('/refresh', methods=['POST'])
def refresh_token():
    data = request.get_json()
    tokenRefreshRequest = TokenRefreshRequest()
    errors = tokenRefreshRequest.validate(data)
    if errors:
        return jsonify(errors), 400
    
    refresh_token = data.get('refresh_token')
    try:
        decoded_token = jwt.decode(refresh_token, JWT_REFRESH_TOKEN_SECRET, algorithms=['HS256'])
    except jwt.ExpiredSignatureError:
        return jsonify({'error': 'Refresh token has expired'}), 401
    
    user = db_session.query(User).filter_by(id=decoded_token['user_id']).first()
    if not user:
        return jsonify({'error': 'Invalid refresh token'}), 401
    
    access_token = jwt.encode({
        'user_id': user.id,
        'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(minutes=10)
    }, JWT_ACCESS_TOKEN_SECRET, algorithm='HS256')
    
    return jsonify({
        'message': 'Token refreshed',
        'access_token': access_token
    }), 200