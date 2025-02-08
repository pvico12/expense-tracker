from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from db import get_db
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
