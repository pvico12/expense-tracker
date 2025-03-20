from http_models import UserProfileUpdateRequest
from dependencies.auth import get_current_user
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from db import get_db, get_user_level_info
from models import User
from middlewares.user_auth import verify_user

router = APIRouter(prefix="/user", tags=["user"])

@router.get("/profile/{user_id}")
def profile_info(
    user_id: int,
    token_data: dict = Depends(verify_user),
    db: Session = Depends(get_db)
):
    user = db.query(User).filter_by(id=user_id).first()
    if user:
        return user.getProfileInfo()
    else:
        raise HTTPException(status_code=404, detail="User not found")

@router.put("/profile")
def update_profile(
    request: UserProfileUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update user profile data."""
    try:
        # update user data
        user = db.query(User).filter_by(id=current_user.id).first()
        if request.firstname is not None:
            user.firstname = request.firstname
        if request.lastname is not None:
            user.lastname = request.lastname
        if request.username is not None:
            user.username = request.username
        db.commit()
        return {"message": "Profile updated successfully"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/level")
def get_level_info(
    current_user: User = Depends(get_current_user)
):
    return get_user_level_info(current_user.id)
        
