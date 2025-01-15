from sqlalchemy import create_engine
from sqlalchemy.orm import scoped_session, sessionmaker, declarative_base
import os
from dotenv import load_dotenv

load_dotenv()
DATABASE_URI = os.getenv('DATABASE_URI')

engine = create_engine(DATABASE_URI)
db_session = scoped_session(sessionmaker(autocommit=False,
                                         autoflush=False,
                                         bind=engine))
Base = declarative_base()
Base.query = db_session.query_property()

def init_tables(reset=True):
    # Delete all tables in the database (if reset is True)
    if reset:
        print("DB CLEANUP: Dropping all tables")
        Base.metadata.drop_all(bind=engine)
    
    # Create all tables defined in models.py
    print("DB SETUP: Creating all tables")
    import models
    Base.metadata.create_all(bind=engine)
    
def fill_tables():
    # Create a admin user if it does not exist
    print("DB SETUP: Populating user table")
    from models import User
    user = User.query.filter_by(name='admin').first()
    if not user:
        print("Creating admin user")
        user = User(name='admin')
        db_session.add(user)
        db_session.commit()

def init_db():
    init_tables()
    fill_tables()
    