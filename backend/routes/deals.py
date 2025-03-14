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

# Helpers
def get_deal_votes_by_id(db, id):
    # find all votes with this deal id
    votes = db.query(DealVote).filter(DealVote.deal_id == id).all()
    
    # count upvotes and downvotes
    upvotes = len([vote for vote in votes if vote.vote == 1])
    downvotes = len([vote for vote in votes if vote.vote == -1])
    
    return {
        "upvotes": upvotes,
        "downvotes": downvotes
    }
    
def get_maps_link(latitude, longitude):
    return f"https://www.google.com/maps?q={latitude},{longitude}"
    
@router.post("/list", response_model=List[HttpDeal], status_code=status.HTTP_200_OK)
def get_deals(
    filters: Optional[DealRetrievalRequest] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Get all deals. Optionally filter by user_id and location.
    """
    try:
        # get all the deals
        deals = db_get_deals(filters.user_id)
        
        location_filter = filters.location
        if location_filter:
            # filter by location
            deals = [deal for deal in deals if
                     abs(get_coordinate_distance(
                            deal.latitude, deal.longitude,
                            location_filter.latitude, location_filter.longitude
                         )) <= location_filter.distance]
            
        for deal in deals:
            # get votes for each deal
            votes = get_deal_votes_by_id(db, deal.id)
            deal.upvotes = votes["upvotes"]
            deal.downvotes = votes["downvotes"]
            
            # check if current_user has voted
            target_vote = db.query(DealVote).filter(DealVote.deal_id == deal.id, DealVote.user_id == current_user.id).first()
            deal.user_vote = target_vote.vote if target_vote else 0
            
            # insert maps link
            deal.maps_link = get_maps_link(deal.latitude, deal.longitude)
            
        deals_list = [HttpDeal.from_orm(deal) for deal in deals]
        return deals_list
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/{deal_id}", response_model=HttpDeal, status_code=status.HTTP_200_OK)
def get_specific_deal(
    deal_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Get a specific deal.
    """
    try:
        # find deal with this id
        deal = get_single_deal(deal_id)
        
        if not deal:
            raise Exception("Deal not found.")
        
        # get votes for this deal
        votes = get_deal_votes_by_id(db, deal_id)
        deal.upvotes = votes["upvotes"]
        deal.downvotes = votes["downvotes"]
        
        # check if current_user has voted
        target_vote = db.query(DealVote).filter(DealVote.deal_id == deal_id, DealVote.user_id == current_user.id).first()
        deal.user_vote = target_vote.vote if target_vote else 0
        
        # insert maps link
        deal.maps_link = get_maps_link(deal.latitude, deal.longitude)
        
        return HttpDeal.from_orm(deal)
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
        add_deal(current_user.id, deal.name, deal.description, deal.price, deal.address, deal.longitude, deal.latitude, deal.date, deal.vendor)
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
        
        votes = get_deal_votes_by_id(db, deal_id)
        return DealVoteResponse(**votes)
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
        raise HTTPException(status_code=400, detail=str(e))
    
@router.post("/cancel_vote/{deal_id}", response_model=None, status_code=status.HTTP_200_OK)
def cancel_vote(
    deal_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Cancel a vote for a deal.
    """
    try:
        # check if deal exists
        deal = get_single_deal(deal_id)
        if not deal:
            raise Exception("Deal not found.")
        
        # find vote with this deal id and current user id
        curr_user_vote = db.query(DealVote).filter(DealVote.deal_id == deal_id, DealVote.user_id == current_user.id).first()
        
        if curr_user_vote:
            # delete the vote
            db.delete(curr_user_vote)
            db.commit()
            return "You have successfully removed your vote."
        else:
            raise Exception("You have not voted on this deal.")
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))