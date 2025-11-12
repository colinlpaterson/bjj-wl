# 🥋 BJJ Workout Log (Java Command-Line App)

A simple command-line Java application to log Brazilian Jiu-Jitsu (BJJ) workouts.  
You can record drills, multiple sparring rounds, and detailed round observations.  
Each workout is saved as structured JSON in `data/workouts.jsonl`, making it easy to analyze or import into a database or mobile app later.

---

## Getting Started

### 1️ Clone the Repository
```bash
git clone https://github.com/colinlpaterson/bjj-wl.git
cd "BJJ WL"
```
### 2️ Verify Prerequisites
```bash
java -version    # should show JDK 17 or newer
git --version
```
### 3 Build and Run
```bash
./build.sh
./run.sh

```bash

## Development Workflow

### 1 Edit Code
```bash
nano src/BJJWorkoutLog.java
# or use your editor of choice (IntelliJ, VS Code, etc.)
```

### 2 Compile
```bash
./build.sh
# internally runs: javac -d out src/*.java
```

### 3 Run
```bash
./run.sh
# internally runs: java -cp out BJJWorkoutLog

```

### 4 Commit and Push to Git Hub
```bash
git add .
git commit -m "Describe your change"
git push


```



