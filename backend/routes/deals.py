from typing import Optional
from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session

from models import User, Deal
from utils import predict_category, get_category_by_name
from dependencies.auth import get_current_user
from db import add_deal, get_db, get_single_deal
from db import get_deals as db_get_deals
from typing import List
from http_models import DealCreationRequest, DealUpdateRequest, HttpDeal

router = APIRouter(
    prefix="/deals",
    tags=["deals"]
)
    
@router.get("/", response_model=List[HttpDeal], status_code=status.HTTP_200_OK)
def get_deals(
    user_id: Optional[int] = None
):
    """
    Get all deals. Optionally filter by user_id.
    """
    try:
        # get all the deals
        deals = db_get_deals(user_id)
        deals_list = [HttpDeal.from_orm(deal) for deal in deals]
        return deals_list
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    
@router.post("/", response_model=None, status_code=status.HTTP_201_CREATED)
def create_deal(
    deal: DealCreationRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Create a deal.
    """
    try:
        # create deal
        add_deal(current_user.id, deal.name, deal.description, deal.price, deal.address, deal.longitude, deal.latitude, deal.date)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    
@router.put("/{deal_id}", response_model=None, status_code=status.HTTP_200_OK)
def update_deal(
    deal_id: int,
    data: DealUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Update a deal.
    """
    try:
        # find deal with this id
        target_deal = get_single_deal(deal_id)
        
        # check if user owns the deal
        if target_deal.user_id != current_user.id:
            raise Exception("You do not own this deal.")
        
        # update the deal
        for key, value in data.dict().items():
            setattr(target_deal, key, value)
        db.commit()
        db.refresh(target_deal)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    
@router.delete("/{deal_id}", response_model=None, status_code=status.HTTP_200_OK)
def delete_deal(
    deal_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Delete a deal.
    """
    try:
        # find deal with this id
        target_deal = get_single_deal(deal_id)
        
        # check if user owns the deal
        if target_deal.user_id != current_user.id:
            raise Exception("You do not own this deal.")
        
        # delete the deal
        db.delete(target_deal)
        db.commit()
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    
