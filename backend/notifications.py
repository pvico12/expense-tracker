import asyncio
import logging
import time
from utils import get_coordinate_distance
from middlewares.goal_utils import get_mid_period_notifications, get_post_period_notifications, recalc_goal_progress
from models import Deal, DealLocationSubscription, FcmToken, Goal, User, RecurringTransaction
import json
import requests
from google.oauth2 import service_account
from google.auth.transport.requests import Request
from db import db_session
from datetime import datetime, timedelta

logger = logging.getLogger(__name__)

FCM_ENDPOINT = "https://fcm.googleapis.com/v1/projects/expense-tracker-app-448920/messages:send"

class FirebaseHTTPV1:
    def __init__(self, service_account_file):
        self.credentials = service_account.Credentials.from_service_account_file(
            service_account_file,
            scopes=['https://www.googleapis.com/auth/firebase.messaging']
        )
        self.project_id = self.credentials.project_id

    def get_access_token(self):
        if self.credentials.expired or not self.credentials.valid:
            self.credentials.refresh(Request())
        return self.credentials.token

    def send_single_notification(self, device_token, title, body):
        try:
            access_token = self.get_access_token()
            
            logger.info(f"Sending notification to {device_token}")
            
            # Construct the message payload
            message = {
                "message": {
                    "token": device_token,
                    "notification": {
                        "title": title,
                        "body": body
                    }
                }
            }
            
            # Set headers
            headers = {
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/json; UTF-8",
            }
            
            # Send the request
            url = FCM_ENDPOINT.format(project_id=self.project_id)
            response = requests.post(url, headers=headers, json=message)
            response.raise_for_status()
            
            return response.json()
            
        except requests.exceptions.RequestException as e:
            print(f"HTTP error: {str(e)}")
            if e.response is not None:
                print(f"Response: {e.response.text}")
            return None
        except Exception as e:
            print(f"Error: {str(e)}")
            return None

    def send_multiple_notifications(self, device_tokens, title, body):
        """Send notification to multiple devices"""
        results = []
        for token in device_tokens:
            result = self.send_single_notification(token, title, body)
            results.append(result)
        return results


def send_push_notification_healthcheck():
    fcm = FirebaseHTTPV1("expense-tracker-firebase.json")
    # get all FCM tokens from fcm_tokens table
    fcm_tokens = db_session.query(FcmToken).all()
    logger.info(f"Sending healthcheck notification to {len(fcm_tokens)} devices")
    
    # send a test notification to each device
    fcm.send_multiple_notifications([token.token for token in fcm_tokens], "Healthcheck", "This is a test notification")
    
    # FOR TESTING
    # fcm_tokens = ["dnAFw0TDTvCPhzV2YhJeYl:APA91bGiM-YlnIDDEelSp5bZ8O3QxxRgjMghEQwcZHrKDUvnGrRwP8M--pM1AwqJfOCjxQAR3AxCl6kqdeqNh7Nh8P5mXoipnvyopJNq3SqyLOOVgjtlmqo"]
    # fcm.send_multiple_notifications(fcm_tokens, "Healthcheck", "This is a test notification")
        

async def push_notification_healthcheck():
    fcm = FirebaseHTTPV1("expense-tracker-firebase.json")
    
    while True:
        send_push_notification_healthcheck()
        await asyncio.sleep(3600)  # sleep for 1 hour
        
async def send_goal_notifications():
    fcm = FirebaseHTTPV1("expense-tracker-firebase.json")
    
    while True:
        fcm_tokens = []
        
        # recaculate goal progress
        users = db_session.query(Goal).distinct(Goal.user_id).all()
        user_ids = [user.user_id for user in users]
        for user_id in user_ids:
            recalc_goal_progress(db_session, user_id)
        
        # get goal notifications
        mid_period_notifications = get_mid_period_notifications(db_session)
        post_period_notifications = get_post_period_notifications(db_session)
        all_notifications = mid_period_notifications + post_period_notifications
        for goal_notifaction in all_notifications:
            logger.info(f"Sending goal notification for goal {goal_notifaction['goal_id']}")
            
            # get goal data by id
            goal_id = goal_notifaction["goal_id"]
            user_id = db_session.query(Goal).filter(Goal.id == goal_id).first().user_id
            
            # get FCM tokens with this user_id
            tokens = db_session.query(FcmToken).filter(FcmToken.user_id == user_id).all()
            fcm_tokens.extend([token.token for token in tokens])
            
            # send notifications
            fcm.send_multiple_notifications(fcm_tokens, "Expense Tracker Goal!", goal_notifaction["message"])
            
            if goal_notifaction.get("completed", False):
                # get period of goal
                goal = db_session.query(Goal).filter(Goal.id == goal_id).first()
                period = goal.end_date - goal.start_date
                period = period.days
                
                # add xp to user
                if (period < 8):
                    add_xp_to_user(user_id, 5)
                else:
                    add_xp_to_user(user_id, 20)
                
        
        await asyncio.sleep(120)  # sleep for 2 minutes

def send_new_deal_notification(new_deal: Deal):
    fcm = FirebaseHTTPV1("expense-tracker-firebase.json")
    
    # get all deal subscriptions
    deal_subscriptions = db_session.query(DealLocationSubscription).all()
    user_ids_notify = set()
    
    # for every subscription, check if the deal is within 100km of the subscription location
    for deal_subscription in deal_subscriptions:
        if deal_subscription.user_id == new_deal.user_id:
            continue
        distance = get_coordinate_distance(
            new_deal.latitude, new_deal.longitude,
            deal_subscription.latitude, deal_subscription.longitude
        )
        
        if distance <= 100:
            user_ids_notify.add(deal_subscription.user_id)
    
    # get FCM tokens with these user_ids
    fcm_tokens = []
    for user_id in user_ids_notify:
        tokens = db_session.query(FcmToken).filter(FcmToken.user_id == user_id).all()
        fcm_tokens.extend([token.token for token in tokens])
    
    print(fcm_tokens)
    
    # send notifications
    fcm.send_multiple_notifications(fcm_tokens, "New Deal Alert!", f"A new deal at {new_deal.vendor} has been posted near you!")
    
def send_level_up_notification(user_id: int, level: int):
    fcm = FirebaseHTTPV1("expense-tracker-firebase.json")
    
    # get FCM tokens with this user_id
    tokens = db_session.query(FcmToken).filter(FcmToken.user_id == user_id).all()
    fcm_tokens = [token.token for token in tokens]
    
    logger.info(f"Sending level up notification to user {user_id}")
    
    # send notifications
    fcm.send_multiple_notifications(fcm_tokens, "Level Up!", f"Congratulations! You have reached level {level}!")

def add_xp_to_user(user_id: int, xp: int) -> User:
    """Add XP to the user's profile."""
    user = db_session.query(User).filter_by(id=user_id).first()
    if not user:
        return
    
    user.xp += xp
    old_level = user.level
    
    # calcuate new level
    new_level = 1
    score = xp
    xp_for_next_level = 5
    
    while score >= xp_for_next_level:
        score -= xp_for_next_level
        new_level += 1
        xp_for_next_level *= 2
    
    user.level = new_level
    
    db_session.commit()
    db_session.refresh(user)
    
    if new_level > old_level:
        send_level_up_notification(user_id, new_level)
    
    return user

async def send_upcoming_recurring_payment_notifications():
    fcm = FirebaseHTTPV1("expense-tracker-firebase.json")
        
    while True:
        recurring_transactions = db_session.query(RecurringTransaction).all()
        now = datetime.utcnow()
        # Define the upcoming threshold (next 24 hours)
        upcoming_threshold = now + timedelta(hours=24)
        
        for recurring in recurring_transactions:
            period_delta = timedelta(days=recurring.period)
            if now < recurring.start_date:
                next_payment_date = recurring.start_date
            else:
                periods_passed = ((now - recurring.start_date) // period_delta) + 1
                next_payment_date = recurring.start_date + periods_passed * period_delta
            if next_payment_date > recurring.end_date:
                continue

            # Check if the upcoming payment falls within the next 24 hours
            if now <= next_payment_date <= upcoming_threshold:
                # If a notification was already sent for this upcoming payment, skip it.
                if recurring.last_notified_payment_date is not None and recurring.last_notified_payment_date == next_payment_date:
                    continue
                
                tokens = db_session.query(FcmToken).filter(FcmToken.user_id == recurring.user_id).all()
                fcm_tokens = [token.token for token in tokens]
                message = (
                    f"Reminder: Your recurring payment is scheduled for "
                    f"{next_payment_date.strftime('%Y-%m-%d %H:%M:%S')}."
                )
                fcm.send_multiple_notifications(fcm_tokens, "Upcoming Recurring Payment", message)
                
                # Mark that a notification for this upcoming payment date has been sent.
                recurring.last_notified_payment_date = next_payment_date
                db_session.commit()
        await asyncio.sleep(120)