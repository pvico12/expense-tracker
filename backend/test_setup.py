import pytest
import uvicorn
import requests
from multiprocessing import Process
import time

BASE_URL = "http://localhost:8000"

def run_server():
    """Function to run the FastAPI server in a separate process"""
    uvicorn.run("app:app", host="0.0.0.0", port=8000, log_level="info")

@pytest.fixture(scope="module")
def server():
    """Fixture to start and stop the server"""
    proc = Process(target=run_server)
    proc.start()
    time.sleep(10)
    
    print("Server started")
    
    # Check if the server is running
    try:
        response = requests.get(f"{BASE_URL}/healthcheck")
        if response.status_code != 200:
            pytest.fail("Server health check failed")
    except requests.exceptions.ConnectionError:
        pytest.fail("Could not connect to the server for health check")
        
        
    yield
    proc.terminate()
    proc.join()
