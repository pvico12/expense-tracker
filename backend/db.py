from sqlalchemy import create_engine
from sqlalchemy.orm import scoped_session, sessionmaker, declarative_base, joinedload
import os
from dotenv import load_dotenv
from http_models import TransactionResponse
import utils
from models import Deal, DealLocationSubscription, DealVote, Goal, User, Category, Transaction, TransactionType, Base, UserLevelInfo
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
    
    print("DB SETUP: Populating sample transactions")
    add_sample_transactions()
    print("DB SETUP: Sample transactions added successfully.")
    
    print("DB SETUP: Populating sample deals")
    create_sample_deals()
    print("DB SETUP: Sample deals created successfully.")
    
    print("DB SETUP: Populating sample goals")
    create_sample_goals()
    print("DB SETUP: Sample goals created successfully.")
    
    print("DB SETUP: Populating sample deal subscriptions")
    create_sample_deal_subscriptions()
    print("DB SETUP: Sample deal subscriptions created successfully.")

def create_initial_users():
    """Create initial admin and team users."""
    try:
        admin = User(
            username='admin',
            password=utils.hash_password('admin'),
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
    except Exception as e:
        db_session.rollback()
        print(f"Error creating users: {e}")

def add_predefined_categories(user_id: int):
    """Assign predefined categories to a specific user."""
    categories = [
        {"name": "Entertainment", "color": "#FF9A3B3B"},
        {"name": "Food & Drinks", "color": "#FFC08261"},
        {"name": "Housing", "color": "#FFDBAD8C"},
        {"name": "Lifestyle", "color": "#FFFFEBCF"},
        {"name": "Miscellaneous", "color": "#FFFFCFAC"},
        {"name": "Savings", "color": "#FFFFDADA"},
        {"name": "Transportation", "color": "#FFD6CBAF"}
    ]

    try:
        for category_info in categories:
            # Check if the category already exists for the user
            category = db_session.query(Category).filter_by(
                name=category_info["name"],
                user_id=user_id
            ).first()
            if not category:
                category = Category(name=category_info["name"], color=category_info["color"], user_id=user_id)
                db_session.add(category)

        db_session.commit()
    except Exception as e:
        db_session.rollback()
        print(f"Error assigning categories to user {user_id}: {e}")

def add_sample_transactions():
    """Add sample transactions to the database."""
    try:
        # Fetch all users
        users = db_session.query(User).all()
        if not users:
            raise ValueError("No users found in the database.")

        # Define sample transactions for each category
        sample_transactions_data = {
            "Food & Drinks": [
                {"amount": 50.75, "note": "Dinner at Italian Restaurant", "vendor": "Italian Bistro"},
                {"amount": 20.00, "note": "Lunch at Cafe", "vendor": "Cafe Delight"},
                {"amount": 15.50, "note": "Groceries", "vendor": "Supermarket"}
            ],
            "Entertainment": [
                {"amount": 200.00, "note": "Concert tickets", "vendor": "Concert Hall"},
                {"amount": 50.00, "note": "Movie night", "vendor": "Cinema"},
                {"amount": 30.00, "note": "Amusement park", "vendor": "Fun Park"}
            ],
            "Transportation": [
                {"amount": 75.00, "note": "Gas for car", "vendor": "Gas Station"},
                {"amount": 50.00, "note": "Bus pass", "vendor": "City Transport"},
                {"amount": 100.00, "note": "Car maintenance", "vendor": "Auto Shop"}
            ],
            "Housing": [
                {"amount": 1200.00, "note": "Monthly Rent", "vendor": "Landlord"},
                {"amount": 100.00, "note": "Utilities", "vendor": "Utility Company"},
                {"amount": 50.00, "note": "Home repairs", "vendor": "Repair Service"}
            ],
            "Savings": [
                {"amount": 300.00, "note": "Savings deposit", "vendor": "Bank"},
                {"amount": 500.00, "note": "Emergency fund", "vendor": "Bank"}
            ],
            "Miscellaneous": [
                {"amount": 20.00, "note": "Stationery", "vendor": "Office Supplies"},
                {"amount": 10.00, "note": "Gift", "vendor": "Gift Shop"},
                {"amount": 5.00, "note": "Charity donation", "vendor": "Charity"}
            ],
            "Lifestyle": [
                {"amount": 50.00, "note": "Gym membership", "vendor": "Gym"},
                {"amount": 30.00, "note": "Spa treatment", "vendor": "Spa"},
                {"amount": 25.00, "note": "Haircut", "vendor": "Salon"}
            ]
        }

        # For each user, fetch their specific categories and add sample transactions
        for user in users:
            # Build a dictionary of user-specific categories
            user_categories = {}
            for category_name in sample_transactions_data.keys():
                category = db_session.query(Category).filter_by(name=category_name, user_id=user.id).first()
                if not category:
                    raise ValueError(f"Category {category_name} does not exist for user {user.id}.")
                user_categories[category_name] = category

            # Add transactions using the category IDs belonging to that specific user
            for category_name, transactions in sample_transactions_data.items():
                for transaction_info in transactions:
                    for month in range(1, 13):
                        sample_transaction = Transaction(
                            user_id=user.id,
                            amount=transaction_info["amount"],
                            category_id=user_categories[category_name].id,
                            transaction_type=TransactionType.EXPENSE,
                            note=transaction_info["note"],
                            date=datetime.datetime(datetime.datetime.utcnow().year, month, 15), # 15th day of the month
                            vendor=transaction_info["vendor"]
                        )
                        db_session.add(sample_transaction)

        db_session.commit()
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
            # Waterloo Deals
            {
                "name": "Discounted Coffee",
                "description": "50% off on all coffee varieties",
                "price": 2.50,
                "address": "247 King St N, Waterloo, ON N2J 2Y8",
                "longitude": -80.5252355084388, 
                "latitude": 43.4765187812192,
                "vendor": "Starbucks"
            },
            {
                "name": "Gym Membership",
                "description": "20% off on annual membership",
                "price": 300.00,
                "address": "560 Parkside Dr, Waterloo, ON N2L 5Z4",
                "longitude": -80.54331470097128, 
                "latitude": 43.49613260747481,
                "vendor": "Crunch Fitness"
            },
            {
                "name": "Concert Tickets",
                "description": "Buy 1 Get 1 Free",
                "price": 75.00,
                "address": "200 University Ave W, Waterloo, ON N2L 3G1",
                "longitude": -80.54130657364077,
                "latitude": 43.467625068716494,
                "vendor": "Humanities Theatre - UW"
            },
            {
                "name": "BOGO Pizza",
                "description": "Buy 1 Get 1 Free (Large)",
                "price": 15.00,
                "address": "160 University Ave W #2, Waterloo, ON N2L 3E9",
                "longitude": -80.53803732886678,
                "latitude": 43.47231281322054,
                "vendor": "Campus Pizza"
            },
            {
                "name": "Cheap Pizza Slices",
                "description": "$3 per slice",
                "price": 3.00,
                "address": "251 Hemlock St #110, Waterloo, ON N2L 0H2",
                "longitude": -80.53163653751186,
                "latitude": 43.47531462727487,
                "vendor": "Toma's Pizza"
            },
            
            # Vancouver Deals
            {
                "name": "Free Donut With Coffee",
                "description": "Free Donuts with the purchase of 1 medium coffee",
                "price": 2.00,
                "address": "2225 W 41st Ave, Vancouver, BC V6M 2A3",
                "longitude": -123.15772777161725,
                "latitude": 49.23740319158456,
                "vendor": "Tim Hortons"
            },
            {
                "name": "Discounted Movie Tickets",
                "description": "50% off on all movie tickets",
                "price": 10.00,
                "address": "452 SW Marine Dr, Vancouver, BC V5X 0C3",
                "longitude": -123.11591147675632,
                "latitude": 49.21276936478535,
                "vendor": "Cineplex"
            }
        ]

        # create deals
        for deal_info in sample_deals:
            add_deal(
                user_id=random.choice(user_ids),
                name=deal_info["name"],
                description=deal_info["description"],
                vendor=deal_info["vendor"],
                price=round(deal_info["price"], 2),
                address=deal_info["address"],
                longitude=deal_info["longitude"],
                latitude=deal_info["latitude"]
            )
        
        # create sample votes
        deals = db_session.query(Deal).all()
        for deal in deals:
            for user_id in user_ids:
                db_session.add(DealVote(deal_id=deal.id, user_id=user_id, vote=random.choice([1, -1])))
        db_session.commit()
    except Exception as e:
        db_session.rollback()
        print(f"Error creating sample deals: {e}")

def create_sample_deal_subscriptions():
    """Create sample deal subscriptions for each user."""
    try:
        users = db_session.query(User).all()
        if not users:
            raise ValueError("No users found in the database.")

        waterloo_coordinates = {"address": "Waterloo, ON", "latitude": 43.46422335402901, "longitude": -80.5209870181904}
        vancouver_coordinates = {"address": "Vancouver, BC", "latitude": 49.2816214189974, "longitude": -123.11757524562407} 

        for user in users:
            db_session.add(DealLocationSubscription(
                user_id=user.id,
                address=waterloo_coordinates["address"],
                latitude=waterloo_coordinates["latitude"],
                longitude=waterloo_coordinates["longitude"]
            ))
            db_session.add(DealLocationSubscription(
                user_id=user.id,
                address=vancouver_coordinates["address"],
                latitude=vancouver_coordinates["latitude"],
                longitude=vancouver_coordinates["longitude"]
            ))
            
        db_session.commit()
    except Exception as e:
        db_session.rollback()
        print(f"Error creating sample deal subscriptions: {e}")

def create_sample_goals():
    """Create a few sample goals for each user."""
    try:
        users = db_session.query(User).all()
        if not users:
            raise ValueError("No users found in the database.")
        
        sample_goals = [
            # 10% less on Entertainment this month
            {
                "goal_type": "percentage",
                "limit": 10,
                "category_name": "Entertainment",
                "start_date": datetime.datetime.utcnow().replace(day=1, hour=0, minute=0, second=0, microsecond=0),
                "end_date": datetime.datetime.utcnow().replace(day=1, hour=23, minute=59, second=59, microsecond=999999).replace(month=datetime.datetime.utcnow().month + 1) - datetime.timedelta(days=1)
            },
            # less than 20 this week on food and drink
            {
                "goal_type": "amount",
                "limit": 20,
                "category_name": "Food & Drinks",
                "start_date": datetime.datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0),
                "end_date": datetime.datetime.utcnow().replace(hour=23, minute=59, second=59, microsecond=999999) + datetime.timedelta(days=6)
            },
        ]

        for user in users:
            for goal_info in sample_goals:
                # find category id with the category name
                category = db_session.query(Category).filter_by(name=goal_info["category_name"], user_id=user.id).first()
                if not category:
                    raise ValueError(f"Category {goal_info['category_name']} does not exist for user {user.id}.")
                
                goal = Goal(
                    user_id=user.id,
                    category_id=category.id,
                    goal_type=goal_info["goal_type"],
                    limit=goal_info["limit"],
                    start_date=goal_info["start_date"],
                    end_date=goal_info["end_date"]
                )
                db_session.add(goal)
        
        db_session.commit()
    except Exception as e:
        db_session.rollback()
        print(f"Error creating sample goals: {e}")

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
                   date: Optional[datetime.datetime] = None, vendor: Optional[str] = None) -> Transaction:
    """Add a new transaction."""
    try:
        db_transaction = Transaction(
            user_id=user_id,
            amount=amount,
            category_id=category_id,
            transaction_type=transaction_type,
            note=note,
            date=date or datetime.datetime.utcnow(),
            vendor=vendor
        )
        db_session.add(db_transaction)
        db_session.commit()
        db_session.refresh(db_transaction)
        return db_transaction
    except SQLAlchemyError as e:
        db_session.rollback()
        print(f"Error adding transaction: {e}")
        raise

def get_transactions(user_id: int, limit: int = 100, offset: int = 0, 
                     start_date: Optional[datetime.datetime] = None, 
                     end_date: Optional[datetime.datetime] = None) -> List[TransactionResponse]:
    """Retrieve transactions for a user, including category names, with optional date filters."""
    try:
        query = db_session.query(Transaction).filter_by(user_id=user_id)
        
        if start_date:
            query = query.filter(Transaction.date >= start_date)
        if end_date:
            query = query.filter(Transaction.date <= end_date)
        
        return query.order_by(Transaction.date.desc()).limit(limit).offset(offset).all()
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
             address: str, longitude: float, latitude: float,
             date: Optional[datetime.datetime] = datetime.datetime.utcnow(),
             vendor: Optional[str] = "") -> Deal:
    """Add a new deal."""
    try:
        deal = Deal(
            user_id=user_id,
            name=name,
            description=description,
            vendor=vendor,
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
    
def get_user_level_info(user_id: int) -> dict:
    """Get the user's level and XP information."""
    user = db_session.query(User).filter_by(id=user_id).first()
    if not user:
        return
    
    score = user.xp
    level = 1
    xp_for_next_level = 5

    while score >= xp_for_next_level:
        score -= xp_for_next_level
        level += 1
        xp_for_next_level *= 2

    current_xp = score
    remaining_xp_for_next_level = xp_for_next_level - score

    return UserLevelInfo(
        level=level,
        current_xp=current_xp,
        remaining_xp_until_next_level=remaining_xp_for_next_level,
        total_xp_for_next_level=xp_for_next_level
    )

# endregion