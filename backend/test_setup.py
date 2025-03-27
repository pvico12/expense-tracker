import pytest
import uvicorn
import requests
from multiprocessing import Process
import time

BASE_URL = "http://localhost:8000"

def run_server():
    """Function to run the FastAPI server in a separate process"""
    uvicorn.run("app:app", host="0.0.0.0", port=8000, log_level="info")

@pytest.fixture(scope="module")
def server():
    """Fixture to start and stop the server"""
    proc = Process(target=run_server)
    proc.start()
    time.sleep(10)
    
    print("Server started")
    
    # Check if the server is running
    try:
        response = requests.get(f"{BASE_URL}/healthcheck")
        if response.status_code != 200:
            pytest.fail("Server health check failed")
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server for health check")
        
        
    yield
    proc.terminate()
    proc.join()

def get_user_id(username="targetuser", password="targetpassword"):
    # Login
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": username,
        "password": password
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 200
        login_response = response.json()
        assert "access_token" in login_response
        target_user_auth_token = login_response["access_token"]
        
        # decode access token
        import jwt
        from dotenv import load_dotenv
        import os
        load_dotenv()
        JWT_ACCESS_TOKEN_SECRET = os.getenv("JWT_ACCESS_TOKEN_SECRET")
        if JWT_ACCESS_TOKEN_SECRET is None:
            pytest.fail("JWT_ACCESS_TOKEN_SECRET is not set in the .env file")
        
        token_data = jwt.decode(target_user_auth_token, JWT_ACCESS_TOKEN_SECRET, algorithms=['HS256'])
        return token_data["user_id"]
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
        
    return -1

def get_headers(username="targetuser", password="targetpassword"):
    # Login
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": username,
        "password": password
    }
    
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 200
        login_response = response.json()
        assert "access_token" in login_response
        target_user_auth_token = login_response["access_token"]
        
        return {"Authorization": f"Bearer {target_user_auth_token}"}
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
        
    return {}