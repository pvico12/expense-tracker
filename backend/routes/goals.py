from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session
from typing import List, Optional
from http_models import GoalCreateRequest, GoalUpdateRequest, GoalResponse, GoalsStatisticsResponse
from models import Goal, Category, User, Transaction
from dependencies.auth import get_current_user
from db import get_db
from sqlalchemy import func
import datetime
from middlewares.goal_utils import calculate_percentage_goal_progress

router = APIRouter(
    prefix="/goals",
    tags=["goals"]
)

@router.get("/{goal_id}", response_model=GoalResponse, status_code=status.HTTP_200_OK)
def get_goal_by_id(goal_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    Retrieve a specific goal for the authenticated user by its ID.
    """
    goal = db.query(Goal).filter(Goal.id == goal_id, Goal.user_id == current_user.id).first()
    if not goal:
        raise HTTPException(status_code=404, detail="Goal not found")
    if goal.category_id:
        if goal.goal_type == "percentage":
            progress, on_track = calculate_percentage_goal_progress(db, current_user.id, goal)
            goal.amount_spent = progress
            goal.on_track = on_track
        else:
            amount_spent = db.query(func.sum(Transaction.amount)).filter(
                Transaction.category_id == goal.category_id,
                Transaction.date >= goal.start_date,
                Transaction.date <= goal.end_date
            ).scalar() or 0
            goal.amount_spent = amount_spent
    else:
        goal.amount_spent = None
    return goal

@router.get("/", response_model=GoalsStatisticsResponse)
def get_goals(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    start_date: Optional[datetime.datetime] = None,
    end_date: Optional[datetime.datetime] = None
):
    """
    Retrieve goals for the authenticated user.
    Optionally filter goals based on start_date and end_date.
    If both start_date and end_date are provided, the query filters so that both the goal's start_date 
    and end_date fall within that period.
    """
    query = db.query(Goal).filter(Goal.user_id == current_user.id)
    if start_date and end_date:
        query = query.filter(
            Goal.start_date <= end_date,
            Goal.end_date >= start_date
        )
    else:
        if start_date:
            query = query.filter(Goal.start_date >= start_date)
        if end_date:
            query = query.filter(Goal.end_date <= end_date)
    goals = query.all()

    for goal in goals:
        if goal.category_id:
            if goal.goal_type == "percentage":
                progress, on_track = calculate_percentage_goal_progress(db, current_user.id, goal)
                goal.amount_spent = progress
                goal.on_track = on_track
            else:
                amount_spent = db.query(func.sum(Transaction.amount)).filter(
                    Transaction.category_id == goal.category_id,
                    Transaction.date >= goal.start_date,
                    Transaction.date <= goal.end_date
                ).scalar() or 0
                goal.amount_spent = amount_spent
        else:
            goal.amount_spent = None

    if start_date is not None and end_date is not None:
        ref_date = end_date
    else:
        ref_date = datetime.datetime.utcnow()
    completed_count = sum(1 for goal in goals if goal.end_date <= ref_date and goal.on_track)
    failed_count = sum(1 for goal in goals if goal.end_date <= ref_date and not goal.on_track)
    incompleted_count = sum(1 for goal in goals if goal.end_date > ref_date)
    return {
        "goals": goals,
        "stats": {
            "completed": completed_count,
            "incompleted": incompleted_count,
            "failed": failed_count
        }
    }

@router.post("/", response_model=GoalResponse, status_code=status.HTTP_201_CREATED)
def create_goal(
    goal: GoalCreateRequest, 
    current_user: User = Depends(get_current_user), 
    db: Session = Depends(get_db)
):
    if goal.category_id is not None:
        category = db.query(Category).filter(
            Category.id == goal.category_id,
            (Category.user_id == current_user.id) | (Category.user_id.is_(None))
        ).first()
        if not category:
            raise HTTPException(status_code=404, detail="Category not found")
    
    start_date = goal.start_date
    end_date = start_date + datetime.timedelta(days=goal.period - 1)

    new_goal = Goal(
        user_id=current_user.id,
        category_id=goal.category_id,
        goal_type=goal.goal_type,
        limit=goal.limit,
        start_date=start_date,
        end_date=end_date,
    )
    db.add(new_goal)
    db.commit()
    db.refresh(new_goal)
    return new_goal

@router.put("/{goal_id}", response_model=GoalResponse)
def update_goal(goal_id: int, goal_update: GoalUpdateRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    goal = db.query(Goal).filter(Goal.id == goal_id, Goal.user_id == current_user.id).first()
    if not goal:
        raise HTTPException(status_code=404, detail="Goal not found")
    if goal_update.category_id is not None:
        category = db.query(Category).filter(
            Category.id == goal_update.category_id,
            (Category.user_id == current_user.id) | (Category.user_id.is_(None))
        ).first()
        if not category:
            raise HTTPException(status_code=404, detail="Category not found")
        goal.category_id = goal_update.category_id

    if goal_update.goal_type is not None:
        goal.goal_type = goal_update.goal_type
    if goal_update.limit is not None:
        goal.limit = goal_update.limit
    if goal_update.start_date is not None:
        goal.start_date = goal_update.start_date
    if goal_update.end_date is not None:
        goal.end_date = goal_update.end_date
    if goal.category_id:
        actual_amount_spent = db.query(func.sum(Transaction.amount)).filter(
            Transaction.category_id == goal.category_id,
            Transaction.date >= goal.start_date,
            Transaction.date <= goal.end_date
        ).scalar() or 0
        if goal.goal_type == "percentage":
            progress, on_track = calculate_percentage_goal_progress(db, current_user.id, goal)
            goal.amount_spent = progress
            goal.on_track = on_track
        else:
            goal.amount_spent = actual_amount_spent
            goal.on_track = actual_amount_spent <= goal.limit
    else:
        goal.amount_spent = None
        goal.on_track = True
    db.commit()
    db.refresh(goal)
    return goal

@router.delete("/{goal_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_goal(goal_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    goal = db.query(Goal).filter(Goal.id == goal_id, Goal.user_id == current_user.id).first()
    if not goal:
        raise HTTPException(status_code=404, detail="Goal not found")
    db.delete(goal)
    db.commit()
    return
