import hashlib
from fastapi.security import HTTPBearer
from openai import OpenAI
from dotenv import load_dotenv
import os

from google.cloud import vision
import re
import io
from math import radians, sin, cos, sqrt, atan2

load_dotenv()

client = OpenAI(
    api_key=os.getenv("OPENAI_API_KEY")
)

def hash_password(password: str) -> str:
    """
    Hashes a password using SHA-256.

    Args:
        password (str): The plain text password.

    Returns:
        str: The hashed password.
    """
    return hashlib.sha256(password.encode()).hexdigest()

# Define the HTTPBearer scheme for Bearer token
bearer_scheme = HTTPBearer()

def predict_category(item_name: str, categories: list) -> str:
    prompt = f"""
    Which category does the item '{item_name}' fit into? 
    Categories: {', '.join(categories)}. Provide only the category name as the answer.
    If you can not find a suitable category, type 'None'.
    """

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": prompt}
        ]
    )

    suggestion = response.choices[0].message.content.strip()
    return suggestion

def get_category_by_name(name: str, categories: list) -> str:
    for category in categories:
        if name.lower().strip() ==  category.name.lower().strip():
            return category.id
    return None

def read_receipt(content):
    client = vision.ImageAnnotatorClient()

    # Create an image object for Vision API
    image = vision.Image(content=content)

    # Send to Google Vision API
    response = client.text_detection(image=image)
    if response.error.message:
        raise Exception(f"Error from Vision API: {response.error.message}")

    # Pull out text
    full_text = response.text_annotations[0].description
    return full_text

def parse_receipt(content):
    text = read_receipt(content)
    
    prompt = f"""
        You will receive some text that was pulled from a receipt.
        Please parse this text and extract the amount, and the descriptor of each line item.
        If there are is a quantity, disregard it.
        If you notice any fees or taxe or subtotals, disregard them.
        If you find the total, return it in the following format ***TOTAL: amount.
        For the output, make sure that each item is on its own row, seperated by newlines.
        The output should be a list of rows in the following format:
        descriptor (string), amount (float).
        The output should be nothing but the list of rows.
        Here is the text: {text}
        """

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": prompt}
        ]
    )

    data = response.choices[0].message.content.strip()
    
    items = []
    total = 0
    
    for line in data.split("\n"):
        if "TOTAL" in line:
            total = float(re.search(r'\d+\.\d+', line).group())
        else:
            items.append({
                "descriptor": line.split(",")[0],
                "amount": float(line.split(",")[1])
            })
            
    return items, total

def get_coordinate_distance(lat1, lon1, lat2, lon2):
    """
    Calculate the distance between two coordinates in kilometers.

    Args:
        lat1 (float): The latitude of the first coordinate.
        lon1 (float): The longitude of the first coordinate.
        lat2 (float): The latitude of the second coordinate.
        lon2 (float): The longitude of the second coordinate.

    Returns:
        float: The distance between the two coordinates in kilometers.
    """
    # approximate radius of earth in km
    R = 6373.0

    lat1 = radians(lat1)
    lon1 = radians(lon1)
    lat2 = radians(lat2)
    lon2 = radians(lon2)

    dlon = lon2 - lon1
    dlat = lat2 - lat1

    a = sin(dlat / 2)**2 + cos(lat1) * cos(lat2) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))

    distance = R * c
    return distance
