from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session
from typing import List
from http_models import GoalCreateRequest, GoalUpdateRequest, GoalResponse
from models import Goal, Category, User
from dependencies.auth import get_current_user
from db import get_db

router = APIRouter(
    prefix="/goals",
    tags=["goals"]
)

@router.get("/", response_model=List[GoalResponse])
def get_goals(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    goals = db.query(Goal).filter(Goal.user_id == current_user.id).all()
    return goals

@router.post("/", response_model=GoalResponse, status_code=status.HTTP_201_CREATED)
def create_goal(goal: GoalCreateRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if goal.category_id is not None:
        # Ensure that if a category is supplied, it exists and is accessible.
        category = db.query(Category).filter(
            Category.id == goal.category_id,
            (Category.user_id == current_user.id) | (Category.user_id.is_(None))
        ).first()
        if not category:
            raise HTTPException(status_code=404, detail="Category not found")
    
    new_goal = Goal(
        user_id=current_user.id,
        category_id=goal.category_id,
        goal_type=goal.goal_type,
        limit=goal.limit,
        duration=goal.duration
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
    if goal_update.goal_type is not None:
        goal.goal_type = goal_update.goal_type
    if goal_update.limit is not None:
        goal.limit = goal_update.limit
    if goal_update.duration is not None:
        goal.duration = goal_update.duration
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
