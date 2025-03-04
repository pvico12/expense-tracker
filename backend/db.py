from sqlalchemy import create_engine
from sqlalchemy.orm import scoped_session, sessionmaker, declarative_base, joinedload
import os
from dotenv import load_dotenv
from http_models import TransactionResponse
import utils
from models import User, Category, Transaction, TransactionType, Base
from typing import List, Optional
import datetime
from sqlalchemy.exc import SQLAlchemyError

load_dotenv()
DATABASE_URI = os.getenv('DATABASE_URI')

engine = create_engine(DATABASE_URI)
db_session = scoped_session(sessionmaker(autocommit=False,
                                         autoflush=False,
                                         bind=engine))
Base.query = db_session.query_property()

def test_connection() -> bool:
    """Test the database connection."""
    try:
        engine.connect()
        return True
    except Exception as e:
        print(f"Database connection failed: {e}")
        return False

def init_tables(reset: bool = True):
    """Initialize the database tables."""
    if reset:
        print("DB CLEANUP: Dropping all tables")
        Base.metadata.drop_all(bind=engine)
    print("DB SETUP: Creating all tables")
    Base.metadata.create_all(bind=engine)

def fill_tables():
    """Populate the database with initial data."""
    print("DB SETUP: Populating user table")
    admin_user = db_session.query(User).filter_by(username='admin').first()
    if not admin_user:
        create_initial_users()

def create_initial_users():
    """Create initial admin and team users."""
    try:
        admin = User(
            username='admin',
            password=utils.hash_password('admin'),  # Ensure you have a hash_password function
            firstname='Admin',
            lastname='User',
            role='admin'
        )
        db_session.add(admin)
        db_session.commit()
        db_session.refresh(admin)  # Refresh to get the admin's ID
        add_predefined_categories(admin.id)  # Assign predefined categories to admin

        team_users = [
            {'username': 'petar', 'password': 'cs', 'firstname': 'Petar', 'lastname': 'Vico'},
            {'username': 'jack', 'password': 'cs', 'firstname': 'PuYuan', 'lastname': 'Li'},
            {'username': 'jeni', 'password': 'cs', 'firstname': 'Jennifer', 'lastname': 'Wu'},
            {'username': 'jia', 'password': 'cs', 'firstname': 'Jia', 'lastname': 'Li'},
            {'username': 'nicole', 'password': 'cs', 'firstname': 'Nicole', 'lastname': 'Planeta'},
            {'username': 'arif', 'password': 'cs', 'firstname': 'Ariful', 'lastname': 'Islam'},
        ]

        for user_info in team_users:
            user = User(
                username=user_info['username'],
                password=utils.hash_password(user_info['password']),
                firstname=user_info['firstname'],
                lastname=user_info['lastname']
            )
            db_session.add(user)
            db_session.commit()
            db_session.refresh(user)  # Refresh to get the user's ID
            add_predefined_categories(user.id)  # Assign predefined categories to each team user

        print("Admin and team users created successfully.")
    except Exception as e:
        db_session.rollback()
        print(f"Error creating users: {e}")

def add_predefined_categories(user_id: int):
    """Assign predefined categories to a specific user."""
    categories = [
        {"name": "Entertainment", "color": "#FF5733"},
        {"name": "Food & Drinks", "color": "#33FF57"},
        {"name": "Housing", "color": "#3357FF"},
        {"name": "Income", "color": "#FF33A1"},
        {"name": "Lifestyle", "color": "#FF8C33"},
        {"name": "Miscellaneous", "color": "#8C33FF"},
        {"name": "Savings", "color": "#33FFF5"},
        {"name": "Transportation", "color": "#FF3333"}
    ]

    try:
        for category_info in categories:
            # Check if the category already exists for the user
            category = db_session.query(Category).filter_by(
                name=category_info["name"],
                user_id=user_id
            ).first()
            if not category:
                print(f"Creating category for user {user_id}: {category_info['name']}")
                category = Category(name=category_info["name"], color=category_info["color"], user_id=user_id)
                db_session.add(category)

        db_session.commit()
        print(f"Predefined categories assigned to user {user_id} successfully.")
    except Exception as e:
        db_session.rollback()
        print(f"Error assigning categories to user {user_id}: {e}")

def add_sample_transactions():
    """Add sample transactions to the database."""
    try:
        # Fetch the admin user
        admin_user = db_session.query(User).filter_by(username='admin').first()
        if not admin_user:
            raise ValueError("Admin user does not exist.")

        # Fetch a category (assuming 'Food & Drinks' exists)
        food_category = db_session.query(Category).filter_by(name='Food & Drinks').first()
        if not food_category:
            raise ValueError("Food & Drinks category does not exist.")

        # Add a sample expense transaction
        sample_transaction = Transaction(
            user_id=admin_user.id,
            amount=50.75,
            category_id=food_category.id,
            transaction_type=TransactionType.EXPENSE,
            note="Dinner at Italian Restaurant",
            date=datetime.datetime.utcnow()
        )
        db_session.add(sample_transaction)

        # Add a sample income transaction
        sample_income = Transaction(
            user_id=admin_user.id,
            amount=1500.00,
            category_id=food_category.id,  # Assuming income can also be categorized under 'Income'
            transaction_type=TransactionType.INCOME,
            note="Monthly Salary",
            date=datetime.datetime.utcnow()
        )
        db_session.add(sample_income)

        db_session.commit()
        print("Sample transactions added successfully.")
    except Exception as e:
        db_session.rollback()
        print(f"Error adding sample transactions: {e}")

def init_db():
    """Initialize and populate the database."""
    init_tables()
    fill_tables()
    add_sample_transactions()

def get_db():
    """Dependency that provides a database session."""
    try:
        yield db_session
    finally:
        db_session.remove()

# CRUD Helper Functions

def add_transaction(user_id: int, amount: float, category_id: int,
                   transaction_type: TransactionType, note: Optional[str] = None,
                   date: Optional[datetime.datetime] = None) -> Transaction:
    """Add a new transaction."""
    try:
        db_transaction = Transaction(
            user_id=user_id,
            amount=amount,
            category_id=category_id,
            transaction_type=transaction_type,
            note=note,
            date=date or datetime.datetime.utcnow()
        )
        db_session.add(db_transaction)
        db_session.commit()
        db_session.refresh(db_transaction)
        return db_transaction
    except SQLAlchemyError as e:
        db_session.rollback()
        print(f"Error adding transaction: {e}")
        raise

def get_transactions(user_id: int, limit: int = 100, offset: int = 0) -> List[TransactionResponse]:
    """Retrieve transactions for a user, including category names."""
    try:
        return db_session.query(Transaction).filter_by(user_id=user_id).order_by(Transaction.date.desc()).limit(limit).offset(offset).all()
    except SQLAlchemyError as e:
        print(f"Error fetching transactions: {e}")
        return []

def add_category(name: str, user_id: Optional[int] = None, color: Optional[str] = None) -> Category:
    """Add a new category."""
    try:
        category = Category(name=name, user_id=user_id, color=color)
        db_session.add(category)
        db_session.commit()
        db_session.refresh(category)
        return category
    except SQLAlchemyError as e:
        db_session.rollback()
        print(f"Error adding category: {e}")
        raise

def get_all_categories_for_user(user_id: Optional[int] = None) -> List[Category]:
    """Get all categories for a user, including global and user-specific categories."""
    try:
        # Fetch all categories that are either global (user_id is None) or specific to the user
        categories = db_session.query(Category).filter(
            (Category.user_id == user_id) | (Category.user_id.is_(None))
        ).all()
        
        return categories
    except SQLAlchemyError as e:
        print(f"Error fetching categories for user: {e}")
        return []
