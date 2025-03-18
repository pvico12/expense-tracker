from fastapi import APIRouter, HTTPException, Depends, status
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from typing import List, Optional, Dict

import jwt
from jwt import PyJWTError
from dotenv import load_dotenv
import os

from http_models import ReceiptParseResponse, TransactionCreateRequest, TransactionResponse, CategoryResponse, SummaryResponse, CategoryStats, CustomCategoryCreateRequest, TransactionUpdateRequest
from datetime import datetime
from typing import Optional
from db import get_db, add_transaction, get_transactions as db_get_transactions, get_all_categories_for_user
from models import Transaction, TransactionType, User, Category
from dependencies.auth import get_current_user
from utils import get_category_by_name, parse_receipt
from fastapi import UploadFile, File
import csv
from io import StringIO

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
        # Validate the category (if applicable).
        category = db.query(Category).filter(
            Category.id == transaction.category_id,
            (Category.user_id == current_user.id) | (Category.user_id.is_(None))
        ).first()
        
        if not category:
            raise HTTPException(status_code=404, detail="Category not found")

        new_transaction = add_transaction(
            user_id=current_user.id,
            amount=transaction.amount,
            category_id=transaction.category_id,
            transaction_type=transaction.transaction_type,
            note=transaction.note,
            date=transaction.date,
            vendor=transaction.vendor
        )
        
        from middlewares.goal_utils import recalc_goal_progress
        recalc_goal_progress(db, current_user.id, transaction.category_id)
        recalc_goal_progress(db, current_user.id, None)
        
        return TransactionResponse.from_orm(new_transaction)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/", response_model=List[TransactionResponse])
def read_transactions(
    skip: int = 0,
    limit: int = 100,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Retrieve transactions for the authenticated user.
    """
    transactions = db_get_transactions(
        user_id=current_user.id, 
        limit=limit, 
        offset=skip, 
        start_date=start_date, 
        end_date=end_date
    )
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
    transaction = db.query(Transaction).filter(
        Transaction.id == transaction_id, 
        Transaction.user_id == current_user.id
    ).first()
    if not transaction:
        raise HTTPException(status_code=404, detail="Transaction not found")
    
    category_id = transaction.category_id
    db.delete(transaction)
    db.commit()
    
    from middlewares.goal_utils import recalc_goal_progress
    recalc_goal_progress(db, current_user.id, category_id)
    recalc_goal_progress(db, current_user.id, None)
    
    return

@router.post("/csv", status_code=status.HTTP_201_CREATED)
def upload_csv(
    file: UploadFile = File(...),
    create_transactions: Optional[int] = 0,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Parse a transaction CSV file and return list of transactions.
    If create_transactions is set to 1, also create the transactions in the database.
    """
    try:
        content = file.file.read().decode('utf-8')
        csv_reader = csv.DictReader(StringIO(content))
        
        # Fetch categories for the current user
        categories = get_all_categories_for_user(user_id=current_user.id)
        
        transactions = []
        for row in csv_reader:
            # Find the category by name for the current user
            target_category_id = get_category_by_name(row['category'], categories)
            if not target_category_id:
                raise Exception(f"Category '{row['category']}' not found for the current user")
        
            transaction = {
                "user_id": current_user.id,
                "amount": round(float(row['amount']), 2),
                "category_id": target_category_id,
                "note": row.get('note', ''),
                "date": row['date'],
                "vendor": row.get('vendor')
            }
            transactions.append(transaction)
        
        if create_transactions == 1:
            db_transactions = [Transaction(**t) for t in transactions]
            db.bulk_save_objects(db_transactions)
            db.commit()
            return {"message": "Transactions successfully inserted"}
        
        return transactions
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/csv/template", response_class=FileResponse)
def get_csv_template():
    """
    Return the CSV template file.
    """
    file_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'template.csv')
    return FileResponse(file_path, media_type='text/csv', filename="template.csv")

@router.post("/receipt/scan", response_model=ReceiptParseResponse, status_code=status.HTTP_200_OK)
def scan_receipt(
    file: UploadFile = File(...)
):
    """
    Parse a receipt image file and return list of transactions.
    """
    try:
        content = file.file.read()
        items, total = parse_receipt(content)
        
        """
        At this point, the items and total are extracted from the receipt.
        The items are the specific line items on the receipt with a description and amount.
        The total is the total amount on the receipt.
        We can calculate an approximate "fees" amount by subtracting the sum of the items
        from the total.
        """
        approx_subtotal = sum([item['amount'] for item in items])
        approx_fees = round(total - approx_subtotal, 2)
        
        return ReceiptParseResponse(
            items=items,
            approx_subtotal=approx_subtotal,
            approx_fees=approx_fees,
            total=total
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/{transaction_id}", response_model=TransactionResponse)
def update_transaction(
    transaction_id: int,
    transaction_update: TransactionUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Update an existing transaction for the authenticated user.
    """
    tx = db.query(Transaction).filter(
        Transaction.id == transaction_id,
        Transaction.user_id == current_user.id
    ).first()
    if not tx:
        raise HTTPException(status_code=404, detail="Transaction not found")
    
    # If a new category is provided, validate it.
    if transaction_update.category_id is not None:
        category = db.query(Category).filter(
            Category.id == transaction_update.category_id,
            (Category.user_id == current_user.id) | (Category.user_id.is_(None))
        ).first()
        if not category:
            raise HTTPException(status_code=404, detail="Category not found")
        tx.category_id = transaction_update.category_id

    if transaction_update.amount is not None:
        tx.amount = transaction_update.amount
    if transaction_update.transaction_type is not None:
        tx.transaction_type = transaction_update.transaction_type
    if transaction_update.note is not None:
        tx.note = transaction_update.note
    if transaction_update.date is not None:
        tx.date = transaction_update.date
    if transaction_update.vendor is not None:
        tx.vendor = transaction_update.vendor

    db.commit()
    db.refresh(tx)
    
    from middlewares.goal_utils import recalc_goal_progress
    recalc_goal_progress(db, current_user.id, tx.category_id)
    recalc_goal_progress(db, current_user.id, None)
    
    return TransactionResponse.from_orm(tx)