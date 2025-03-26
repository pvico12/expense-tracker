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
    """
    url = f"{BASE_URL}/categories/custom"
    payload = {"name": name, "color": color}
    response = requests.post(url, json=payload, headers=auth_headers)
    response.raise_for_status()
    return response.json()

def create_recurring_transaction(auth_headers, category_id, note, amount, trans_date, vendor, period=1):
    """
    Helper function to create a recurring transaction.
    Setting start_date and end_date to the same value will generate a single transaction.
    """
    url = f"{BASE_URL}/transactions/recurring/"
    payload = {
        "start_date": trans_date.isoformat(),
        "end_date": trans_date.isoformat(),
        "note": note,
        "period": period,  # Only one transaction will be generated.
        "amount": amount,
        "category_id": category_id,
        "transaction_type": "expense",  # Expense to contribute to total spend.
        "vendor": vendor
    }
    response = requests.post(url, json=payload, headers=auth_headers)
    response.raise_for_status()
    return response.json()

def test_summary_spend_endpoint(server, auth_headers):
    """
    Test the /statistics/summary_spend endpoint.
    Create a recurring transaction to generate an expense transaction,
    then call the summary endpoint and verify the response.
    """
    # Create a custom category.
    category = create_custom_category(auth_headers, name="Stats Category", color="#ff5733")
    category_id = category.get("id")
    
    # Set the transaction date to now.
    trans_date = datetime.utcnow().replace(microsecond=0)
    amount = 100.0
    note = "Test expense for statistics"
    vendor = "Stats Vendor"
    
    # Create a recurring transaction that generates a single expense transaction.
    create_recurring_transaction(auth_headers, category_id, note, amount, trans_date, vendor)
    
    # Define a time window that should include the generated transaction.
    start_window = (trans_date - timedelta(minutes=5)).isoformat()
    end_window = (trans_date + timedelta(minutes=5)).isoformat()
    
    # Call the summary_spend endpoint with query parameters.
    summary_url = f"{BASE_URL}/statistics/summary_spend"
    params = {"start_date": start_window, "end_date": end_window}
    resp = requests.get(summary_url, headers=auth_headers, params=params)
    assert resp.status_code == 200, resp.text
    data = resp.json()
    
    # Verify response structure and values.
    assert "total_spend" in data, "total_spend missing in response"
    assert "category_breakdown" in data, "category_breakdown missing in response"
    assert "transaction_history" in data, "transaction_history missing in response"
    # Depending on your model, type_totals may also be present.
    # Check that total_spend is at least the amount we added.
    assert data["total_spend"] >= amount, f"Expected total_spend >= {amount}, got {data['total_spend']}"
    
    # Check if our category appears in the breakdown.
    found = False
    for entry in data["category_breakdown"]:
        if entry.get("category_name") == "Stats Category":
            found = True
            # Check that total_amount is as expected.
            assert abs(entry.get("total_amount") - amount) < 0.01, "Mismatch in category total_amount"
            break
    assert found, "Created category not found in category_breakdown"
    
    # Ensure type_totals includes key for expense with non-zero value if provided.
    if "type_totals" in data:
        assert "expense" in data["type_totals"], "Missing 'expense' in type_totals"
        assert data["type_totals"]["expense"] >= amount, "Type totals for expense is less than expected"

def test_summary_category_endpoint(server, auth_headers):
    """
    Test the /statistics/summary_category endpoint.
    Create a recurring transaction to generate an expense transaction,
    then call the summary_category endpoint and verify the response.
    """
    # Create a custom category.
    category = create_custom_category(auth_headers, name="Category Stats", color="#33cc44")
    category_id = category.get("id")
    
    # Set the transaction date to now.
    trans_date = datetime.utcnow().replace(microsecond=0)
    amount = 75.0
    note = "Test category expense"
    vendor = "Category Vendor"
    
    # Create a recurring transaction.
    create_recurring_transaction(auth_headers, category_id, note, amount, trans_date, vendor)
    
    # Define a time window that should include the generated transaction.
    start_window = (trans_date - timedelta(minutes=5)).isoformat()
    end_window = (trans_date + timedelta(minutes=5)).isoformat()
    
    # Call the summary_category endpoint.
    summary_url = f"{BASE_URL}/statistics/summary_category"
    params = {"start_date": start_window, "end_date": end_window}
    resp = requests.get(summary_url, headers=auth_headers, params=params)
    assert resp.status_code == 200, resp.text
    data = resp.json()
    
    # Verify response structure.
    assert "type_totals" in data, "type_totals missing in summary_category response"
    assert "transaction_history" in data, "transaction_history missing in summary_category response"
    
    # Check that type_totals contains 'expense' with at least the expected amount.
    assert "expense" in data["type_totals"], "Expense key missing in type_totals"
    assert data["type_totals"]["expense"] >= amount, f"Expected expense total >= {amount}, got {data['type_totals']['expense']}"
    
    # transaction_history should be a list.
    assert isinstance(data["transaction_history"], list), "transaction_history is not a list"
