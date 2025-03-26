import pytest
import uvicorn
import requests
from multiprocessing import Process
import time
from test_setup import *

@pytest.fixture(scope="module", autouse=True)
def create_user(server):
    """Create a new user before running the tests"""
    url = f"{BASE_URL}/auth/register"
    data = {
        "username": "newuser",
        "password": "newpassword",
        "firstname": "New",
        "lastname": "User"
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 201
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

@pytest.mark.skip(reason="Test is disabled")
def test_auto_categorization(server):
    """Test the /tools/categories/suggestion endpoint"""        
    # Login
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": "newuser",
        "password": "newpassword"
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 200
        login_response = response.json()
        assert "access_token" in login_response
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
        
    
    url = f"{BASE_URL}/tools/categories/suggestion"
    data = {
        "item_name": "Coffee"
    }
    headers = {
        "Authorization": f"Bearer {login_response['access_token']}"
    }
    
    try:
        response = requests.post(url, json=data, headers=headers)
        assert response.status_code == 200
        data = response.json()
        assert "category_id" in data
        assert "category_name" in data
        assert data["category_name"] == "Food & Drinks" # should be this since registration auto creates categories
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
        
def test_auto_categorization_invalid_input(server):
    """Test the /tools/categories/suggestion endpoint with invalid input"""        
    # Login
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": "newuser",
        "password": "newpassword"
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 200
        login_response = response.json()
        assert "access_token" in login_response
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
        
    
    url = f"{BASE_URL}/tools/categories/suggestion"
    data = {
        "item_name": ""
    }
    headers = {
        "Authorization": f"Bearer {login_response['access_token']}"
    }
    
    try:
        response = requests.post(url, json=data, headers=headers)
        assert response.status_code == 422
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")