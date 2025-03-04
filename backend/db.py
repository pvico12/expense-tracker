from sqlalchemy import create_engine
from sqlalchemy.orm import scoped_session, sessionmaker, declarative_base, joinedload
import os
from dotenv import load_dotenv
import utils
from models import Deal, User, Category, Transaction, TransactionType, Base
from typing import List, Optional
import datetime
from sqlalchemy.exc import SQLAlchemyError
import random

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

# region DB initialization
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

    print("DB SETUP: Populating predefined categories")
    create_predefined_categories()
    print("DB SETUP: Predefined categories populated successfully.")
    
    print("DB SETUP: Populating sample transactions")
    add_sample_transactions()
    print("DB SETUP: Sample transactions added successfully.")
    
    print("DB SETUP: Populating sample deals")
    create_sample_deals()
    print("DB SETUP: Sample deals created successfully.")
    
    

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
        print("Admin and team users created successfully.")
    except Exception as e:
        db_session.rollback()
        print(f"Error creating users: {e}")

def create_predefined_categories():
    """Create head categories and their predefined subcategories."""
    head_categories = {
        "Entertainment": [
            "Bowling", "Cinema", "Concert", "Electronics", "Entertainment",
            "Gym", "Hobby", "Nightclub", "Sports", "Subscription", "Vacation"
        ],
        "Food & Drinks": [
            "Candy", "Coffee", "Drinks", "Food", "Groceries", "Restaurant"
        ],
        "Housing": [
            "Bank", "Bills", "Electricity", "Home supplies", "Housing",
            "Insurance", "Internet", "Loan", "Maintenance", "Rent", "Service",
            "TV", "Taxes", "Telephone", "Water"
        ],
        "Income": [
            "Child benefit", "Income", "Interest", "Investment", "Pension", "Salary"
        ],
        "Lifestyle": [
            "Charity", "Child care", "Community", "Dentist", "Doctor", "Education",
            "Gifts", "Hotel", "Lifestyle", "Office expenses", "Pet", "Pharmacy",
            "Shopping", "Travel", "Work"
        ],
        "Miscellaneous": [
            "Bank cost", "Clothes", "Healthcare", "Miscellaneous", "Student loan", "Unknown"
        ],
        "Savings": [
            "Emergency savings", "Savings", "Vacation savings"
        ],
        "Transportation": [
            "Car costs", "Car insurance", "Car loan", "Flight", "Gas",
            "Parking", "Public transport", "Repair", "Taxi", "Transportation"
        ]
    }

    try:
        for head_cat_name, subcats in head_categories.items():
            head_cat = db_session.query(Category).filter_by(
                name=head_cat_name,
                user_id=None,
                parent_id=None
            ).first()
            if not head_cat:
                print(f"Creating head category: {head_cat_name}")
                head_cat = Category(name=head_cat_name)
                db_session.add(head_cat)
                db_session.flush()  # Get the head_category.id without committing

            for subcat_name in subcats:
                subcat = db_session.query(Category).filter_by(
                    name=subcat_name,
                    user_id=None,
                    parent_id=head_cat.id
                ).first()
                if not subcat:
                    print(f"  Adding subcategory: {subcat_name} under {head_cat_name}")
                    subcategory = Category(
                        name=subcat_name,
                        user_id=None,
                        parent_id=head_cat.id
                    )
                    db_session.add(subcategory)

        db_session.commit()
    except Exception as e:
        db_session.rollback()
        print(f"Error creating categories: {e}")

def add_sample_transactions():
    """Add sample transactions to the database."""
    try:
        # Fetch the admin user
        admin_user = db_session.query(User).filter_by(username='admin').first()
        if not admin_user:
            raise ValueError("Admin user does not exist.")

        # Fetch a category (assuming 'Food & Drinks' exists)
        food_category = db_session.query(Category).filter_by(name='Food & Drinks', parent_id=None).first()
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

def create_sample_deals():
    """Create a few random deals for each user."""
    try:
        users = db_session.query(User).all()
        if not users:
            raise ValueError("No users found in the database.")
        
        user_ids = [user.id for user in users]

        sample_deals = [
            {
                "name": "Discounted Coffee",
                "description": "50% off on all coffee varieties",
                "price": 2.50,
                "address": "123 Coffee St",
                "longitude": -122.4194,
                "latitude": 37.7749
            },
            {
                "name": "Gym Membership",
                "description": "20% off on annual membership",
                "price": 300.00,
                "address": "456 Fitness Ave",
                "longitude": -122.4194,
                "latitude": 37.7749
            },
            {
                "name": "Concert Tickets",
                "description": "Buy 1 Get 1 Free",
                "price": 75.00,
                "address": "789 Music Blvd",
                "longitude": -122.4194,
                "latitude": 37.7749
            }
        ]

        for deal_info in sample_deals:
            add_deal(
                user_id=random.choice(user_ids),
                name=deal_info["name"],
                description=deal_info["description"],
                price=round(deal_info["price"], 2),
                address=deal_info["address"],
                longitude=deal_info["longitude"],
                latitude=deal_info["latitude"]
            )

        print("Sample deals created successfully.")
    except Exception as e:
        db_session.rollback()
        print(f"Error creating sample deals: {e}")

def init_db():
    """Initialize and populate the database."""
    init_tables()
    fill_tables()

# endregion

def get_db():
    """Dependency that provides a database session."""
    try:
        yield db_session
    finally:
        db_session.remove()

# region Helpers
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

def get_transactions(user_id: int, limit: int = 100, offset: int = 0) -> List[Transaction]:
    """Retrieve transactions for a user."""
    try:
        return db_session.query(Transaction).filter_by(user_id=user_id).order_by(Transaction.date.desc()).limit(limit).offset(offset).all()
    except SQLAlchemyError as e:
        print(f"Error fetching transactions: {e}")
        return []

def add_category(name: str, user_id: Optional[int] = None, parent_id: Optional[int] = None) -> Category:
    """Add a new category."""
    try:
        if parent_id:
            parent_category = db_session.query(Category).filter_by(id=parent_id, parent_id=None).first()
            if not parent_category:
                raise ValueError("Parent category must be one of the predefined head categories.")

        category = Category(name=name, user_id=user_id, parent_id=parent_id)
        db_session.add(category)
        db_session.commit()
        db_session.refresh(category)
        return category
    except (SQLAlchemyError, ValueError) as e:
        db_session.rollback()
        print(f"Error adding category: {e}")
        raise

def get_head_categories_with_subcategories() -> List[Category]:
    """Get head categories along with their subcategories."""
    try:
        return db_session.query(Category).options(joinedload(Category.subcategories)).filter(
            Category.user_id.is_(None),
            Category.parent_id.is_(None)
        ).all()
    except SQLAlchemyError as e:
        print(f"Error fetching head categories: {e}")
        return []

def get_all_categories_for_user(user_id: Optional[int] = None) -> List[Category]:
    """Get all categories for a user, including global and user-specific subcategories."""
    try:
        head_cats = get_head_categories_with_subcategories()
        
        if user_id:
            user_subcats = db_session.query(Category).filter(
                Category.user_id == user_id
            ).all()
            
            user_subcat_map = {}
            for subcat in user_subcats:
                if subcat.parent_id not in user_subcat_map:
                    user_subcat_map[subcat.parent_id] = []
                user_subcat_map[subcat.parent_id].append(subcat)
            
            for head_cat in head_cats:
                head_cat.subcategories.extend(user_subcat_map.get(head_cat.id, []))
        
        return head_cats
    except SQLAlchemyError as e:
        print(f"Error fetching categories for user: {e}")
        return []

def get_deals(user_id: Optional[int] = None) -> List[Deal]:
    """Retrieve deals. Optionally for a specific user."""
    try:
        if user_id is None:
            return db_session.query(Deal).order_by(Deal.date.desc()).all()
        return db_session.query(Deal).filter_by(user_id=user_id).order_by(Deal.date.desc()).all()
    except SQLAlchemyError as e:
        print(f"Error fetching deals: {e}")
        return []

def add_deal(user_id: int, name: str, description: str, price: float,
             address: str, longitude: float, latitude: float, date: Optional[datetime.datetime] = datetime.datetime.utcnow()) -> Deal:
    """Add a new deal."""
    try:
        deal = Deal(
            user_id=user_id,
            name=name,
            description=description,
            price=round(price, 2),
            date=date,
            address=address,
            longitude=longitude,
            latitude=latitude
        )
        db_session.add(deal)
        db_session.commit()
        db_session.refresh(deal)
        return deal
    except SQLAlchemyError as e:
        db_session.rollback()
        print(f"Error adding deal: {e}")
        raise

def get_single_deal(deal_id: int) -> Optional[Deal]:
    """Retrieve a single deal by ID."""
    try:
        return db_session.query(Deal).filter_by(id=deal_id).first()
    except SQLAlchemyError as e:
        print(f"Error fetching deal: {e}")
        return None

# endregion