import os
import jwt
from flask import request, jsonify
from functools import wraps
from dotenv import load_dotenv

load_dotenv()
JWT_ACCESS_TOKEN_SECRET = os.getenv('JWT_ACCESS_TOKEN_SECRET')

if not JWT_ACCESS_TOKEN_SECRET:
    raise Exception("JWT_ACCESS_TOKEN_SECRET not found in .env file")

def authenticate_user_token(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        if os.getenv('FLASK_ENV') == 'development':
            return func(*args, **kwargs)

        # Check if auth Bearer token header is present
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({"error": "Unauthorized"}), 401
        
        # Extract the token after 'Bearer'
        token = auth_header.split("Bearer ")[1]
        if not token:
            return jsonify({"error": "Unauthorized"}), 401

        try:
            # Decode and verify the JWT
            decoded_token = jwt.decode(token, JWT_ACCESS_TOKEN_SECRET, algorithms=["HS256"])
            print("Decoded JWT:", decoded_token)
            
            # check if the user_id exists in the token
            if 'user_id' not in decoded_token:
                return jsonify({"error": "Unauthorized"}), 401
            
            # Check if decoded_token user_id matches the user_id in the endpoint
            user_id = kwargs.get('user_id')
            if not user_id or decoded_token['user_id'] != user_id:
                return jsonify({"error": "Unauthorized"}), 401

        except jwt.ExpiredSignatureError:
            return jsonify({"error": "Token has expired"}), 401
        except jwt.InvalidTokenError:
            return jsonify({"error": "Invalid token"}), 401
        
        # continue with the route function if token is valid
        return func(*args, **kwargs)
    return wrapper
