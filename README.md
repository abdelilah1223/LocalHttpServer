# HttpLocalServer

A lightweight Android HTTP server application that allows device-to-device communication through a Ktor-based server implementation. Built with Android Service architecture for reliable background execution.

## Project Structure and Implementation Details

### Key Components:
1. **LocalHttpServer.kt** (Ktor-based server implementation)
   - Creates an embedded Netty server using Ktor
   - Stores uploaded files in `context.getExternalFilesDir("uploads")`
   - Serves content on port 8080 (default) with following endpoints:
     - `GET /` - Serves `index.html` from assets
     - `POST /upload` - Handles file uploads via multipart/form-data
     - `GET /data` - Returns JSON list of uploaded files
     - `GET /download/{name}` - Downloads specific files
     - `DELETE /file/{name}` - Deletes specific files
     - `GET /download-app` - Downloads the app's APK file

2. **ServerService.kt** (Background service handler)
   - Android Service implementation that maintains server operation
   - Creates persistent notifications for server status
   - Handles server lifecycle (start/stop) through intents

### Technology Stack:
- Android Service for reliable background execution
- Ktor framework for HTTP server implementation
- Kotlinx serialization for JSON handling
- Android SDK for system integration

## Core Features

1. **Embedded HTTP Server**
   - Runs using Ktor embedded server on Android
   - Listens on port 8080 (can be customized)
   - Supports GET, POST, and DELETE HTTP methods

2. **File Handling System**
   - Stores user-uploaded files in app's external storage directory
   - Allows browsing uploaded files through JSON API endpoint
   - Provides file download/delete functionality
   - Includes mechanism to share app package (APK file)

3. **Background Service**
   - Maintains server operation in foreground service
   - Shows persistent notification with stop control
   - Survives app termination through START_STICKY flag

4. **Static Content Delivery**
   - Serves initial HTML page from assets directory
   - Demonstrates basic web interface functionality

## Getting Started

To build and run the project:
[Download here ](https://abdelilah.wuaze.com/archive/apps)
OR
1. Install Android Studio (Chipmunk version or newer)
2. Clone the repository:
   ```shell
   git clone https://github.com/abdelilah1223/LocalHttpServer.git
   ```
3. Import the project into Android Studio
4. Minimum SDK: API 24+ (Android 7.0 Nougat)
5. Connect a physical device/AVD with proper storage permissions
6. Click Run (or Shift+F10) to deploy the application

## Development Roadmap

### Potential enhancements:
1. **Security Improvements**
   - Implement authentication (JWT, Basic, etc.)
   - Add TLS/HTTPS support for secure connections
   - Implement permissions system for file access

2. **Advanced Features**
   - Add WebSocket support for real-time communication
   - Implement REST API documentation (Swagger/OpenAPI)
   - Develop dynamic page generation (Kotlinx.html or template engine)
   - Add request routing/middleware system

3. **Monitoring & Management**
   - Build request analytics dashboard
   - Implement rate-limiting and access control
   - Add real-time logging interface
   - Create configuration system (SharedPreferences/DataStore)

4. **User Experience**
   - Expand file management UI in the app
   - Add multiple-upload support with progress tracking
   - Implement folder structure support
   - Add sorting/filtering for file list

5. **Cross-Platform Integration**
   - Create client SDK for easier integration
   - Implement UPnP for network discovery
   - Add Bluetooth/WiFi Direct support

## Contributing

Contributions are welcome! Please follow these guidelines:
- Follow existing code style and Ktor best practices
- Document public methods with KotlinDoc
- Include unit tests for new functionality
- Use Android's security best practices
- Consider battery optimization strategies

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

This project provides a functional starting point for an embedded HTTP server in Android applications. It demonstrates key concepts for implementing network services while maintaining proper app architecture and user experience.
