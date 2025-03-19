from notifications import send_push_notification_healthcheck
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from db import get_db

router = APIRouter(
    prefix="/notifications",
    tags=["notifications"]
)

@router.post("/healthcheck", status_code=status.HTTP_200_OK)
def send_healtcheck_notification(
    db: Session = Depends(get_db)
):
    send_push_notification_healthcheck()
    return {"message": "Healthcheck notifications sent"}


