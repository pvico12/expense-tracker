import os
import datetime
import jwt
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from dotenv import load_dotenv

from http_models import RegistrationRequest, LoginRequest, TokenRefreshRequest
from models import User
from db import get_db
from utils import hash_password

load_dotenv()
JWT_ACCESS_TOKEN_SECRET = os.getenv('JWT_ACCESS_TOKEN_SECRET')
JWT_REFRESH_TOKEN_SECRET = os.getenv('JWT_REFRESH_TOKEN_SECRET')

router = APIRouter(prefix="/auth", tags=["auth"])

@router.post("/register", status_code=201)
def register(data: RegistrationRequest, db: Session = Depends(get_db)):
    hashed_password = hash_password(data.password)
    new_user = User(
        username=data.username,
        password=hashed_password,
        firstname=data.firstname,
        lastname=data.lastname
    )
    db.add(new_user)
    db.commit()
    return {"message": "User created successfully"}

@router.post("/login")
def login(data: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter_by(username=data.username).first()
    if not user or user.password != hash_password(data.password):
        raise HTTPException(status_code=401, detail="Invalid username or password")
    
    access_token = jwt.encode({
        'user_id': user.id,
        'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(minutes=10)
    }, JWT_ACCESS_TOKEN_SECRET, algorithm='HS256')
    
    refresh_token = jwt.encode({
        'user_id': user.id,
        'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=30)
    }, JWT_REFRESH_TOKEN_SECRET, algorithm='HS256')
    
    return {
        'message': 'Login successful',
        'access_token': access_token,
        'refresh_token': refresh_token
    }

@router.post("/refresh")
def refresh_token(data: TokenRefreshRequest, db: Session = Depends(get_db)):
    try:
        decoded_token = jwt.decode(data.refresh_token, JWT_REFRESH_TOKEN_SECRET, algorithms=['HS256'])
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Refresh token has expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid refresh token")
    
    user = db.query(User).filter_by(id=decoded_token['user_id']).first()
    if not user:
        raise HTTPException(status_code=401, detail="Invalid refresh token")
    
    access_token = jwt.encode({
        'user_id': user.id,
        'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(minutes=10)
    }, JWT_ACCESS_TOKEN_SECRET, algorithm='HS256')
    
    return {
        'message': 'Token refreshed',
        'access_token': access_token
    }
