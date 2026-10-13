import random
from locust import HttpUser, task, between

QUERIES = [
    "headphones",
    "wireless headphones",
    "phone charger",
    "usb cable",
    "bluetooth speaker",
    "camera lens",
    "gaming mouse",
    "mechanical keyboard",
    "laptop stand",
    "screen protector",
    "power bank",
    "smart watch",
    "noise canceling headphones",
    "fast charging cable",
    "dual monitor stand"
]

class SearchUser(HttpUser):
    host = "http://localhost:8080"
    
    wait_time = between(0.1, 0.3)

    @task
    def search(self):
        query = random.choice(QUERIES)
        payload = {
            "query": query,
            "mode": "KEYWORD",
            "page": 0,
            "size": 10
        }
        self.client.post("/api/search", json=payload)
