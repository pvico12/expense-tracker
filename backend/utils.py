import hashlib
from fastapi.security import HTTPBearer

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