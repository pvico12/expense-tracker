import pytest
import uvicorn
import requests
from multiprocessing import Process
import time
import os
from test_setup import *

@pytest.fixture
def auth_headers(server):
    """
    Fixture to obtain authentication headers by logging in.
    If login fails, it attempts registration.
    """
    login_url = f"{BASE_URL}/auth/login"
    login_data = {"username": "newuser", "password": "newpassword"}
    try:
        response = requests.post(login_url, json=login_data)
        if response.status_code != 200:
            # Attempt registration if login fails
            reg_url = f"{BASE_URL}/auth/register"
            reg_data = {
                "username": "newuser",
                "password": "newpassword",
                "firstname": "Test",
                "lastname": "User"
            }
            reg_response = requests.post(reg_url, json=reg_data)
            if reg_response.status_code != 201:
                pytest.fail("User registration failed")
            response = requests.post(login_url, json=login_data)
        access_token = response.json().get("access_token")
        if not access_token:
            pytest.fail("No access token received")
        return {"Authorization": f"Bearer {access_token}"}
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_categories(server, auth_headers):
    """Test retrieving categories for the authenticated user"""
    url = f"{BASE_URL}/categories/"
    response = requests.get(url, headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)

def test_create_custom_category(server, auth_headers):
    """Test creation of a custom category"""
    url = f"{BASE_URL}/categories/custom"
    payload = {
        "name": "Test Category",
        "color": "#112233"
    }
    response = requests.post(url, json=payload, headers=auth_headers)
    assert response.status_code == 201
    data = response.json()
    assert data.get("name") == payload["name"]
    assert data.get("color") == payload["color"]
    assert "id" in data

def test_update_category(server, auth_headers):
    """Test updating an existing custom category"""
    # First, create a new category to update
    create_url = f"{BASE_URL}/categories/custom"
    payload = {
        "name": "Updatable Category",
        "color": "#445566"
    }
    create_resp = requests.post(create_url, json=payload, headers=auth_headers)
    assert create_resp.status_code == 201
    category = create_resp.json()
    category_id = category["id"]

    # Update the category
    update_url = f"{BASE_URL}/categories/{category_id}"
    update_payload = {
        "name": "Updated Category",
        "color": "#667788"
    }
    update_resp = requests.put(update_url, json=update_payload, headers=auth_headers)
    assert update_resp.status_code == 200
    updated_category = update_resp.json()
    assert updated_category.get("name") == update_payload["name"]
    assert updated_category.get("color") == update_payload["color"]

def test_delete_category(server, auth_headers):
    """Test deleting an existing custom category"""
    # Create a category to be deleted
    create_url = f"{BASE_URL}/categories/custom"
    payload = {
        "name": "Deletable Category",
        "color": "#778899"
    }
    create_resp = requests.post(create_url, json=payload, headers=auth_headers)
    assert create_resp.status_code == 201
    category = create_resp.json()
    category_id = category["id"]

    # Delete the category
    delete_url = f"{BASE_URL}/categories/{category_id}"
    delete_resp = requests.delete(delete_url, headers=auth_headers)
    assert delete_resp.status_code == 204

    # Verify that deleting again returns 404
    delete_resp2 = requests.delete(delete_url, headers=auth_headers)
    assert delete_resp2.status_code == 404

def test_delete_nonexistent_category(server, auth_headers):
    """Test deleting a category that does not exist"""
    url = f"{BASE_URL}/categories/999999"
    resp = requests.delete(url, headers=auth_headers)
    assert resp.status_code == 404

def test_update_nonexistent_category(server, auth_headers):
    """Test updating a category that does not exist"""
    url = f"{BASE_URL}/categories/999999"
    payload = {
        "name": "Nonexistent Category",
        "color": "#000000"
    }
    resp = requests.put(url, json=payload, headers=auth_headers)
    assert resp.status_code == 404
