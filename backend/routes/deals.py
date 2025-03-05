from typing import Optional
from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session

from models import DealVote, User, Deal
from utils import get_coordinate_distance, predict_category, get_category_by_name
from dependencies.auth import get_current_user
from db import add_deal, get_db, get_single_deal
from db import get_deals as db_get_deals
from typing import List
from http_models import DealCreationRequest, DealRetrievalRequest, DealUpdateRequest, DealVoteResponse, HttpDeal, LocationFilter

router = APIRouter(
    prefix="/deals",
    tags=["deals"]
)
    
@router.post("/list", response_model=List[HttpDeal], status_code=status.HTTP_200_OK)
def get_deals(
    filters: Optional[DealRetrievalRequest] = None
):
    """
    Get all deals. Optionally filter by user_id and location.
    """
    try:
        # get all the deals
        deals = db_get_deals(filters.user_id)
        
        location_filter = filters.location
        if location_filter:
            print("here")
            # filter by location
            deals = [deal for deal in deals if
                     abs(get_coordinate_distance(
                            deal.latitude, deal.longitude,
                            location_filter.latitude, location_filter.longitude
                         )) <= location_filter.distance]
            
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

@router.get("/votes/{deal_id}", response_model=DealVoteResponse, status_code=status.HTTP_200_OK)
def get_deal_votes(
    deal_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Get the total upvotes and downvotes for a deal.
    """
    try:
        # check if deal exists
        deal = get_single_deal(deal_id)
        if not deal:
            raise Exception("Deal not found.")
        
        # find all votes with this deal id
        votes = db.query(DealVote).filter(DealVote.deal_id == deal_id).all()
        
        # count upvotes and downvotes
        upvotes = len([vote for vote in votes if vote.vote == 1])
        downvotes = len([vote for vote in votes if vote.vote == -1])
        
        return DealVoteResponse(upvotes=upvotes, downvotes=downvotes)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/upvote/{deal_id}", response_model=None, status_code=status.HTTP_200_OK)
def upvote_deal(
    deal_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Upvote a deal.
    """
    try:
        # check if deal exists
        deal = get_single_deal(deal_id)
        if not deal:
            raise Exception("Deal not found.")
        
        # find votes with this deal id and current user id
        curr_user_vote = db.query(DealVote).filter(DealVote.deal_id == deal_id, DealVote.user_id == current_user.id).first()
        
        if curr_user_vote:
            # if vote is already an upvote, throw Exception
            if curr_user_vote.vote == 1:
                raise Exception("You have already upvoted this deal.")
            
            # if user has already downvoted, update the vote
            curr_user_vote.vote = 1
            db.commit()
        else:
            # if user has not voted, create a new vote
            new_vote = DealVote(deal_id=deal_id, user_id=current_user.id, vote=1)
            db.add(new_vote)
            db.commit()
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    
@router.post("/downvote/{deal_id}", response_model=None, status_code=status.HTTP_200_OK)
def downvote_deal(
    deal_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Downvote a deal.
    """
    try:
        # check if deal exists
        deal = get_single_deal(deal_id)
        if not deal:
            raise Exception("Deal not found.")
        
        # find votes with this deal id and current user id
        curr_user_vote = db.query(DealVote).filter(DealVote.deal_id == deal_id, DealVote.user_id == current_user.id).first()
        
        if curr_user_vote:
            # if user has already downvoted, throw Exception
            if curr_user_vote.vote == -1:
                raise Exception("You have already downvoted this deal.")
            
            # if user has already upvoted, update the vote
            curr_user_vote.vote = -1
            db.commit()
        else:
            # if user has not voted, create a new vote
            new_vote = DealVote(deal_id=deal_id, user_id=current_user.id, vote=-1)
            db.add(new_vote)
            db.commit()
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e)
)