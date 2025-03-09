import time
from models import FcmToken
import json
import requests
from google.oauth2 import service_account
from google.auth.transport.requests import Request
from db import db_session

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


def push_notification_healthcheck():
    fcm = FirebaseHTTPV1("expense-tracker-app-448920-firebase-adminsdk-fbsvc-cb6e177e39.json")
    
    while True:
        # get all FCM tokens from fcm_tokens table
        fcm_tokens = db_session.query(FcmToken).all()
        print(fcm_tokens)
        
        # send a test notification to each device
        fcm.send_multiple_notifications([token.token for token in fcm_tokens], "Healthcheck", "This is a test notification")
        
        # FOR TESTING
        # fcm_tokens = ["dnAFw0TDTvCPhzV2YhJeYl:APA91bGiM-YlnIDDEelSp5bZ8O3QxxRgjMghEQwcZHrKDUvnGrRwP8M--pM1AwqJfOCjxQAR3AxCl6kqdeqNh7Nh8P5mXoipnvyopJNq3SqyLOOVgjtlmqo"]
        # fcm.send_multiple_notifications(fcm_tokens, "Healthcheck", "This is a test notification")
        
        time.sleep(20)