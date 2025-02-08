from pydantic import BaseModel, Field

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
