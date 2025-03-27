import pytest
import uvicorn
import requests
from multiprocessing import Process
import time
from test_setup import *
from datetime import datetime

TARGET_DEAL_ID = None  # Global variable to track the target deal ID
TARGET_DEAL_SUBSCRIPTION_ID = None  # Global variable to track the target deal subscription ID

@pytest.fixture(scope="module", autouse=True)
def create_test_user(server):
    """Create a user for testing deals"""
    url = f"{BASE_URL}/auth/register"
    data = {
        "username": "dealtester",
        "password": "testpassword",
        "firstname": "Deal",
        "lastname": "Tester"
    }
    try:
        response = requests.post(url, json=data)
        assert response.status_code == 201
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_create_deal(server):
    """Test creating a new deal"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "name": "Test Deal",
        "description": "This is a test deal.",
        "price": 10.99,
        "address": "123 Test St",
        "longitude": -122.4194,
        "latitude": 37.7749,
        "date": datetime(2025, 1, 1).isoformat(),
        "vendor": "Test Vendor"
    }
    try:
        response = requests.post(url, json=data, headers=headers)
        assert response.status_code == 201
        created_deal = response.json()
        TARGET_DEAL_ID = created_deal["id"]  # Store the created deal ID
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_deals(server):
    """Test retrieving all deals"""
    url = f"{BASE_URL}/deals/list"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.post(url, headers=headers)
        assert response.status_code == 200
        deals = response.json()
        assert isinstance(deals, list)
        assert len(deals) > 0
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_update_deal(server):
    """Test updating a deal"""
    global TARGET_DEAL_ID
    
    url = f"{BASE_URL}/deals/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "name": "Updated Deal Name",
        "description": "Updated description."
    }
    try:
        response = requests.put(url, json=data, headers=headers)
        assert response.status_code == 200
        
        # Verify the updated deal
        response = requests.get(url, headers=headers)
        assert response.status_code == 200
        updated_deal = response.json()
        assert updated_deal["name"] == data["name"]
        assert updated_deal["description"] == data["description"]
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_upvote_deal(server):
    """Test upvoting a deal"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/upvote/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.post(url, headers=headers)
        assert response.status_code == 200

        # Verify the upvote count
        vote_url = f"{BASE_URL}/deals/votes/{TARGET_DEAL_ID}"
        vote_response = requests.get(vote_url, headers=headers)
        assert vote_response.status_code == 200
        votes = vote_response.json()
        assert votes["upvotes"] > 0
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_downvote_deal(server):
    """Test downvoting a deal"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/downvote/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.post(url, headers=headers)
        assert response.status_code == 200

        # Verify the downvote count
        vote_url = f"{BASE_URL}/deals/votes/{TARGET_DEAL_ID}"
        vote_response = requests.get(vote_url, headers=headers)
        assert vote_response.status_code == 200
        votes = vote_response.json()
        assert votes["downvotes"] > 0
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_cancel_vote(server):
    """Test canceling a vote"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/cancel_vote/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.post(url, headers=headers)
        assert response.status_code == 200

        # Verify the vote counts are reset
        vote_url = f"{BASE_URL}/deals/votes/{TARGET_DEAL_ID}"
        vote_response = requests.get(vote_url, headers=headers)
        assert vote_response.status_code == 200
        votes = vote_response.json()
        assert votes["upvotes"] == 0
        assert votes["downvotes"] == 0
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_delete_deal(server):
    """Test deleting a deal"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.delete(url, headers=headers)
        assert response.status_code == 200
        TARGET_DEAL_ID = None  # Reset the global variable after deletion
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")


def test_create_deal_subscription(server):
    """Test creating a deal subscription"""
    global TARGET_DEAL_SUBSCRIPTION_ID
    url = f"{BASE_URL}/deals/subscription"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "address": "456 Subscription St",
        "longitude": -122.4194,
        "latitude": 37.7749
    }
    try:
        response = requests.post(url, json=data, headers=headers)
        assert response.status_code == 201
        subscription = response.json()
        TARGET_DEAL_SUBSCRIPTION_ID = subscription["id"]  # Store the created subscription ID
        assert subscription["address"] == data["address"]
        assert subscription["longitude"] == data["longitude"]
        assert subscription["latitude"] == data["latitude"]
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_all_deal_subscriptions(server):
    """Test retrieving all deal subscriptions for a user"""
    url = f"{BASE_URL}/deals/subscriptions/all"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.get(url, headers=headers)
        assert response.status_code == 200
        subscriptions = response.json()
        assert isinstance(subscriptions, list)
        assert len(subscriptions) > 0
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_update_deal_subscription(server):
    """Test updating a deal subscription"""
    global TARGET_DEAL_SUBSCRIPTION_ID
    url = f"{BASE_URL}/deals/subscription/{TARGET_DEAL_SUBSCRIPTION_ID}"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "address": "789 Updated St",
        "longitude": -122.4195,
        "latitude": 37.7750
    }
    try:
        response = requests.put(url, json=data, headers=headers)
        assert response.status_code == 200
        updated_subscription = response.json()
        assert updated_subscription["address"] == data["address"]
        assert updated_subscription["longitude"] == data["longitude"]
        assert updated_subscription["latitude"] == data["latitude"]
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_delete_deal_subscription(server):
    """Test deleting a deal subscription"""
    global TARGET_DEAL_SUBSCRIPTION_ID
    url = f"{BASE_URL}/deals/subscription/{TARGET_DEAL_SUBSCRIPTION_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.delete(url, headers=headers)
        assert response.status_code == 200
        TARGET_DEAL_SUBSCRIPTION_ID = None  # Reset the global variable after deletion
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
