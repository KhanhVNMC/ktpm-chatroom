# Giai đoạn 1: Dùng image Maven có sẵn JDK 21 để build code
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml trước để tải thư viện (tận dụng Docker cache giúp các lần build sau cực nhanh)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copy toàn bộ code vào và đóng gói + gom toàn bộ thư viện ra thư mục target/dependency
COPY src ./src
RUN mvn -q clean package dependency:copy-dependencies -DincludeScope=runtime -DskipTests

# Giai đoạn 2: Dùng JRE 21 (nhẹ hơn JDK) để chạy app
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy file jar đã build và thư mục thư viện từ Giai đoạn 1 sang
COPY --from=build /app/target/ktpm-chat-1.0.0-SNAPSHOT.jar app.jar
COPY --from=build /app/target/dependency ./lib

# Tạo sẵn 2 thư mục chứa ảnh/file upload của user trong container
RUN mkdir -p usercontent serveravatars

EXPOSE 8080

# Chạy app bằng cách nạp app.jar kèm toàn bộ thư viện trong thư mục lib/
ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "dev.ktpm.App"]