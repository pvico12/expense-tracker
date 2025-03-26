import pytest
import uvicorn
import requests
from multiprocessing import Process
import time
import datetime
import os
from test_setup import *

@pytest.fixture
def auth_headers(server):
    """
    Fixture to obtain authentication headers by logging in.
    If login fails, it attempts to register the user.
    """
    login_url = f"{BASE_URL}/auth/login"
    login_data = {"username": "newuser", "password": "newpassword"}
    try:
        response = requests.post(login_url, json=login_data)
        if response.status_code != 200:
            # Attempt to register if login fails
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
    This is used to ensure a valid category_id when creating percentage goals.
    """
    url = f"{BASE_URL}/categories/custom"
    payload = {"name": name, "color": color}
    response = requests.post(url, json=payload, headers=auth_headers)
    response.raise_for_status()
    return response.json()

def test_create_goal_amount(server, auth_headers):
    """Test creating a goal of type 'amount' without a category_id."""
    url = f"{BASE_URL}/goals/"
    start_date = datetime.datetime.utcnow().replace(microsecond=0)
    payload = {
        "goal_type": "amount",
        "limit": 100.0,
        "start_date": start_date.isoformat(),
        "period": 10  # Input period; response period should be (end_date - start_date).days + 1.
    }
    response = requests.post(url, json=payload, headers=auth_headers)
    assert response.status_code == 201, response.text
    data = response.json()
    assert data.get("goal_type") == "amount"
    assert data.get("limit") == 100.0
    # Verify the computed period.
    start = datetime.datetime.fromisoformat(data.get("start_date"))
    end = datetime.datetime.fromisoformat(data.get("end_date"))
    computed_period = (end - start).days + 1
    assert data.get("period") == computed_period
    assert data.get("id") is not None

def test_create_goal_percentage(server, auth_headers):
    """
    Test creating a goal of type 'percentage',
    which requires a valid category_id.
    """
    # Create a custom category first.
    category = create_custom_category(auth_headers, name="Percentage Category", color="#abcdef")
    category_id = category.get("id")
    url = f"{BASE_URL}/goals/"
    start_date = datetime.datetime.utcnow().replace(microsecond=0)
    payload = {
        "goal_type": "percentage",
        "limit": 50.0,
        "start_date": start_date.isoformat(),
        "period": 7,  # Must be >6 as per the validator.
        "category_id": category_id
    }
    response = requests.post(url, json=payload, headers=auth_headers)
    assert response.status_code == 201, response.text
    data = response.json()
    assert data.get("goal_type") == "percentage"
    assert data.get("category_id") == category_id
    assert data.get("limit") == 50.0

def test_get_goal_by_id(server, auth_headers):
    """Test retrieving a specific goal by its ID."""
    # Create a goal.
    url = f"{BASE_URL}/goals/"
    start_date = datetime.datetime.utcnow().replace(microsecond=0)
    payload = {
        "goal_type": "amount",
        "limit": 200.0,
        "start_date": start_date.isoformat(),
        "period": 8
    }
    create_resp = requests.post(url, json=payload, headers=auth_headers)
    assert create_resp.status_code == 201, create_resp.text
    goal = create_resp.json()
    goal_id = goal.get("id")
    
    # Retrieve the goal by its ID.
    get_url = f"{BASE_URL}/goals/{goal_id}"
    get_resp = requests.get(get_url, headers=auth_headers)
    assert get_resp.status_code == 200, get_resp.text
    retrieved_goal = get_resp.json()
    assert retrieved_goal.get("id") == goal_id
    assert retrieved_goal.get("limit") == 200.0

def test_get_goals_list(server, auth_headers):
    """Test retrieving a list of goals with additional statistics."""
    url = f"{BASE_URL}/goals/"
    # Create two goals.
    start_date1 = datetime.datetime.utcnow().replace(microsecond=0)
    payload1 = {
        "goal_type": "amount",
        "limit": 150.0,
        "start_date": start_date1.isoformat(),
        "period": 9
    }
    resp1 = requests.post(url, json=payload1, headers=auth_headers)
    assert resp1.status_code == 201, resp1.text

    start_date2 = datetime.datetime.utcnow().replace(microsecond=0)
    payload2 = {
        "goal_type": "amount",
        "limit": 250.0,
        "start_date": start_date2.isoformat(),
        "period": 12
    }
    resp2 = requests.post(url, json=payload2, headers=auth_headers)
    assert resp2.status_code == 201, resp2.text

    # Retrieve the list of goals.
    list_resp = requests.get(url, headers=auth_headers)
    assert list_resp.status_code == 200, list_resp.text
    data = list_resp.json()
    assert "goals" in data
    assert "stats" in data
    assert isinstance(data["goals"], list)
    assert isinstance(data["stats"], dict)

def test_update_goal(server, auth_headers):
    """Test updating an existing goal."""
    # Create a goal.
    url = f"{BASE_URL}/goals/"
    start_date = datetime.datetime.utcnow().replace(microsecond=0)
    payload = {
        "goal_type": "amount",
        "limit": 300.0,
        "start_date": start_date.isoformat(),
        "period": 10
    }
    create_resp = requests.post(url, json=payload, headers=auth_headers)
    assert create_resp.status_code == 201, create_resp.text
    goal = create_resp.json()
    goal_id = goal.get("id")

    # Update the goal's limit.
    update_url = f"{BASE_URL}/goals/{goal_id}"
    update_payload = {
        "limit": 350.0
    }
    update_resp = requests.put(update_url, json=update_payload, headers=auth_headers)
    assert update_resp.status_code == 200, update_resp.text
    updated_goal = update_resp.json()
    assert updated_goal.get("limit") == 350.0

def test_update_nonexistent_goal(server, auth_headers):
    """Test updating a goal that does not exist."""
    update_url = f"{BASE_URL}/goals/999999"
    update_payload = {"limit": 400.0}
    resp = requests.put(update_url, json=update_payload, headers=auth_headers)
    assert resp.status_code == 404

def test_delete_goal(server, auth_headers):
    """Test deleting an existing goal and verifying it is deleted."""
    # Create a goal.
    url = f"{BASE_URL}/goals/"
    start_date = datetime.datetime.utcnow().replace(microsecond=0)
    payload = {
        "goal_type": "amount",
        "limit": 500.0,
        "start_date": start_date.isoformat(),
        "period": 10
    }
    create_resp = requests.post(url, json=payload, headers=auth_headers)
    assert create_resp.status_code == 201, create_resp.text
    goal = create_resp.json()
    goal_id = goal.get("id")

    # Delete the goal.
    delete_url = f"{BASE_URL}/goals/{goal_id}"
    delete_resp = requests.delete(delete_url, headers=auth_headers)
    assert delete_resp.status_code == 204

    # Verify deletion by trying to retrieve the goal.
    get_resp = requests.get(delete_url, headers=auth_headers)
    assert get_resp.status_code == 404

def test_delete_nonexistent_goal(server, auth_headers):
    """Test deleting a goal that does not exist."""
    delete_url = f"{BASE_URL}/goals/999999"
    resp = requests.delete(delete_url, headers=auth_headers)
    assert resp.status_code == 404
