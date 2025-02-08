from sqlalchemy import create_engine
from sqlalchemy.orm import scoped_session, sessionmaker, declarative_base
import os
from dotenv import load_dotenv
import utils

load_dotenv()
DATABASE_URI = os.getenv('DATABASE_URI')

engine = create_engine(DATABASE_URI)
db_session = scoped_session(sessionmaker(autocommit=False,
                                         autoflush=False,
                                         bind=engine))
Base = declarative_base()
Base.query = db_session.query_property()

def test_connection():
    try:
        engine.connect()
        return True
    except Exception as e:
        print(f"Database connection failed: {e}")
        return False

def init_tables(reset=True):
    if reset:
        print("DB CLEANUP: Dropping all tables")
        Base.metadata.drop_all(bind=engine)
    
    print("DB SETUP: Creating all tables")
    import models
    Base.metadata.create_all(bind=engine)
    
def fill_tables():
    print("DB SETUP: Populating user table")
    from models import User
    user = db_session.query(User).filter_by(username='admin').first()
    if not user:
        print("Creating admin user")
        user = User(username='admin', password=utils.hash_password('admin'),
                    firstname='Admin', lastname='User', role='admin')
        db_session.add(user)
        
        print("Creating team users")
        db_session.add(User(username='petar', password=utils.hash_password('cs'),
                              firstname='Petar', lastname='Vico'))
        db_session.add(User(username='jack', password=utils.hash_password('cs'),
                              firstname='PuYuan', lastname='Li'))
        db_session.add(User(username='jeni', password=utils.hash_password('cs'),
                              firstname='Jennifer', lastname='Wu'))
        db_session.add(User(username='jia', password=utils.hash_password('cs'),
                              firstname='Jia', lastname='Li'))
        db_session.add(User(username='nicole', password=utils.hash_password('cs'),
                              firstname='Nicole', lastname='Planeta'))
        db_session.add(User(username='arif', password=utils.hash_password('cs'),
                              firstname='Ariful', lastname='Islam'))
        
        db_session.commit()

def init_db():
    init_tables()
    fill_tables()
    
def get_db():
    """Dependency that provides a database session."""
    try:
        yield db_session
    finally:
        db_session.remove()
