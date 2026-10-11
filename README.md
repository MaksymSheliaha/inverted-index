# Inverted Index Search Engine

Паралельна пошукова система на Java 25 та Spring Boot з використанням інвертованого індексу та семантичного розширення запитів через LLM (Ollama). Проєкт реалізує кастомні багатопотокові структури даних для швидкої індексації великих масивів документів, підтримує REST та WebSocket API, а також містить вебінтерфейс і термінальний CLI-клієнт.

---

## Запуск через Docker Compose (Рекомендовано)

Запуск усіх сервісів (бекенд, Ollama та веб-клієнт) однією командою:

```bash
docker compose up --build -d
```

Після запуску сервіси доступні за адресами:
- **Веб-клієнт**: [http://localhost:3000](http://localhost:3000)
- **Бекенд API**: [http://localhost:8080](http://localhost:8080)
- **Ollama**: [http://localhost:11434](http://localhost:11434)

Зупинка:
```bash
docker compose down
```

---

## Локальне збирання та запуск

### 1. Бекенд (Java 25, Maven)
```bash
# Збирання проєкту
mvn clean package -DskipTests

# Запуск сервісу
java -jar target/inverted-index-0.0.1-SNAPSHOT.jar
```

### 2. Веб-клієнт
```bash
# Компіляція та запуск
javac -d clients/web-client/bin --source-path clients/web-client/src clients/web-client/src/Server.java
java -cp clients/web-client/bin Server
```
*Доступний за адресою [http://localhost:8081](http://localhost:8081).*

### 3. Консольний CLI-клієнт
```bash
python clients/cli/client.py
```
