import os
import jwt
from fastapi import Request, HTTPException, Depends
from dotenv import load_dotenv

load_dotenv()
JWT_ACCESS_TOKEN_SECRET = os.getenv('JWT_ACCESS_TOKEN_SECRET')

if not JWT_ACCESS_TOKEN_SECRET:
    raise Exception("JWT_ACCESS_TOKEN_SECRET not found in .env file")

def authenticate_user_token(request: Request):
    """
    Dependency that verifies the JWT access token.
    In development mode, authentication is skipped.
    """
    # Skip authentication in development mode.
    if os.getenv('FLASK_ENV') == 'development':
        return {"user_id": 0}  # dummy payload

    # Retrieve the Authorization header.
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    token = auth_header.split("Bearer ")[1]
    if not token:
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    try:
        decoded_token = jwt.decode(token, JWT_ACCESS_TOKEN_SECRET, algorithms=["HS256"])
        print("Decoded JWT:", decoded_token)
        if 'user_id' not in decoded_token:
            raise HTTPException(status_code=401, detail="Unauthorized")
        return decoded_token
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token has expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")

def verify_user(user_id: int, token_data: dict = Depends(authenticate_user_token)):
    """
    Dependency that checks if the user_id from the route matches the user_id in the token.
    """
    if os.getenv('FLASK_ENV') == 'development':
        return token_data

    if token_data.get('user_id') != user_id:
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    return token_data
