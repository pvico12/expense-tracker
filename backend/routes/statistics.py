from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session
from typing import List, Optional, Dict

import jwt
from jwt import PyJWTError
from dotenv import load_dotenv
import os

from http_models import SummaryResponse, CategoryStats, SummaryCategoryResponse, TransactionResponse
from datetime import datetime, timedelta
from db import get_db, get_transactions as db_get_transactions, get_all_categories_for_user
from models import Transaction, TransactionType, User
from fastapi.security import HTTPAuthorizationCredentials
from dependencies.auth import get_current_user  # Updated import

load_dotenv()
JWT_ACCESS_TOKEN_SECRET = os.getenv('JWT_ACCESS_TOKEN_SECRET')

router = APIRouter(
    prefix="/statistics",
    tags=["statistics"]
)

def fetch_transactions(
    db: Session,
    user: User,
    start_date: Optional[datetime],
    end_date: Optional[datetime]
) -> List[Transaction]:
    if end_date is None:
        # last day of the month
        end_date = datetime.utcnow().replace(day=1, hour=23, minute=59, second=59, microsecond=999999).replace(month=datetime.utcnow().month + 1) - timedelta(days=1)
    if start_date is None:
        # first day of the month
        start_date = end_date.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

    transactions = db.query(Transaction).filter(
        Transaction.user_id == user.id,
        Transaction.date >= start_date,
        Transaction.date <= end_date
    ).all()
    return transactions

def calculate_type_totals(transactions: List[Transaction]) -> Dict[str, float]:
    type_totals: Dict[str, float] = {
        TransactionType.INCOME.value: 0.0,
        TransactionType.EXPENSE.value: 0.0
        # TransactionType.TRANSFER.value: 0.0
    }

    for tx in transactions:
        tx_type = tx.transaction_type.value
        if tx_type in type_totals:
            type_totals[tx_type] += tx.amount

    return type_totals

def serialize_transaction_history(transactions: List[Transaction]) -> List[TransactionResponse]:
    return [
        TransactionResponse(
            id=tx.id,
            amount=tx.amount,
            transaction_type=tx.transaction_type.value,
            category_name=tx.category.name if tx.category else None,
            date=tx.date
            # Add other fields as necessary
        )
        for tx in transactions
    ]

@router.get("/summary_spend", response_model=SummaryResponse)
def get_summary(
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    transactions = fetch_transactions(db, current_user, start_date, end_date)
    type_totals = calculate_type_totals(transactions)

    total_spend = 0.0
    # Group transactions by (category name, color) tuple
    category_totals: Dict[tuple, float] = {}

    for tx in transactions:
        # if tx.transaction_type in [TransactionType.EXPENSE, TransactionType.TRANSFER]:
        if tx.transaction_type in [TransactionType.EXPENSE]:
            total_spend += tx.amount
            # Group by both category name and color; if no category exists, use "Uncategorized" and no color.
            key = (tx.category.name, tx.category.color) if tx.category else ("Uncategorized", None)
            category_totals[key] = category_totals.get(key, 0.0) + tx.amount

    # Calculate percentages and build the breakdown list including the color.
    category_breakdown = []
    for (name, color), amount in category_totals.items():
        percentage = (amount / total_spend) * 100 if total_spend > 0 else 0
        category_breakdown.append(CategoryStats(
            category_name=name,
            total_amount=amount,
            percentage=percentage,
            color=color
        ))

    # Optionally, sort the breakdown by percentage descending
    category_breakdown.sort(key=lambda x: x.percentage, reverse=True)

    return SummaryResponse(
        total_spend=total_spend,
        category_breakdown=category_breakdown,
        transaction_history=transactions,
        type_totals=type_totals
    )

@router.get("/summary_category", response_model=SummaryCategoryResponse)
def get_summary_category(
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    transactions = fetch_transactions(db, current_user, start_date, end_date)
    type_totals = calculate_type_totals(transactions)

    return SummaryCategoryResponse(
        type_totals=type_totals,
        transaction_history=transactions
    )
