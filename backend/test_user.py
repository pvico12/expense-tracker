import pytest
import uvicorn
import requests
from multiprocessing import Process
import time
from test_setup import *

@pytest.fixture(scope="module", autouse=True)
def create_target_user(server):
    """Create a new user before running the tests"""
    url = f"{BASE_URL}/auth/register"
    data = {
        "username": "targetuser",
        "password": "targetpassword",
        "firstname": "Target",
        "lastname": "User"
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 201
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_user_profile(server):
    """Test the /user/profile/{user_id} endpoint"""
    url = f"{BASE_URL}/user/profile/{get_user_id()}"
    
    try:
        response = requests.get(url, headers=get_headers())
        assert response.status_code == 200
        data = response.json()
        
        assert data["username"] == "targetuser"
        assert data["firstname"] == "Target"
        assert data["lastname"] == "User"
        
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_user_level_info(server):
    """Test the /user/level endpoint"""
    url = f"{BASE_URL}/user/level"
    
    try:
        response = requests.get(url, headers=get_headers())
        assert response.status_code == 200
        data = response.json()
        
        assert "level" in data
        assert "current_xp" in data
        assert "remaining_xp_until_next_level" in data
        assert "total_xp_for_next_level" in data
        
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_invalid_user_profile(server):
    """Test the /user/profile/{user_id} endpoint with an invalid user_id"""
    invalid_user_id = 99999
    url = f"{BASE_URL}/user/profile/{invalid_user_id}"
    
    try:
        response = requests.get(url, headers=get_headers())
        assert response.status_code == 401
        
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def get_unauthorized_user_profile(server):
    """Test the /user/profile/{user_id} endpoint without an auth token"""
    url = f"{BASE_URL}/user/profile/{get_user_id()}"
    
    try:
        response = requests.get(url)
        assert response.status_code == 401
        data = response.json()
        assert data["detail"] == "Not authenticated"
        
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_update_user_profile_invalid_input(server):
    """Test the /user/profile endpoint with invalid input"""
    url = f"{BASE_URL}/user/profile"
    data = {
        "firstname": "",
        "lastname": "",
        "username": ""
    }
    
    try:
        response = requests.put(url, json=data, headers=get_headers())
        assert response.status_code == 422
        data = response.json()
        assert "detail" in data
        
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_update_user_profile(server):
    """Test the /user/profile endpoint for updating user profile"""
    url = f"{BASE_URL}/user/profile"
    data = {
        "firstname": "UpdatedFirstName",
        "lastname": "UpdatedLastName",
        "username": "updatedusername"
    }
    
    try:
        response = requests.put(url, json=data, headers=get_headers())
        assert response.status_code == 200
        response_data = response.json()
        assert response_data["message"] == "Profile updated successfully"
        
        # Verify the update
        url = f"{BASE_URL}/user/profile/{get_user_id('updatedusername')}"
        response = requests.get(url, headers=get_headers('updatedusername'))
        assert response.status_code == 200
        data = response.json()
        
        assert data["firstname"] == "UpdatedFirstName"
        assert data["lastname"] == "UpdatedLastName"
        assert data["username"] == "updatedusername"
        
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
