import math
import random
from locust import HttpUser, task, between, LoadTestShape

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


class StepLoadShape(LoadTestShape):

    step_time = 10
    step_users = 5
    spawn_rate = 5
    max_users = 200
    hold_time_at_max = 20

    def tick(self):
        run_time = self.get_run_time()

        time_to_max = (self.max_users / self.step_users) * self.step_time
        total_time = time_to_max + self.hold_time_at_max

        # Зупиняємо тест після досягнення ліміту часу
        if run_time > total_time:
            return None

        current_step = math.floor(run_time / self.step_time) + 1
        user_count = min(current_step * self.step_users, self.max_users)

        return (user_count, self.spawn_rate)

