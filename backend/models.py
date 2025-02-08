from sqlalchemy import Column, Integer, String
from db import Base

class User(Base):
    __tablename__ = 'users'
    id = Column(Integer, primary_key=True)
    role = Column(String(50), nullable=False, default='user')
    username = Column(String(50), unique=True, nullable=False)
    password = Column(String(100), nullable=False)
    firstname = Column(String(50), nullable=False)
    lastname = Column(String(50), nullable=False)

    def __init__(self, username=None, password=None, firstname=None, lastname=None, role=None):
        self.username = username
        self.password = password
        self.firstname = firstname
        self.lastname = lastname
        self.role = role

    def __repr__(self):
        return f'<User {self.username!r}>'
    
    def getProfileInfo(self):
        return {
            'id': self.id,
            'username': self.username,
            'firstname': self.firstname,
            'lastname': self.lastname
        }