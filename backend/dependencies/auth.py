from fastapi import HTTPException, Depends, status
from sqlalchemy.orm import Session
from fastapi.security import HTTPAuthorizationCredentials

import jwt
from jwt import PyJWTError
from dotenv import load_dotenv
import os

from models import User
from db import get_db
import utils

load_dotenv()
JWT_ACCESS_TOKEN_SECRET = os.getenv('JWT_ACCESS_TOKEN_SECRET')


def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(utils.bearer_scheme), db: Session = Depends(get_db)) -> User:
    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_ACCESS_TOKEN_SECRET, algorithms=['HS256'])
        user_id: int = payload.get("user_id")
        if user_id is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid authentication credentials",
                headers={"WWW-Authenticate": "Bearer"},
            )
    except PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return user