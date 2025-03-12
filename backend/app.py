from notifications import push_notification_healthcheck
from fastapi import FastAPI
from routes import auth, user, transaction, statistics, tools, category, deals, recurring_transaction, goals
from db import test_connection, init_db
import uvicorn
import logging
import asyncio

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("app.log"),  # Save logs to a file
        logging.StreamHandler()          # Also output to console
    ]
)
logger = logging.getLogger(__name__)

app = FastAPI()

app.include_router(auth.router)
app.include_router(user.router)
app.include_router(category.router)
app.include_router(transaction.router)
app.include_router(recurring_transaction.router)
app.include_router(statistics.router)
app.include_router(tools.router)
app.include_router(deals.router)
app.include_router(goals.router)

@app.get("/")
def home():
    return "Hello, World!!!"

@app.get("/healthcheck")
def healthcheck():
    return {"status": "healthy"}

@app.on_event("startup")
async def startup():
    if not test_connection():
        print("Database connection failed!")
    init_db()
    
    # Start the healthcheck thread
    asyncio.create_task(push_notification_healthcheck())
    
if __name__ == '__main__':
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)

## uvicorn app:app --reload
