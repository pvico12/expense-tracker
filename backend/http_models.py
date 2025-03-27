from pydantic import BaseModel, Field, validator, root_validator
from typing import Optional, List, Dict
import datetime
from models import TransactionType
from typing import Any

class LoginRequest(BaseModel):
    username: str = Field(..., min_length=1)
    password: str = Field(..., min_length=1)

class RegistrationRequest(BaseModel):
    username: str = Field(..., min_length=4)
    password: str = Field(..., min_length=6)
    firstname: str = Field(..., min_length=1)
    lastname: str = Field(..., min_length=1)
    
class TokenRefreshRequest(BaseModel):
    refresh_token: str = Field(..., min_length=1)

class FCMTokenUploadRequest(BaseModel):
    fcm_token: str = Field(..., min_length=1)

class CategoryCreateRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    color: Optional[str] = Field(None, regex=r'^#[0-9A-Fa-f]{6}$')

class TransactionCreateRequest(BaseModel):
    amount: float
    category_id: int
    transaction_type: TransactionType
    note: str
    date: Optional[datetime.datetime] = None
    vendor: Optional[str] = None

    @validator('transaction_type', pre=True)
    def parse_transaction_type(cls, v: Any) -> Any:
        if isinstance(v, str):
            return TransactionType(v.lower())
        return v
    
    @validator('amount', pre=True)
    def validate_amount(cls, v: Any) -> Any:
        if v is not None and v <= 0:
            raise ValueError("Amount must be greater than 0")
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
    color: Optional[str] = None

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
    item_name: str = Field(..., min_length=1)
    
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
    description: Optional[str]
    vendor: str
    price: float
    date: datetime.datetime
    address: str
    longitude: float
    latitude: float
    upvotes: Optional[int]
    downvotes: Optional[int]
    user_vote: Optional[int]
    maps_link: Optional[str]
    
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
    name: str = Field(..., min_length=1)
    description: str = Field(..., min_length=1)
    vendor: str = Field(..., min_length=1)
    price: float
    date: datetime.datetime
    address: str = Field(..., min_length=1)
    longitude: float
    latitude: float

    @validator('price', pre=True)
    def validate_price(cls, v: Any) -> Any:
        if v is not None and v <= 0:
            raise ValueError("Price must be greater than 0")
        return v

class DealUpdateRequest(BaseModel):
    name: Optional[str] = None  
    description: Optional[str] = None 
    vendor: Optional[str] = None
    price: Optional[float]
    date: Optional[datetime.datetime]
    address: Optional[str] = None
    longitude: Optional[float]
    latitude: Optional[float]

    @validator('price', pre=True)
    def validate_price(cls, v: Any) -> Any:
        if v is not None and v <= 0:
            raise ValueError("Price must be greater than 0")
        return v

class DealVoteResponse(BaseModel):
    upvotes: int
    downvotes: int
    
class UserProfileUpdateRequest(BaseModel):
    firstname: Optional[str] = Field(..., min_length=1)
    lastname: Optional[str] = Field(..., min_length=1)
    username: Optional[str] = Field(..., min_length=1)
    


# Needed for self-referencing models
CategoryResponse.update_forward_refs()

class RecurringTransactionCreateRequest(BaseModel):
    start_date: datetime.datetime
    end_date: datetime.datetime
    note: Optional[str] = None
    period: int  # in days
    amount: float
    category_id: int
    transaction_type: TransactionType
    vendor: Optional[str] = None

    @validator('amount', pre=True)
    def validate_amount(cls, v: Any) -> Any:
        if v is not None and v <= 0:
            raise ValueError("Amount must be greater than 0")
        return v
    
    @validator('period', pre=True)
    def validate_period(cls, v: Any) -> Any:
        if v is not None and v <= 0:
            raise ValueError("Period must be greater than 0")
        return v

class RecurringTransactionUpdateRequest(RecurringTransactionCreateRequest):
    # For now, the update fields mirror the creation request.
    pass

class RecurringTransactionResponse(BaseModel):
    id: int
    start_date: datetime.datetime
    end_date: datetime.datetime
    note: Optional[str]
    period: int

    class Config:
        orm_mode = True

class TransactionUpdateRequest(BaseModel):
    amount: Optional[float] = None
    category_id: Optional[int] = None
    transaction_type: Optional[TransactionType] = None
    note: Optional[str] = None
    date: Optional[datetime.datetime] = None
    vendor: Optional[str] = None

    @validator('transaction_type', pre=True)
    def parse_transaction_type(cls, v: Any) -> Any:
        if v is not None and isinstance(v, str):
            return TransactionType(v.lower())
        return v
    
    @validator('amount', pre=True)
    def validate_amount(cls, v: Any) -> Any:
        if v is not None and v <= 0:
            raise ValueError("Amount must be greater than 0")
        return v

class GoalCreateRequest(BaseModel):
    category_id: Optional[int] = None
    goal_type: str
    limit: float
    start_date: datetime.datetime
    period: int = Field(..., gt=0, description="The duration of the goal in days from the start_date")

    @validator("goal_type")
    def validate_goal_type(cls, v):
        if v not in ["amount", "percentage"]:
            raise ValueError("goal_type must be either 'amount' or 'percentage'")
        return v.lower()

    @root_validator
    def check_goal_specifics(cls, values):
        goal_type = values.get("goal_type")
        category_id = values.get("category_id")
        if goal_type == "percentage" and category_id is None:
            raise ValueError("Percentage goal must have a category_id")
        return values
    
    @validator('limit', pre=True)
    def validate_limit(cls, v: Any) -> Any:
        if v is not None and v <= 0:
            raise ValueError("Limit must be greater than 0")
        return v
    
    @validator('period', pre=True)
    def validate_period(cls, v: Any) -> Any:
        if v is not None and v <= 6:
            raise ValueError("Period must be greater than 6")
        return v

class GoalUpdateRequest(BaseModel):
    category_id: Optional[int] = None
    limit: Optional[float] = None
    start_date: Optional[datetime.datetime] = None
    end_date: Optional[datetime.datetime] = None
    goal_type: Optional[str] = None

    @validator("goal_type")
    def validate_goal_type(cls, v):
        if v and v not in ["amount", "percentage"]:
            raise ValueError("goal_type must be either 'amount' or 'percentage'")
        return v.lower()

    @root_validator
    def validate_dates(cls, values):
        start_date = values.get("start_date")
        end_date = values.get("end_date")
        if start_date and end_date and start_date > end_date:
            raise ValueError("start_date must be before or equal to end_date")
        return values

class GoalResponse(BaseModel):
    id: int
    category_id: Optional[int] = None
    goal_type: str
    limit: float
    start_date: datetime.datetime
    end_date: datetime.datetime
    period: int = 0
    on_track: bool
    time_left: int = 0
    amount_spent: Optional[float] = None

    class Config:
        orm_mode = True

    @validator("period", always=True, pre=True)
    def compute_period(cls, v, values):
        start_date = values.get("start_date")
        end_date = values.get("end_date")
        if start_date and end_date:
            return (end_date - start_date).days + 1
        return 0

    @validator("time_left", always=True, pre=True)
    def compute_time_left(cls, v, values):
        now = datetime.datetime.utcnow()
        end_date = values.get("end_date")
        if end_date:
            diff = (end_date - now).days
            return diff if diff > 0 else 0
        return 0

class GoalsStatisticsResponse(BaseModel):
    goals: List[GoalResponse]
    stats: Dict[str, int]

class HttpDealLocationSubscription(BaseModel):
    id: int
    user_id: int
    address: str
    longitude: float
    latitude: float
    
    class Config:
        orm_mode = True
    
class DealSubscriptionResponse(BaseModel):
    subs: List[HttpDealLocationSubscription]

class DealSubscriptionLocation(BaseModel):
    address: str
    longitude: float
    latitude: float
    
class DealSubscriptionLocationUpdateRequest(BaseModel):
    address: Optional[str] = None
    longitude: Optional[float] = None
    latitude: Optional[float] = None
