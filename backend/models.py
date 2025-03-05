# models.py
from typing import Optional
from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Enum as SQLEnum, Boolean
from sqlalchemy.orm import relationship, backref
from base import Base  # Import Base from base.py
import enum
import datetime

class TransactionType(enum.Enum):
    EXPENSE = "expense"
    INCOME = "income"
    TRANSFER = "transfer"
    # You can add more specific transaction types if needed
    # For example:
    # SAVINGS = "savings"
    # INVESTMENT = "investment"

class Category(Base):
    __tablename__ = 'categories'
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50), nullable=False)
    user_id = Column(Integer, ForeignKey('users.id'), nullable=True)  # Null for global categories
    color = Column(String(7), nullable=True)

    user = relationship("User", back_populates="categories")
    transactions = relationship("Transaction", back_populates="category")

    def __repr__(self):
        return f'<Category {self.name!r}>'

class Transaction(Base):
    __tablename__ = 'transactions'
    id = Column(Integer, primary_key=True, index=True)
    amount = Column(Float, nullable=False)
    category_id = Column(Integer, ForeignKey('categories.id'), nullable=False)
    transaction_type = Column(SQLEnum(TransactionType, name="transactiontype"), nullable=False)
    user_id = Column(Integer, ForeignKey('users.id'), nullable=False)
    note = Column(String(255), nullable=True)
    date = Column(DateTime, default=datetime.datetime.utcnow, nullable=False)
    
    # Change the vendor field to be defined as a column
    vendor = Column(String, nullable=True)
    
    # Recurring ID column and relationship remain unchanged
    recurring_id = Column(Integer, ForeignKey('recurring_transactions.id'), nullable=True)
    recurring = relationship("RecurringTransaction", backref="transactions", foreign_keys=[recurring_id])
    
    user = relationship("User", back_populates="transactions")
    category = relationship("Category", back_populates="transactions")
    
    def __repr__(self):
        return f'<Transaction {self.id} - {self.amount}>'

# Update User model to include relationships
class User(Base):
    __tablename__ = 'users'
    id = Column(Integer, primary_key=True, index=True)
    role = Column(String(50), nullable=False, default='user')
    username = Column(String(50), unique=True, nullable=False)
    password = Column(String(100), nullable=False)
    firstname = Column(String(50), nullable=False)
    lastname = Column(String(50), nullable=False)

    categories = relationship("Category", back_populates="user", cascade="all, delete-orphan")
    transactions = relationship("Transaction", back_populates="user", cascade="all, delete-orphan")

    def __init__(self, username=None, password=None, firstname=None, lastname=None, role=None):
        self.username = username
        self.password = password
        self.firstname = firstname
        self.lastname = lastname
        self.role = role if role is not None else 'user'

    def __repr__(self):
        return f'<User {self.username!r}>'
    
    def getProfileInfo(self):
        return {
            'id': self.id,
            'username': self.username,
            'firstname': self.firstname,
            'lastname': self.lastname
        }
        
        
class Deal(Base):
    __tablename__ = 'deals'
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50), nullable=False)
    description = Column(String(255), nullable=False)
    price = Column(Float, nullable=False)
    user_id = Column(Integer, ForeignKey('users.id'), nullable=False)
    date = Column(DateTime, default=datetime.datetime.utcnow, nullable=False)
    address = Column(String(255), nullable=False)
    longitude = Column(Float, nullable=False)
    latitude = Column(Float, nullable=False)

    def __repr__(self):
        return f'<Deal {self.name!r}>'
    
class DealVote(Base):
    __tablename__ = 'deal_votes'
    id = Column(Integer, primary_key=True, index=True)
    deal_id = Column(Integer, ForeignKey('deals.id'), nullable=False)
    user_id = Column(Integer, ForeignKey('users.id'), nullable=False)
    vote = Column(Integer, nullable=False)  # 1 for upvote, -1 for downvote

    def __repr__(self):
        return f'<DealVote {self.vote!r}>'

class RecurringTransaction(Base):
    __tablename__ = 'recurring_transactions'
    id = Column(Integer, primary_key=True, index=True)
    start_date = Column(DateTime, nullable=False)
    end_date = Column(DateTime, nullable=True)  # Could be optional if it goes on indefinitely
    note = Column(String(255), nullable=True)
    period = Column(Integer, nullable=False)  # period in days
    user_id = Column(Integer, ForeignKey('users.id'), nullable=False)

    # Establish relationship with the User
    user = relationship("User", backref=backref("recurring_transactions", cascade="all, delete-orphan"))
    
    def __repr__(self):
        return f'<RecurringTransaction {self.id} for user {self.user_id}>'