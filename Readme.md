### **High-Performance Multi-Threaded Web Server in Java**
## 👨‍💻 **Developed By Team Code Kacheri**

## 📌 **Problem Statement**
Traditional web servers like **Tomcat** and **Jetty** often experience performance bottlenecks due to the overhead of handling extreme concurrent requests. The goal of this project is to develop a **custom multi-threaded web server** using **Java NIO (Non-Blocking I/O)** to efficiently process HTTP requests with minimal latency.

## 🚀 **Solution Approach**
We aim to build a **high-performance web server** that:  
✅ Uses **Java NIO** for non-blocking request handling.  
✅ Implements **asynchronous request processing** using **Java Executors & Thread Pools**.  
✅ Integrates an **efficient caching layer** to reduce database calls and enhance performance.

## 🛠 **Key Features**
✔ **Multi-threaded request handling** with optimized thread management.  
✔ **Asynchronous processing** to minimize blocking operations.  
✔ **Efficient caching mechanism** to reduce redundant computations and database queries.  
✔ **Benchmark testing** using Apache Benchmark (`ab`) for performance evaluation.

## 🔥 **Performance Testing Commands**
To evaluate the server's performance, run the following **Apache Benchmark (ab) commands**:

1️⃣ **Test with 10,000 requests & 500 concurrent users:**
```sh
ab -n 10000 -c 500 http://localhost:8080/
```

2️⃣ **Query endpoint testing with 1,000 requests & 50 concurrent users:**
```sh
ab -n 1000 -c 50 http://localhost:8080/query?name=Alice
```

## 🔧 **Challenges & Implementation**
### **1️⃣ Asynchronous Request Handling**
- Utilizes **Java Executors** and **Thread Pools** to manage concurrent client connections efficiently.
- Reduces thread contention and context-switching overhead.

### **2️⃣ Efficient Caching Layer**
- Implements an **in-memory cache** to store frequent query results, reducing repeated database calls.
- Uses **LRU (Least Recently Used) Cache** for optimal performance.

### **3️⃣ Scalability & Load Handling**
- Designed to handle **thousands of concurrent requests** without significant performance degradation.
- Uses **non-blocking I/O (NIO)** to minimize thread blocking issues.

## 📂 **Project Structure**
```
📦 MultiThreadedWebServer
 ┣ 📂 src
 ┃ ┣ 📂 server
 ┃ ┃ ┣ 📜 WebServer.java
 ┃ ┃ ┣ 📜 RequestHandler.java
 ┃ ┃ ┣ 📜 CacheManager.java
 ┃ ┃ ┣ 📜 ThreadPoolManager.java
 ┃ ┣ 📂 tests
 ┃ ┃ ┣ 📜 BenchmarkTests.java
 ┗ 📜 README.md
```

## 📌 **Technologies Used**
🔹 **Java 11+** (for modern concurrency features)  
🔹 **Java NIO (Non-Blocking I/O)**  
🔹 **Apache Benchmark (`ab`)** for load testing  
🔹 **LRU Caching** for performance enhancement

## 📝 **How to Run the Server?**
1️⃣ Clone the repository:
```sh
git clone https://github.com/your-repo/multi-threaded-web-server.git
cd multi-threaded-web-server
```
2️⃣ Compile and run the server:
```sh
javac -d out src/server/*.java
java -cp out server.WebServer
```
3️⃣ Open a browser or use **cURL** to test the server:
```sh
curl http://localhost:8080/
```

## 🎯 **Future Enhancements**
- ✅ Support for **HTTPS & TLS encryption**.
- ✅ Implement **WebSocket support** for real-time applications.
- ✅ Optimize caching strategy with **Redis or Memcached**.  
