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
    time.sleep(5)
    yield
    proc.terminate()
    proc.join()
