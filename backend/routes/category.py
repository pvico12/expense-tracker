from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session
from typing import List, Optional

from http_models import CategoryResponse, CustomCategoryCreateRequest
from db import get_db, get_all_categories_for_user
from models import Category, User, Transaction
from dependencies.auth import get_current_user

router = APIRouter(
    prefix="/categories",
    tags=["categories"]
)

@router.get("/", response_model=List[CategoryResponse])
def get_categories(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Retrieve all categories (global and user-specific) for the authenticated user.
    """
    categories = get_all_categories_for_user(user_id=current_user.id)
    return [CategoryResponse.from_orm(cat) for cat in categories]

@router.post("/custom", response_model=CategoryResponse, status_code=status.HTTP_201_CREATED)
def create_custom_category(
    custom_category: CustomCategoryCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    try:
        new_category = Category(
            name=custom_category.name,
            color=custom_category.color,
            user_id=current_user.id
        )
        db.add(new_category)
        db.commit()
        db.refresh(new_category)
        
        return CategoryResponse.from_orm(new_category)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/{category_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_category(
    category_id: int,
    new_category_id: Optional[int] = None,  # New parameter to reassign transactions
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Delete a category by its ID for the authenticated user.
    """
    category = db.query(Category).filter(Category.id == category_id).first()
    print(category)
    
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    if category.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="You do not have permission to delete this category")
    
    # Check if there are any transactions associated with this category
    transaction_count = db.query(Transaction).filter(Transaction.category_id == category_id).count()
    if transaction_count > 0:
        if new_category_id is None:
            raise HTTPException(status_code=400, detail="Cannot delete category with existing transactions. Please provide a new category ID to reassign transactions.")
        
        # Reassign transactions to the new category
        db.query(Transaction).filter(Transaction.category_id == category_id).update({"category_id": new_category_id})
    
    db.delete(category)
    db.commit()
    return

@router.put("/{category_id}", response_model=CategoryResponse)
def update_category(
    category_id: int,
    category_data: CustomCategoryCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Update a category's details for the authenticated user.
    """
    category = db.query(Category).filter(Category.id == category_id, Category.user_id == current_user.id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    category.name = category_data.name
    category.color = category_data.color
    db.commit()
    db.refresh(category)
    
    return CategoryResponse.from_orm(category)
