from pydantic import BaseModel, Field, validator
from typing import Optional, List, Dict
import datetime
from models import TransactionType
from typing import Any

class LoginRequest(BaseModel):
    username: str = Field(..., min_length=1)
    password: str = Field(..., min_length=1)

class RegistrationRequest(BaseModel):
    username: str = Field(..., min_length=1)
    password: str = Field(..., min_length=1)
    firstname: str = Field(..., min_length=1)
    lastname: str = Field(..., min_length=1)
    
class TokenRefreshRequest(BaseModel):
    refresh_token: str = Field(..., min_length=1)

class CategoryCreateRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    color: Optional[str] = Field(None, regex=r'^#[0-9A-Fa-f]{6}$')

class TransactionCreateRequest(BaseModel):
    amount: float
    category_id: int
    transaction_type: TransactionType
    note: str
    date: Optional[datetime.datetime] = None

    @validator('transaction_type', pre=True)
    def parse_transaction_type(cls, v: Any) -> Any:
        if isinstance(v, str):
            return TransactionType(v.lower())
        return v

# Response Models

class UserResponse(BaseModel):
    id: int
    username: str
    firstname: str
    lastname: str
    role: str

    class Config:
        orm_mode = True

class CategoryResponse(BaseModel):
    id: int
    name: str
    color: Optional[str]

    class Config:
        orm_mode = True

class TransactionResponse(BaseModel):
    id: int
    user_id: int
    amount: float
    category_id: int
    transaction_type: TransactionType
    note: Optional[str] = None
    date: datetime.datetime
    vendor: Optional[str] = None

    class Config:
        orm_mode = True

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"

    class Config:
        orm_mode = True

class CategoryStats(BaseModel):
    category_name: str
    total_amount: float
    percentage: float

class SummaryResponse(BaseModel):
    total_spend: float
    category_breakdown: List[CategoryStats]
    transaction_history: List[TransactionResponse]

    class Config:
        orm_mode = True

class SummaryCategoryResponse(BaseModel):
    type_totals: Dict[str, float]
    transaction_history: List[TransactionResponse]
    
    class Config:
        orm_mode = True

class CustomCategoryCreateRequest(BaseModel):
    name: str
    color: Optional[str] = Field(None, regex=r'^#[0-9A-Fa-f]{6}$')

# === Category Suggestions ===
class CategorySuggestionRequest(BaseModel):
    item_name: str
    
class CategorySuggestionResponse(BaseModel):
    category_id: int
    category_name: str
    
# === Receipt Parsing ===
class ReceiptParseResponse(BaseModel):
    items: List[Dict[str, Any]]
    approx_subtotal: float
    approx_fees: float
    total: float

# === Deals ====
class HttpDeal(BaseModel):
    id: Optional[int]
    name: str
    description: str
    price: float
    date: datetime.datetime
    address: str
    longitude: float
    latitude: float
    
    class Config:
        orm_mode = True

class LocationFilter(BaseModel):
    longitude: float
    latitude: float
    distance: float
    
class DealRetrievalRequest(BaseModel):
    user_id: Optional[int] = None
    location: Optional[LocationFilter] = None

class DealCreationRequest(BaseModel):
    name: str
    description: str
    price: float
    date: datetime.datetime
    address: str
    longitude: float
    latitude: float

class DealUpdateRequest(BaseModel):
    name: Optional[str]
    description: Optional[str]
    price: Optional[float]
    date: Optional[datetime.datetime]
    address: Optional[str]
    longitude: Optional[float]
    latitude: Optional[float]

class DealVoteResponse(BaseModel):
    upvotes: int
    downvotes: int
    


# Needed for self-referencing models
CategoryResponse.update_forward_refs()