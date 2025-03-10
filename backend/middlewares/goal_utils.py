from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from sqlalchemy import func
from models import Goal, Transaction
from typing import Optional

def recalc_goal_progress(db: Session, user_id: int, category_id: Optional[int] = None):
    """
    Recalculate progress for all goals.
    If category_id is provided, recalc goals for that category;
    if None, recalc overall (global) spending goals.
    """
    now = datetime.utcnow()
    if category_id is None:
        goals = db.query(Goal).filter(Goal.user_id == user_id, Goal.category_id.is_(None)).all()
    else:
        goals = db.query(Goal).filter(Goal.user_id == user_id, Goal.category_id == category_id).all()
    
    for goal in goals:
        # Determine period start based on the goal's duration.
        if goal.duration == "week":
            # Assuming the week starts on Monday; adjust if needed.
            period_start = now - timedelta(days=now.weekday())
        elif goal.duration == "month":
            period_start = now.replace(day=1)
        else:
            period_start = now
        
        # Sum up spending in the period.
        if goal.category_id is None:
            # Global spending: sum all transactions for the user.
            total_spent = db.query(func.sum(Transaction.amount)).filter(
                Transaction.user_id == user_id,
                Transaction.date >= period_start
            ).scalar() or 0.0
        else:
            total_spent = db.query(func.sum(Transaction.amount)).filter(
                Transaction.user_id == user_id,
                Transaction.category_id == goal.category_id,
                Transaction.date >= period_start
            ).scalar() or 0.0
        
        # For amount-based goals, check if spending is within limit.
        if goal.goal_type == "amount":
            goal.on_track = total_spent <= goal.limit
        elif goal.goal_type == "percentage":
            # You can implement percentage logic comparing periods here.
            # For now, we default to True.
            goal.on_track = True
        
        db.add(goal)
    
    db.commit()
