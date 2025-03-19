from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from sqlalchemy import func
from models import Goal, Transaction
from typing import Optional, List, Dict, Any

def recalc_goal_progress(db: Session, user_id: int, category_id: Optional[int] = None):
    """
    Recalculate progress for all goals.
    If category_id is provided, recalc goals for that category;
    if None, recalc overall (global) spending goals.
    """
    if category_id is None:
        goals = db.query(Goal).filter(Goal.user_id == user_id, Goal.category_id.is_(None)).all()
    else:
        goals = db.query(Goal).filter(Goal.user_id == user_id, Goal.category_id == category_id).all()
    
    for goal in goals:
        if goal.goal_type == "amount":
            total_spent = db.query(func.sum(Transaction.amount)).filter(
                Transaction.user_id == user_id,
                Transaction.date >= goal.start_date,
                Transaction.date <= goal.end_date
            ).scalar() or 0.0
            goal.on_track = total_spent <= goal.limit
            goal.amount_spent = total_spent
        ## modify this to use the new calculate_percentage_goal_progress function
        elif goal.goal_type == "percentage":
            progress, on_track = calculate_percentage_goal_progress(db, user_id, goal)
            goal.amount_spent = progress
            goal.on_track = on_track
        db.add(goal)
    db.commit()


def calculate_goal_spending(db: Session, user_id: int, goal: Goal) -> float:
    """
    Calculate the total spending for a given goal period.
    """
    total_spent = db.query(func.sum(Transaction.amount)).filter(
        Transaction.user_id == user_id,
        Transaction.date >= goal.start_date,
        Transaction.date <= goal.end_date
    ).scalar() or 0.0
    return total_spent

## Feel free to change the logic here based on the needs of the notification system. I am putting everything into a list, but you can manage it to just return a true/false value. 
def get_mid_period_notifications(db: Session, user_id: int) -> List[Dict[str, Any]]:
    """
    Check for goals that are at least 80% through their period and not yet ended.
    For these goals (currently implemented for "amount" type),
    notify the user of how much percentage difference exists between their spending and the goal limit.
    """
    notifications = []
    now = datetime.utcnow()
    # Only consider ongoing goals (end date > now)
    ongoing_goals = db.query(Goal).filter(Goal.user_id == user_id, Goal.end_date > now).all()
    
    for goal in ongoing_goals:
        total_duration = (goal.end_date - goal.start_date).total_seconds()
        elapsed = (now - goal.start_date).total_seconds()
        # Check if the goal's period is at least 80% complete
        if total_duration > 0 and (elapsed / total_duration) >= 0.8:
            total_spent = calculate_goal_spending(db, user_id, goal)
            if goal.goal_type == "amount":
                if total_spent <= goal.limit:
                    remaining_pct = ((goal.limit - total_spent) / goal.limit) * 100
                    message = (
                        f"Goal {goal.id}: You are {remaining_pct:.1f}% away from breaking your spending limit."
                    )
                else:
                    exceeded_pct = ((total_spent - goal.limit) / goal.limit) * 100
                    message = (
                        f"Goal {goal.id}: You have exceeded your spending limit by {exceeded_pct:.1f}%."
                    )
            else:
                message = f"Goal {goal.id}: Percentage goal notifications are not implemented yet."
            notifications.append({"goal_id": goal.id, "message": message})
    return notifications


def get_post_period_notifications(db: Session, user_id: int) -> List[Dict[str, Any]]:
    """
    For goals whose period has ended, notify the user whether they succeeded or failed their goal.
    For an "amount" goal: if total spending is under the limit, include the margin percentage;
    otherwise, include the percentage by which the goal was exceeded.
    """
    notifications = []
    now = datetime.utcnow()
    ended_goals = db.query(Goal).filter(Goal.user_id == user_id, Goal.end_date <= now).all()
    
    for goal in ended_goals:
        total_spent = calculate_goal_spending(db, user_id, goal)
        if goal.goal_type == "amount":
            if total_spent <= goal.limit:
                margin_pct = ((goal.limit - total_spent) / goal.limit) * 100
                message = (
                    f"Goal {goal.id}: Completed successfully with a {margin_pct:.1f}% margin remaining."
                )
            else:
                excess_pct = ((total_spent - goal.limit) / goal.limit) * 100
                message = (
                    f"Goal {goal.id}: Failed, exceeded the goal by {excess_pct:.1f}%."
                )
        else:
            message = f"Goal {goal.id}: Percentage goal notifications are not implemented yet."
        notifications.append({"goal_id": goal.id, "message": message})
    return notifications


def calculate_percentage_goal_progress(db: Session, user_id: int, goal: Goal) -> tuple[float, bool, float, float]:
    """
    Calculates the current period spending progress for a percentage goal.
    For a percentage goal, the 'limit' field represents the percentage reduction target relative to the previous period.
    The previous period is defined as the period of the same length immediately preceding the goal's start date.
    """
    ## I didn't store period in the goal object, so I'm recalcultating it
    period = goal.end_date - goal.start_date
    previous_period_start_date = goal.start_date - period
    previous_period_end_date = goal.start_date

    # previous_period_amount 
    previous_period_amount = db.query(func.sum(Transaction.amount)).filter(
        Transaction.category_id == goal.category_id,
        Transaction.date >= previous_period_start_date,
        Transaction.date < previous_period_end_date
    ).scalar() or 0.0

    # current_amount
    current_amount = db.query(func.sum(Transaction.amount)).filter(
        Transaction.category_id == goal.category_id,
        Transaction.date >= goal.start_date,
        Transaction.date <= goal.end_date
    ).scalar() or 0.0

    # Avoid division by zero
    if previous_period_amount > 0:
        # example: 500 - 300 / 500 * 100 = 40
        progress_percentage = ((previous_period_amount - current_amount) / previous_period_amount) * 100
    else:
        progress_percentage = 0 if current_amount == 0 else 100

    on_track = progress_percentage >= goal.limit
    return progress_percentage, on_track