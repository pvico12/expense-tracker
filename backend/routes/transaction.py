from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session
from typing import List, Optional, Dict

import jwt
from jwt import PyJWTError
from dotenv import load_dotenv
import os

from http_models import TransactionCreateRequest, TransactionResponse, CategoryResponse, SubCategoryResponse, SummaryResponse, CategoryStats, CustomCategoryCreateRequest
from datetime import datetime
from typing import Optional
from db import get_db, add_transaction, get_transactions as db_get_transactions, get_all_categories_for_user
from models import Transaction, TransactionType, User, Category
from dependencies.auth import get_current_user

load_dotenv()
JWT_ACCESS_TOKEN_SECRET = os.getenv('JWT_ACCESS_TOKEN_SECRET')

router = APIRouter(
    prefix="/transactions",
    tags=["transactions"]
)

@router.post("/", response_model=TransactionResponse, status_code=status.HTTP_201_CREATED)
def create_transaction(
    transaction: TransactionCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Create a new transaction for the authenticated user.
    """
    try:
        new_transaction = add_transaction(
            user_id=current_user.id,
            amount=transaction.amount,
            category_id=transaction.category_id,
            transaction_type=transaction.transaction_type,
            note=transaction.note,
            date=transaction.date
        )
        return TransactionResponse.from_orm(new_transaction)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/", response_model=List[TransactionResponse])
def read_transactions(
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Retrieve transactions for the authenticated user.
    """
    transactions = db_get_transactions(user_id=current_user.id, limit=limit, offset=skip)
    return [TransactionResponse.from_orm(tx) for tx in transactions]

@router.delete("/{transaction_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_transaction(
    transaction_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Delete a transaction by its ID for the authenticated user.
    """
    transaction = db.query(Transaction).filter(Transaction.id == transaction_id, Transaction.user_id == current_user.id).first()
    if not transaction:
        raise HTTPException(status_code=404, detail="Transaction not found")
    db.delete(transaction)
    db.commit()
    return

@router.get("/categories", response_model=List[CategoryResponse])
def get_categories(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Retrieve all categories (global and user-specific) for the authenticated user.
    """
    categories = get_all_categories_for_user(user_id=current_user.id)
    response = []

    for head_cat in categories:
        head_response = CategoryResponse.from_orm(head_cat)
        if head_cat.subcategories:
            head_response.subcategories = [
                SubCategoryResponse.from_orm(subcat)
                for subcat in head_cat.subcategories
            ]
        response.append(head_response)
    
    return response

@router.post("/categories/custom", response_model=CategoryResponse, status_code=status.HTTP_201_CREATED)
def create_custom_category(
    custom_category: CustomCategoryCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    try:
        head_category = db.query(Category).filter(
            Category.id == custom_category.parent_id,
            (Category.user_id == current_user.id) | (Category.user_id == None)
        ).first()
        
        if not head_category:
            raise HTTPException(status_code=404, detail="Head category not found")
        
        # Create the new custom category
        new_category = Category(
            name=custom_category.name,
            parent_id=custom_category.parent_id,
            user_id=current_user.id  # Assign to the current user
        )
        db.add(new_category)
        db.commit()
        db.refresh(new_category)
        
        return CategoryResponse.from_orm(new_category)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

