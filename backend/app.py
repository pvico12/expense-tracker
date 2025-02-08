from fastapi import FastAPI
from routes import auth, user
from db import test_connection, init_db
import uvicorn

app = FastAPI()

app.include_router(auth.router)
app.include_router(user.router)

@app.get("/")
def home():
    return "Hello, World!!!"

@app.get("/healthcheck")
def healthcheck():
    return {"status": "healthy"}

@app.on_event("startup")
def startup():
    if not test_connection():
        print("Database connection failed!")
    init_db()

if __name__ == '__main__':
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)
