import hashlib
from fastapi.security import HTTPBearer
from openai import OpenAI
from dotenv import load_dotenv
import os

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

def getCategorySuggestion(item_name: str, categories: list) -> str:
    prompt = f"Which category does the item '{item_name}' fit into? Categories: {', '.join(categories)}. Provide only the category name as the answer. If you can not find a suitable category, type 'None'."

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": prompt}
        ]
    )

    suggestion = response.choices[0].message.content.strip()
    return suggestion