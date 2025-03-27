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
        TARGET_DEAL_SUBSCRIPTION_ID = subscription["id"]
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

def test_create_deal_missing_required_fields(server):
    """Test creating a deal with missing required fields"""
    url = f"{BASE_URL}/deals/"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "name": "Test Deal",
    }
    try:
        response = requests.post(url, json=data, headers=headers)
        assert response.status_code == 422
        response_data = response.json()
        assert "detail" in response_data
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_update_deal_invalid_id(server):
    """Test updating a deal with an invalid deal ID"""
    invalid_deal_id = -1
    url = f"{BASE_URL}/deals/{invalid_deal_id}"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "name": "Invalid Deal Update"
    }
    try:
        response = requests.put(url, json=data, headers=headers)
        assert response.status_code == 400
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_delete_deal_invalid_id(server):
    """Test deleting a deal with an invalid deal ID"""
    invalid_deal_id = -1
    url = f"{BASE_URL}/deals/{invalid_deal_id}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.delete(url, headers=headers)
        assert response.status_code == 400
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_upvote_deal_invalid_id(server):
    """Test upvoting a deal with an invalid deal ID"""
    invalid_deal_id = -1
    url = f"{BASE_URL}/deals/upvote/{invalid_deal_id}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.post(url, headers=headers)
        assert response.status_code == 400
        response_data = response.json()
        assert response_data["detail"] == "Deal not found."
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_create_deal_subscription_missing_fields(server):
    """Test creating a deal subscription with missing fields"""
    url = f"{BASE_URL}/deals/subscription"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "address": "123 King St"
    }
    try:
        response = requests.post(url, json=data, headers=headers)
        assert response.status_code == 422
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_create_deal_invalid_price(server):
    """Test creating a deal with an invalid price"""
    url = f"{BASE_URL}/deals/"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "name": "Invalid Price Deal",
        "description": "This deal has an invalid price.",
        "price": -10.99,  # Invalid price
        "address": "123 Invalid St",
        "longitude": -122.4194,
        "latitude": 37.7749,
        "date": datetime(2025, 1, 1).isoformat(),
        "vendor": "Invalid Vendor"
    }
    try:
        response = requests.post(url, json=data, headers=headers)
        assert response.status_code == 422
        response_data = response.json()
        assert "detail" in response_data
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_update_deal_partial_update(server):
    """Test partially updating a deal"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "price": 15.99
    }
    try:
        response = requests.put(url, json=data, headers=headers)
        assert response.status_code == 200
        
        # Verify the update
        response = requests.get(url, headers=headers)
        assert response.status_code == 200
        updated_deal = response.json()
        assert updated_deal["price"] == 15.99
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_create_deal_invalid_date(server):
    """Test creating a deal with an invalid date"""
    url = f"{BASE_URL}/deals/"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "name": "Invalid Date Deal",
        "description": "This deal has an invalid date.",
        "price": 10.99,
        "address": "123 Invalid Date St",
        "longitude": -122.4194,
        "latitude": 37.7749,
        "date": "invalid-date",  # Invalid date format
        "vendor": "Invalid Vendor"
    }
    try:
        response = requests.post(url, json=data, headers=headers)
        assert response.status_code == 422
        response_data = response.json()
        assert "detail" in response_data
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_deal_invalid_id(server):
    """Test retrieving a deal with an invalid deal ID"""
    invalid_deal_id = -1
    url = f"{BASE_URL}/deals/{invalid_deal_id}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.get(url, headers=headers)
        assert response.status_code == 400
        response_data = response.json()
        assert response_data["detail"] == "Deal not found."
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_update_deal_no_changes(server):
    """Test updating a deal without providing any changes"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    data = {}  # No changes provided
    try:
        response = requests.put(url, json=data, headers=headers)
        assert response.status_code == 200
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_delete_deal_no_auth(server):
    """Test deleting a deal without authentication"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/{TARGET_DEAL_ID}"
    try:
        response = requests.delete(url)
        assert response.status_code == 403
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_upvote_deal_no_auth(server):
    """Test upvoting a deal without authentication"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/upvote/{TARGET_DEAL_ID}"
    try:
        response = requests.post(url)
        assert response.status_code == 403
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_downvote_deal_no_auth(server):
    """Test downvoting a deal without authentication"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/downvote/{TARGET_DEAL_ID}"
    try:
        response = requests.post(url)
        assert response.status_code == 403
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_cancel_vote_no_auth(server):
    """Test canceling a vote without authentication"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/cancel_vote/{TARGET_DEAL_ID}"
    try:
        response = requests.post(url)
        assert response.status_code == 403
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_upvote_then_downvote(server):
    """Test upvoting a deal and then downvoting it"""
    global TARGET_DEAL_ID
    url_upvote = f"{BASE_URL}/deals/upvote/{TARGET_DEAL_ID}"
    url_downvote = f"{BASE_URL}/deals/downvote/{TARGET_DEAL_ID}"
    vote_url = f"{BASE_URL}/deals/votes/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        # Upvote the deal
        response = requests.post(url_upvote, headers=headers)
        assert response.status_code == 200

        # Verify the upvote count
        vote_response = requests.get(vote_url, headers=headers)
        assert vote_response.status_code == 200
        votes = vote_response.json()
        assert votes["upvotes"] == 1
        assert votes["downvotes"] == 0

        # Downvote the deal
        response = requests.post(url_downvote, headers=headers)
        assert response.status_code == 200

        # Verify the downvote count
        vote_response = requests.get(vote_url, headers=headers)
        assert vote_response.status_code == 200
        votes = vote_response.json()
        assert votes["upvotes"] == 0
        assert votes["downvotes"] == 1
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_multiple_upvotes(server):
    """Test multiple upvotes by the same user"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/upvote/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        # First upvote
        response = requests.post(url, headers=headers)
        assert response.status_code == 200

        # Attempt to upvote again
        response = requests.post(url, headers=headers)
        assert response.status_code == 400
        response_data = response.json()
        assert response_data["detail"] == "You have already upvoted this deal."
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_multiple_downvotes(server):
    """Test multiple downvotes by the same user"""
    global TARGET_DEAL_ID
    url = f"{BASE_URL}/deals/downvote/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        # First downvote
        response = requests.post(url, headers=headers)
        assert response.status_code == 200

        # Attempt to downvote again
        response = requests.post(url, headers=headers)
        assert response.status_code == 400
        response_data = response.json()
        assert response_data["detail"] == "You have already downvoted this deal."
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_cancel_vote_after_upvote(server):
    """Test canceling a vote after upvoting"""
    global TARGET_DEAL_ID
    url_upvote = f"{BASE_URL}/deals/upvote/{TARGET_DEAL_ID}"
    url_cancel = f"{BASE_URL}/deals/cancel_vote/{TARGET_DEAL_ID}"
    vote_url = f"{BASE_URL}/deals/votes/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        # Upvote the deal
        response = requests.post(url_upvote, headers=headers)
        assert response.status_code == 200

        # Cancel the vote
        response = requests.post(url_cancel, headers=headers)
        assert response.status_code == 200

        # Verify the vote counts are reset
        vote_response = requests.get(vote_url, headers=headers)
        assert vote_response.status_code == 200
        votes = vote_response.json()
        assert votes["upvotes"] == 0
        assert votes["downvotes"] == 0
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_cancel_vote_after_downvote(server):
    """Test canceling a vote after downvoting"""
    global TARGET_DEAL_ID
    url_downvote = f"{BASE_URL}/deals/downvote/{TARGET_DEAL_ID}"
    url_cancel = f"{BASE_URL}/deals/cancel_vote/{TARGET_DEAL_ID}"
    vote_url = f"{BASE_URL}/deals/votes/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        # Downvote the deal
        response = requests.post(url_downvote, headers=headers)
        assert response.status_code == 200

        # Cancel the vote
        response = requests.post(url_cancel, headers=headers)
        assert response.status_code == 200

        # Verify the vote counts are reset
        vote_response = requests.get(vote_url, headers=headers)
        assert vote_response.status_code == 200
        votes = vote_response.json()
        assert votes["upvotes"] == 0
        assert votes["downvotes"] == 0
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_upvote_downvote_cancel_sequence(server):
    """Test a sequence of upvote, downvote, and cancel actions"""
    global TARGET_DEAL_ID
    url_upvote = f"{BASE_URL}/deals/upvote/{TARGET_DEAL_ID}"
    url_downvote = f"{BASE_URL}/deals/downvote/{TARGET_DEAL_ID}"
    url_cancel = f"{BASE_URL}/deals/cancel_vote/{TARGET_DEAL_ID}"
    vote_url = f"{BASE_URL}/deals/votes/{TARGET_DEAL_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        # Upvote the deal
        response = requests.post(url_upvote, headers=headers)
        assert response.status_code == 200

        # Downvote the deal
        response = requests.post(url_downvote, headers=headers)
        assert response.status_code == 200

        # Cancel the vote
        response = requests.post(url_cancel, headers=headers)
        assert response.status_code == 200

        # Verify the vote counts are reset
        vote_response = requests.get(vote_url, headers=headers)
        assert vote_response.status_code == 200
        votes = vote_response.json()
        assert votes["upvotes"] == 0
        assert votes["downvotes"] == 0
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")


def test_update_subscription_invalid_id(server):
    """Test updating a subscription with an invalid ID"""
    invalid_subscription_id = -1
    url = f"{BASE_URL}/deals/subscription/{invalid_subscription_id}"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "address": "Updated Address St",
        "longitude": -122.4195,
        "latitude": 37.7750
    }
    try:
        response = requests.put(url, json=data, headers=headers)
        assert response.status_code == 400
        response_data = response.json()
        assert response_data["detail"] == "Subscription not found."
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_delete_subscription_invalid_id(server):
    """Test deleting a subscription with an invalid ID"""
    invalid_subscription_id = -1
    url = f"{BASE_URL}/deals/subscription/{invalid_subscription_id}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.delete(url, headers=headers)
        assert response.status_code == 400
        response_data = response.json()
        assert response_data["detail"] == "Subscription not found."
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_subscription_invalid_id(server):
    """Test retrieving a subscription with an invalid ID"""
    invalid_subscription_id = -1
    url = f"{BASE_URL}/deals/subscription/{invalid_subscription_id}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.get(url, headers=headers)
        assert response.status_code == 400
        response_data = response.json()
        assert response_data["detail"] == "Subscription not found."
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")


def test_update_subscription_partial_update(server):
    """Test partially updating a subscription"""
    global TARGET_DEAL_SUBSCRIPTION_ID
    url = f"{BASE_URL}/deals/subscription/{TARGET_DEAL_SUBSCRIPTION_ID}"
    headers = get_headers("dealtester", "testpassword")
    data = {
        "address": "Partially Updated Address"
    }
    try:
        response = requests.put(url, json=data, headers=headers)
        assert response.status_code == 200

        # Verify the update
        response = requests.get(url, headers=headers)
        assert response.status_code == 200
        updated_subscription = response.json()
        assert updated_subscription["address"] == data["address"]
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_delete_subscription_no_auth(server):
    """Test deleting a subscription without authentication"""
    global TARGET_DEAL_SUBSCRIPTION_ID
    url = f"{BASE_URL}/deals/subscription/{TARGET_DEAL_SUBSCRIPTION_ID}"
    try:
        response = requests.delete(url)
        assert response.status_code == 403
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")

def test_get_all_subscriptions_no_auth(server):
    """Test retrieving all subscriptions without authentication"""
    url = f"{BASE_URL}/deals/subscriptions/all"
    try:
        response = requests.get(url)
        assert response.status_code == 403
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
        

def test_delete_subscription(server):
    """Test deleting a subscription"""
    global TARGET_DEAL_SUBSCRIPTION_ID
    url = f"{BASE_URL}/deals/subscription/{TARGET_DEAL_SUBSCRIPTION_ID}"
    headers = get_headers("dealtester", "testpassword")
    try:
        response = requests.delete(url, headers=headers)
        assert response.status_code == 200
        TARGET_DEAL_SUBSCRIPTION_ID = None
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
        TARGET_DEAL_ID = None
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server")
