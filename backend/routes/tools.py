from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session

from http_models import CategorySuggestionRequest, CategorySuggestionResponse
from db import get_db, add_transaction, get_transactions as db_get_transactions, get_all_categories_for_user
from models import User
from utils import predict_category, get_category_by_name
from dependencies.auth import get_current_user

router = APIRouter(
    prefix="/tools",
    tags=["tools"]
)
    
@router.post("/categories/suggestion", response_model=CategorySuggestionResponse, status_code=status.HTTP_200_OK)
def get_category_suggestion(
    category_suggestion_request: CategorySuggestionRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Get a category suggestion based on the item name.
    """
    try:
        categories = get_all_categories_for_user(current_user.id)
        category_names = [category.name for category in categories]
        category_suggestion = predict_category(category_suggestion_request.item_name, category_names)
        
        if "NONE" in category_suggestion.upper():
            raise Exception("No category suggestion found.")
        
        # now get id of category whos name matches suggestion
        category_id = get_category_by_name(category_suggestion, categories)
        if not category_id:
            raise Exception(f"Category '{category_suggestion}' not found.")
        return CategorySuggestionResponse(category_id=int(category_id), category_name=category_suggestion)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    
