import pytest
import uvicorn
import requests
from multiprocessing import Process
import time
from datetime import datetime, timedelta
import os
from test_setup import *

@pytest.fixture
def auth_headers(server):
    """
    Fixture to obtain authentication headers by logging in.
    If the login fails, it attempts to register the user.
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
            reg_resp = requests.post(reg_url, json=reg_data)
            if reg_resp.status_code != 201:
                pytest.fail("User registration failed")
            response = requests.post(login_url, json=login_data)
        access_token = response.json().get("access_token")
        if not access_token:
            pytest.fail("No access token received")
        return {"Authorization": f"Bearer {access_token}"}
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def create_custom_category(auth_headers, name="Test Category", color="#123456"):
    """
    Helper function to create a custom category.
    This is used to get a valid category_id for recurring transactions.
    """
    url = f"{BASE_URL}/categories/custom"
    payload = {"name": name, "color": color}
    response = requests.post(url, json=payload, headers=auth_headers)
    response.raise_for_status()
    return response.json()

def test_get_recurring_transactions(server, auth_headers):
    """Test retrieving a list of recurring transactions"""
    url = f"{BASE_URL}/transactions/recurring/"
    response = requests.get(url, headers=auth_headers)
    assert response.status_code == 200, response.text
    data = response.json()
    assert isinstance(data, list), "Expected a list of recurring transactions"

def test_create_recurring_transaction(server, auth_headers):
    """Test creating a recurring transaction"""
    # Create a valid category first
    category = create_custom_category(auth_headers, name="Recurring Category", color="#a1b2c3")
    category_id = category.get("id")

    url = f"{BASE_URL}/transactions/recurring/"
    now = datetime.utcnow().replace(microsecond=0)
    start_date = now.isoformat()
    end_date = (now + timedelta(days=30)).isoformat()
    payload = {
        "start_date": start_date,
        "end_date": end_date,
        "note": "Recurring transaction test",
        "period": 7,  # recurring every 7 days
        "amount": 50.0,
        "category_id": category_id,
        "transaction_type": "expense",
        "vendor": "Test Vendor"
    }
    response = requests.post(url, json=payload, headers=auth_headers)
    assert response.status_code == 201, response.text
    data = response.json()
    assert "id" in data, "Expected recurring transaction id in response"
    assert data.get("note") == payload["note"]
    # Optionally, verify that the start and end dates match
    assert data.get("start_date").startswith(start_date[:10])
    assert data.get("end_date").startswith((now + timedelta(days=30)).isoformat()[:10])

def test_update_recurring_transaction(server, auth_headers):
    """Test updating an existing recurring transaction"""
    # First, create a recurring transaction as a baseline
    category = create_custom_category(auth_headers, name="Update Recurring", color="#d4e5f6")
    category_id = category.get("id")

    post_url = f"{BASE_URL}/transactions/recurring/"
    now = datetime.utcnow().replace(microsecond=0)
    start_date = now.isoformat()
    end_date = (now + timedelta(days=30)).isoformat()
    payload = {
        "start_date": start_date,
        "end_date": end_date,
        "note": "Initial recurring transaction",
        "period": 7,
        "amount": 75.0,
        "category_id": category_id,
        "transaction_type": "expense",
        "vendor": "Initial Vendor"
    }
    post_resp = requests.post(post_url, json=payload, headers=auth_headers)
    assert post_resp.status_code == 201, post_resp.text
    recurring = post_resp.json()
    recurring_id = recurring.get("id")
    
    # Now, update the recurring transaction. Note that the update endpoint expects the recurring_id as a query parameter.
    update_url = f"{BASE_URL}/transactions/recurring/"
    new_now = datetime.utcnow().replace(microsecond=0)
    new_start_date = new_now.isoformat()
    new_end_date = (new_now + timedelta(days=40)).isoformat()
    update_payload = {
        "start_date": new_start_date,
        "end_date": new_end_date,
        "note": "Updated recurring transaction",
        "period": 5,
        "amount": 80.0,
        "category_id": category_id,
        "transaction_type": "expense",
        "vendor": "Updated Vendor"
    }
    # Pass the recurring_id as a query parameter
    update_resp = requests.put(update_url, params={"recurring_id": recurring_id}, json=update_payload, headers=auth_headers)
    assert update_resp.status_code == 200, update_resp.text
    updated_recurring = update_resp.json()
    assert updated_recurring.get("note") == "Updated recurring transaction"
    assert updated_recurring.get("period") == update_payload["period"]

def test_update_nonexistent_recurring_transaction(server, auth_headers):
    """Test updating a non-existent recurring transaction"""
    update_url = f"{BASE_URL}/transactions/recurring/"
    new_now = datetime.utcnow().replace(microsecond=0)
    update_payload = {
        "start_date": new_now.isoformat(),
        "end_date": (new_now + timedelta(days=20)).isoformat(),
        "note": "Non-existent update",
        "period": 5,
        "amount": 60.0,
        "category_id": 1,  # Assuming this category_id exists or will lead to a standard 404
        "transaction_type": "expense",
        "vendor": "No Vendor"
    }
    # Use an unlikely recurring_id to exist
    update_resp = requests.put(update_url, params={"recurring_id": 999999}, json=update_payload, headers=auth_headers)
    assert update_resp.status_code == 404

def test_delete_recurring_transaction(server, auth_headers):
    """Test deleting an existing recurring transaction and handling deletion of a non-existent record"""
    # Create a recurring transaction to delete
    category = create_custom_category(auth_headers, name="Delete Recurring", color="#ffcc00")
    category_id = category.get("id")
    
    post_url = f"{BASE_URL}/transactions/recurring/"
    now = datetime.utcnow().replace(microsecond=0)
    start_date = now.isoformat()
    end_date = (now + timedelta(days=25)).isoformat()
    payload = {
        "start_date": start_date,
        "end_date": end_date,
        "note": "Recurring to be deleted",
        "period": 7,
        "amount": 100.0,
        "category_id": category_id,
        "transaction_type": "expense",
        "vendor": "Vendor To Delete"
    }
    post_resp = requests.post(post_url, json=payload, headers=auth_headers)
    assert post_resp.status_code == 201, post_resp.text
    recurring = post_resp.json()
    recurring_id = recurring.get("id")
    
    delete_url = f"{BASE_URL}/transactions/recurring/"
    # Delete the recurring transaction with the given recurring_id
    delete_resp = requests.delete(delete_url, params={"recurring_id": recurring_id}, headers=auth_headers)
    assert delete_resp.status_code == 204, delete_resp.text
    
    # Try deleting the same recurring transaction again; should result in a 404
    delete_resp2 = requests.delete(delete_url, params={"recurring_id": recurring_id}, headers=auth_headers)
    assert delete_resp2.status_code == 404
