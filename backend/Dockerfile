# NOTE: create requirements.txt file by running the following command in the terminal
# MAKE SURE YOU HAVE ACTIVATED A VIRTUAL ENVIRONMENT
# pip freeze > requirements.txt
# then you can build the docker image using this file

# Use an official Python runtime as a parent image
FROM python:3.10-slim

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app

# Update pip
RUN pip install --upgrade pip

# Install the packages from requirements.txt
RUN pip install -r requirements.txt

# Make port 8000 available to the world outside this container
EXPOSE 8000

# Run app.py when the container launches
CMD ["gunicorn", "app:app", "--bind", "0.0.0.0:8000", "-k", "uvicorn.workers.UvicornWorker"]
