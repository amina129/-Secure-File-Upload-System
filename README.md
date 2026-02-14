# -Secure-File-Upload-System
File Upload System

A secure file upload system built with Spring Boot that validates files, detects duplicates, and automatically cleans up old files.
📋 Project Overview

This application provides a safe way to upload files with built-in security measures:

    Size Limiting: Files larger than 5MB are automatically rejected

    Type Validation: Only image files (JPG, PNG, GIF, WEBP) are accepted

    Duplicate Detection: Uses SHA-256 hashing to identify identical files and reuse storage

    Auto-Cleanup: Files older than 24 hours are automatically deleted

    User Interface: Simple web page for uploading files

    REST API: Programmatic access for integrations

🚀 How to Run
Prerequisites

    Java 17 or higher installed

    Maven installed (or use the included Maven wrapper)

Step 1: Clone or Download
bash

git clone <repository-url>
cd file-upload-system

Step 2: Build the Project
bash

# Using Maven
mvn clean install

# OR using Maven wrapper
./mvnw clean install

Step 3: Run the Application
bash

# Using Maven
mvn spring-boot:run

# OR using Maven wrapper
./mvnw spring-boot:run

# OR run the JAR directly
java -jar target/file-upload-system-0.0.1-SNAPSHOT.jar

Step 4: Access the Application

    Web Interface: Open your browser and go to http://localhost:8080

    API Endpoints: Available at http://localhost:8080/api/*

🎯 What You Can Do
Upload Files via Web Interface

    Open http://localhost:8080 in your browser

    Click "Choose File" and select an image

    Click "Upload Image"

    See the result with file details

Upload Files via API
bash

# Upload a file
curl -F "file=@/path/to/your/image.jpg" http://localhost:8080/api/upload

# Check system statistics
curl http://localhost:8080/api/stats

# Manually trigger cleanup
curl -X POST http://localhost:8080/api/cleanup

📁 What Happens When You Upload

    Validation: File size and type are checked

    Hashing: A unique fingerprint (SHA-256) is created

    Deduplication: If the file already exists, storage is reused

    Storage: New files are saved in ./uploads/YYYY/MM/DD/

    Tracking: Metadata is stored for cleanup and statistics

⏰ Auto-Cleanup

    Files are automatically checked every hour

    Files older than 24 hours are deleted

    Empty folders are cleaned up automatically

    You can also trigger cleanup manually via API

🔧 Configuration

The main settings are in src/main/resources/application.yml:
yaml

upload:
  max-file-size: 5242880        # 5MB in bytes
  upload-dir: ./uploads          # Where files are stored
  retention-hours: 24            # How long files are kept

📊 Understanding the Output

When you upload a file, you'll see:

    File ID: Unique identifier for the file

    Duplicate Status: Whether storage was reused

    File Size: Size in KB/MB

    File Type: Detected MIME type

🛑 Stopping the Application

Press Ctrl+C in the terminal where the application is running.
📝 Notes

    The uploads directory (./uploads) is created automatically

    Logs are written to ./logs/file-upload.log

    Default port is 8080 (change in application.yml if needed)

    All times are in your system timezone

