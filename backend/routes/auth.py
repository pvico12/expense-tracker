import logging
import os
import datetime
import jwt
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from dotenv import load_dotenv

from http_models import FCMTokenUploadRequest, RegistrationRequest, LoginRequest, TokenRefreshRequest
from models import FcmToken, User
from db import get_db, add_predefined_categories
from dependencies.auth import get_current_user
from utils import hash_password

logger = logging.getLogger(__name__)

load_dotenv()
JWT_ACCESS_TOKEN_SECRET = os.getenv('JWT_ACCESS_TOKEN_SECRET')
JWT_REFRESH_TOKEN_SECRET = os.getenv('JWT_REFRESH_TOKEN_SECRET')

router = APIRouter(prefix="/auth", tags=["auth"])

@router.post("/register", status_code=201)
def register(data: RegistrationRequest, db: Session = Depends(get_db)):
    
    # check for existing users with same username
    user = db.query(User).filter_by(username=data.username).first()
    if user:
        raise HTTPException(status_code=400, detail="Username already exists")
    
    hashed_password = hash_password(data.password)
    new_user = User(
        username=data.username,
        password=hashed_password,
        firstname=data.firstname,
        lastname=data.lastname
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    add_predefined_categories(new_user.id)
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
        'refresh_token': refresh_token,
        'role': user.role
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

@router.post("/fcm_token")
def send_fcm_token(
    request: FCMTokenUploadRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    try:
        # remove all tokens related to the userid
        db.query(FcmToken).filter_by(user_id=current_user.id).delete()
        db.commit()
        
        # add the new token
        token = FcmToken(user_id=current_user.id, token=request.fcm_token)
        db.add(token)
        db.commit()
        
        logger.info(f"FCM token added for user {current_user.id}. Token: {request.fcm_token}")
        
        return {"message": "Token added successfully"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
