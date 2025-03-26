import pytest
import uvicorn
import requests
from multiprocessing import Process
import time
from test_setup import *

@pytest.mark.run(order=1)
def test_register(server):
    """Test the /auth/register endpoint"""
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
        data = response.json()
        assert "message" in data
        assert data["message"] == "User created successfully"
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_register_existing_user(server):
    """Test the /auth/register endpoint with an existing user"""
    url = f"{BASE_URL}/auth/register"
    data = {
        "username": "newuser",
        "password": "newpassword",
        "firstname": "New",
        "lastname": "User 2"
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 400  # Bad Request
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_register_invalid_input(server):
    """Test the /auth/register endpoint with invalid input"""
    url = f"{BASE_URL}/auth/register"
    invalid_data = [
        {},  # Empty data
        {"username": "newuser"},  # Missing fields
        {"username": "a", "password": "newpassword", "firstname": "New", "lastname": "User"},  # Username too short
        {"username": "newuser2", "password": "short", "firstname": "New", "lastname": "User"},  # Password too short
        {"username": "newuser2", "password": "newpassword", "firstname": "", "lastname": "User"},  # Firstname too short
        {"username": "newuser2", "password": "newpassword", "firstname": "New", "lastname": ""},  # Lastname too short
    ]
    
    for data in invalid_data:
        try:
            response = requests.post(url, json=data)
            assert response.status_code == 422  # Unprocessable Entity
        except requests.exceptions.ConnectionError:
            pytest.fail("Could not connect to the server")

def test_login(server):
    """Test the /auth/login endpoint"""
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": "newuser",
        "password": "newpassword"
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        assert data["message"] == "Login successful"
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_login_invalid_username(server):
    """Test the /auth/login endpoint with an invalid username"""
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": "wronguser",
        "password": "newpassword"
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 401  # Unauthorized
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_login_invalid_password(server):
    """Test the /auth/login endpoint with an invalid password"""
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": "newuser",
        "password": "wrongpassword"
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 401  # Unauthorized
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_login_missing_params(server):
    """Test the /auth/login endpoint with missing parameters"""
    url = f"{BASE_URL}/auth/login"
    invalid_data = [
        {},  # Empty data
        {"username": "newuser"},  # Missing password
        {"password": "newpassword"}  # Missing username
    ]
    
    for data in invalid_data:
        try:
            response = requests.post(url, json=data)
            assert response.status_code == 422  # Unprocessable Entity
        except requests.exceptions.ConnectionError:
            pytest.fail("Could not connect to the server")


def test_refresh_token(server):
    """Test the /auth/refresh endpoint"""
    login_url = f"{BASE_URL}/auth/login"
    refresh_url = f"{BASE_URL}/auth/refresh"
    login_data = {
        "username": "newuser",
        "password": "newpassword"
    }
    
    try:
        login_response = requests.post(login_url, json=login_data)
        assert login_response.status_code == 200
        refresh_token = login_response.json()["refresh_token"]
        
        response = requests.post(refresh_url, json={"refresh_token": refresh_token})
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        assert data["message"] == "Token refreshed"
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_refresh_token_invalid_input(server):
    """Test the /auth/refresh endpoint with invalid input"""
    url = f"{BASE_URL}/auth/refresh"
    invalid_data = [
        {"refresh_token": "invalidtoken"}
    ]
    
    for data in invalid_data:
        try:
            response = requests.post(url, json=data)
            assert response.status_code == 401
        except requests.exceptions.ConnectionError:
            pytest.fail("Could not connect to the server")

def test_refresh_token_missing_params(server):
    """Test the /auth/refresh endpoint with missing parameters"""
    url = f"{BASE_URL}/auth/refresh"
    invalid_data = [
        {},  # Empty data
    ]
    
    for data in invalid_data:
        try:
            response = requests.post(url, json=data)
            assert response.status_code == 422  # Unprocessable Entity
        except requests.exceptions.ConnectionError:
            pytest.fail("Could not connect to the server")


def test_send_fcm_token(server):
    """Test the /auth/fcm_token endpoint"""
    login_url = f"{BASE_URL}/auth/login"
    fcm_token_url = f"{BASE_URL}/auth/fcm_token"
    login_data = {
        "username": "newuser",
        "password": "newpassword"
    }
    
    try:
        login_response = requests.post(login_url, json=login_data)
        assert login_response.status_code == 200
        access_token = login_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {access_token}"}
        
        response = requests.post(fcm_token_url, json={"fcm_token": "test_fcm_token"}, headers=headers)
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        assert data["message"] == "Token added successfully"
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_send_fcm_token_invalid_fcm_token(server):
    """Test the /auth/fcm_token endpoint with invalid FCM token"""
    login_url = f"{BASE_URL}/auth/login"
    fcm_token_url = f"{BASE_URL}/auth/fcm_token"
    login_data = {
        "username": "newuser",
        "password": "newpassword"
    }
    
    try:
        login_response = requests.post(login_url, json=login_data)
        assert login_response.status_code == 200
        access_token = login_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {access_token}"}
        
        invalid_data = [
            {},
            {"fcm_token": ""},
        ]
        
        for data in invalid_data:
            response = requests.post(fcm_token_url, json=data, headers=headers)
            assert response.status_code == 422 
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
        
def send_fcm_token_missing_auth_token(server):
    """Test the /auth/fcm_token endpoint with missing Authorization header"""
    fcm_token_url = f"{BASE_URL}/auth/fcm_token"
    data = {"fcm_token": "test_fcm_token"}
    
    try:
        response = requests.post(fcm_token_url, json=data)
        assert response.status_code == 401
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
