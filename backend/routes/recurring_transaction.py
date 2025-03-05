from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session
from typing import List
from datetime import datetime, timedelta

from db import get_db
from models import RecurringTransaction, Transaction, Category, User, TransactionType
from dependencies.auth import get_current_user
from http_models import (
    RecurringTransactionResponse,
    RecurringTransactionCreateRequest,
    RecurringTransactionUpdateRequest
)

router = APIRouter(
    prefix="/transactions/recurring",
    tags=["recurring_transactions"]
)

def generate_dates(start_date: datetime, end_date: datetime, period: int) -> List[datetime]:
    dates = []
    current_date = start_date
    while current_date <= end_date:
        dates.append(current_date)
        current_date += timedelta(days=period)
    return dates

@router.get("/", response_model=List[RecurringTransactionResponse])
def get_recurring_transactions(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    recurring_list = db.query(RecurringTransaction).filter(RecurringTransaction.user_id == current_user.id).all()
    return recurring_list

@router.post("/", response_model=RecurringTransactionResponse, status_code=status.HTTP_201_CREATED)
def create_recurring_transaction(
    request: RecurringTransactionCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Verify the category is valid for the user
    category = db.query(Category).filter(
        Category.id == request.category_id,
        (Category.user_id == current_user.id) | (Category.user_id.is_(None))
    ).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    # Create the master recurring transaction record
    recurring = RecurringTransaction(
        start_date=request.start_date,
        end_date=request.end_date,
        note=request.note,
        period=request.period,
        user_id=current_user.id
    )
    db.add(recurring)
    db.commit()
    db.refresh(recurring)
    
    # Automatically generate transactions for each recurring date
    dates = generate_dates(request.start_date, request.end_date, request.period)
    transactions = []
    for d in dates:
        tx = Transaction(
            amount=request.amount,
            category_id=request.category_id,
            transaction_type=request.transaction_type,
            note=request.note,
            date=d,
            vendor=request.vendor,
            user_id=current_user.id,
            recurring_id=recurring.id
        )
        transactions.append(tx)
    db.bulk_save_objects(transactions)
    db.commit()

    return recurring

@router.put("/", response_model=RecurringTransactionResponse)
def update_recurring_transaction(
    recurring_id: int,
    request: RecurringTransactionUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    recurring = db.query(RecurringTransaction).filter(
        RecurringTransaction.id == recurring_id,
        RecurringTransaction.user_id == current_user.id
    ).first()
    if not recurring:
        raise HTTPException(status_code=404, detail="Recurring transaction not found")
    
    # Delete all current generated transactions associated with the recurring record
    db.query(Transaction).filter(Transaction.recurring_id == recurring.id).delete()
    db.commit()
    
    # Update recurring master record
    recurring.start_date = request.start_date
    recurring.end_date = request.end_date
    recurring.note = request.note
    recurring.period = request.period
    db.commit()
    
    # Regenerate transactions based on the updated schedule
    dates = generate_dates(request.start_date, request.end_date, request.period)
    transactions = []
    for d in dates:
        tx = Transaction(
            amount=request.amount,
            category_id=request.category_id,
            transaction_type=request.transaction_type,
            note=request.note,
            date=d,
            vendor=request.vendor,
            user_id=current_user.id,
            recurring_id=recurring.id
        )
        transactions.append(tx)
    db.bulk_save_objects(transactions)
    db.commit()
    
    return recurring

@router.delete("/", status_code=status.HTTP_204_NO_CONTENT)
def delete_recurring_transaction(
    recurring_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    recurring = db.query(RecurringTransaction).filter(
        RecurringTransaction.id == recurring_id,
        RecurringTransaction.user_id == current_user.id
    ).first()
    if not recurring:
        raise HTTPException(status_code=404, detail="Recurring transaction not found")
    
    # Delete all associated generated transactions first
    db.query(Transaction).filter(Transaction.recurring_id == recurring.id).delete()
    db.commit()
    
    # Remove the recurring transaction entry itself
    db.delete(recurring)
    db.commit()
    
    return
