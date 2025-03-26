import pytest
import uvicorn
import requests
from multiprocessing import Process
from datetime import datetime
from io import BytesIO, StringIO
import csv
import os
from test_setup import *

@pytest.fixture
def auth_headers(server):
    """
    Obtain authentication headers by logging in.
    If login fails, attempt to register the user.
    """
    login_url = f"{BASE_URL}/auth/login"
    login_data = {"username": "newuser", "password": "newpassword"}
    try:
        response = requests.post(login_url, json=login_data)
        if response.status_code != 200:
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
    This is used to ensure a valid category_id when creating transactions.
    """
    url = f"{BASE_URL}/categories/custom"
    payload = {"name": name, "color": color}
    response = requests.post(url, json=payload, headers=auth_headers)
    response.raise_for_status()
    return response.json()

def test_create_transaction(server, auth_headers):
    """Test creating a new transaction."""
    # Create a custom category to use in the transaction.
    category = create_custom_category(auth_headers, name="Transaction Category", color="#445566")
    category_id = category.get("id")
    
    url = f"{BASE_URL}/transactions/"
    payload = {
        "amount": 123.45,
        "category_id": category_id,
        "transaction_type": "expense",
        "note": "Test transaction",
        "date": datetime.utcnow().isoformat(),
        "vendor": "Test Vendor"
    }
    response = requests.post(url, json=payload, headers=auth_headers)
    assert response.status_code == 201, response.text
    data = response.json()
    assert data.get("amount") == payload["amount"]
    assert data.get("note") == payload["note"]
    assert data.get("vendor") == payload["vendor"]
    assert data.get("category_id") == category_id

def test_read_transactions(server, auth_headers):
    """Test reading a list of transactions."""
    # Create a transaction so that there is at least one.
    category = create_custom_category(auth_headers, name="Read Category", color="#778899")
    category_id = category.get("id")
    create_url = f"{BASE_URL}/transactions/"
    payload = {
        "amount": 50.0,
        "category_id": category_id,
        "transaction_type": "expense",
        "note": "List transaction",
        "date": datetime.utcnow().isoformat(),
        "vendor": "List Vendor"
    }
    create_resp = requests.post(create_url, json=payload, headers=auth_headers)
    assert create_resp.status_code == 201

    url = f"{BASE_URL}/transactions/"
    params = {"skip": 0, "limit": 10}
    resp = requests.get(url, headers=auth_headers, params=params)
    assert resp.status_code == 200, resp.text
    data = resp.json()
    assert isinstance(data, list)

def test_update_transaction(server, auth_headers):
    """Test updating an existing transaction."""
    # Create a transaction first.
    category = create_custom_category(auth_headers, name="Update Category", color="#aabbcc")
    category_id = category.get("id")
    create_url = f"{BASE_URL}/transactions/"
    payload = {
        "amount": 80.0,
        "category_id": category_id,
        "transaction_type": "expense",
        "note": "Original note",
        "date": datetime.utcnow().isoformat(),
        "vendor": "Original Vendor"
    }
    create_resp = requests.post(create_url, json=payload, headers=auth_headers)
    assert create_resp.status_code == 201
    transaction = create_resp.json()
    transaction_id = transaction.get("id")
    
    # Update transaction: change note and amount.
    update_url = f"{BASE_URL}/transactions/{transaction_id}"
    update_payload = {
        "amount": 95.0,
        "note": "Updated note",
        "vendor": "Updated Vendor"
    }
    update_resp = requests.put(update_url, json=update_payload, headers=auth_headers)
    assert update_resp.status_code == 200, update_resp.text
    updated_tx = update_resp.json()
    assert updated_tx.get("amount") == update_payload["amount"]
    assert updated_tx.get("note") == update_payload["note"]
    assert updated_tx.get("vendor") == update_payload["vendor"]

def test_delete_transaction(server, auth_headers):
    """Test deleting an existing transaction."""
    # Create a transaction to delete.
    category = create_custom_category(auth_headers, name="Delete Category", color="#ddeeff")
    category_id = category.get("id")
    create_url = f"{BASE_URL}/transactions/"
    payload = {
        "amount": 60.0,
        "category_id": category_id,
        "transaction_type": "expense",
        "note": "To be deleted",
        "date": datetime.utcnow().isoformat(),
        "vendor": "Delete Vendor"
    }
    create_resp = requests.post(create_url, json=payload, headers=auth_headers)
    assert create_resp.status_code == 201
    tx = create_resp.json()
    transaction_id = tx.get("id")
    
    delete_url = f"{BASE_URL}/transactions/{transaction_id}"
    delete_resp = requests.delete(delete_url, headers=auth_headers)
    assert delete_resp.status_code == 204, delete_resp.text
    
    # Attempting to delete again should return 404.
    delete_resp2 = requests.delete(delete_url, headers=auth_headers)
    assert delete_resp2.status_code == 404

def test_upload_csv(server, auth_headers):
    """Test the CSV upload endpoint (parsing only)."""
    csv_content = (
        "amount,category,date,note,vendor\n"
        "25.50,Transaction Category,2023-10-01T12:00:00,CSV Transaction,CSV Vendor\n"
    )
    files = {
        "file": ("test.csv", csv_content, "text/csv")
    }
    url = f"{BASE_URL}/transactions/csv"
    params = {"create_transactions": 0}
    resp = requests.post(url, files=files, headers=auth_headers, params=params)
    assert resp.status_code == 200, resp.text
    data = resp.json()
    # Expect a list of dictionaries (one transaction parsed from CSV)
    assert isinstance(data, list)
    assert "amount" in data[0]

def test_get_csv_template(server, auth_headers):
    """Test retrieving the CSV template."""
    url = f"{BASE_URL}/transactions/csv/template"
    resp = requests.get(url, headers=auth_headers)
    # Expect a FileResponse; check content type contains 'text/csv'
    assert resp.status_code == 200, resp.text
    assert "text/csv" in resp.headers.get("content-type", "")

def test_scan_receipt(server):
    """Test scanning a receipt."""
    url = f"{BASE_URL}/transactions/receipt/scan"
    # Create a dummy file-like object.
    # Ideally, parse_receipt() will return a valid structure based on the image content.
    dummy_content = b"dummy image bytes"
    files = {
        "file": ("dummy.jpg", BytesIO(dummy_content), "image/jpeg")
    }
    resp = requests.post(url, files=files)
    assert resp.status_code == 200, resp.text
    data = resp.json()
    # Check that required keys are present.
    for key in ["items", "approx_subtotal", "approx_fees", "total"]:
        assert key in data
