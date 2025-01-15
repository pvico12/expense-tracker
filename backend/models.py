from sqlalchemy import Column, Integer, String
from db import Base

# Basic User model to check for database connectivity and interfacing
class User(Base):
    __tablename__ = 'users'
    id = Column(Integer, primary_key=True)
    name = Column(String(50), unique=True)

    def __init__(self, name=None):
        self.name = name

    def __repr__(self):
        return f'<User {self.name!r}>'