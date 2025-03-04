from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session

from http_models import CategorySuggestionRequest, CategorySuggestionResponse
from db import get_db, add_transaction, get_transactions as db_get_transactions, get_all_categories_for_user
from models import User
from utils import getCategorySuggestion
from dependencies.auth import get_current_user

router = APIRouter(
    prefix="/utils",
    tags=["utils"]
)
    
@router.post("/categories/suggestion", status_code=status.HTTP_200_OK)
def get_category_suggestion(
    category_suggestion_request: CategorySuggestionRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Get a category suggestion based on the item name.
    """
    try:
        categories = [category.name for category in get_all_categories_for_user(current_user.id)]
        suggestion = getCategorySuggestion(category_suggestion_request.item_name, categories)
        return suggestion
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    
