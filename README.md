# seatly
Initial codebase for technical interviews

## Requirements
- Java 17
- Kotlin (JVM)
- Gradle
- Docker
- Docker Compose


## Running The App
```bash
cd infra
docker-compose up -d
cd ..

cd backend
./gradlew run
cd ..

```

## Running Backend Tests
```bash
cd infra
docker-compose up -d
cd ..

cd backend
./gradlew test
cd ..
```
