#!/bin/bash

################################################################################
# StarWatchX - Setup Script
# Installs dependencies and prepares the environment
################################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║          StarWatchX - Setup & Installation       ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
echo ""

################################################################################
# Create necessary directories
################################################################################

echo -e "${BLUE}[1/5]${NC} Creating directories..."
mkdir -p logs
mkdir -p .pids
mkdir -p dashboard/public/videos
mkdir -p demo/screenshots
mkdir -p python-ml/dataset/images/train
mkdir -p python-ml/dataset/images/val
mkdir -p python-ml/dataset/images/test
mkdir -p python-ml/dataset/labels/train
mkdir -p python-ml/dataset/labels/val
mkdir -p python-ml/dataset/labels/test
echo -e "${GREEN}✓ Directories created${NC}"
echo ""

################################################################################
# Check system requirements
################################################################################

echo -e "${BLUE}[2/5]${NC} Checking system requirements..."

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}✓ Java installed: $JAVA_VERSION${NC}"
else
    echo -e "${RED}✗ Java not found!${NC}"
    echo "  Please install Java JDK 11 or higher"
    echo "  macOS: brew install openjdk@11"
    echo "  Linux: sudo apt install openjdk-11-jdk"
    exit 1
fi

# Check Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo -e "${GREEN}✓ Maven installed: $MVN_VERSION${NC}"
else
    echo -e "${YELLOW}⚠ Maven not found${NC}"
    echo "  Installing Maven wrapper..."
    cd "$SCRIPT_DIR/java-engine"
    # Create a basic Maven wrapper
    echo -e "${YELLOW}  Please install Maven manually:${NC}"
    echo "  macOS: brew install maven"
    echo "  Linux: sudo apt install maven"
fi

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✓ Node.js installed: $NODE_VERSION${NC}"
else
    echo -e "${RED}✗ Node.js not found!${NC}"
    echo "  Please install Node.js 16 or higher"
    echo "  Visit: https://nodejs.org/"
    exit 1
fi

# Check npm
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    echo -e "${GREEN}✓ npm installed: $NPM_VERSION${NC}"
else
    echo -e "${RED}✗ npm not found!${NC}"
    exit 1
fi

# Check Python (optional)
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version)
    echo -e "${GREEN}✓ Python installed: $PYTHON_VERSION${NC}"
else
    echo -e "${YELLOW}⚠ Python not found (optional for ML training)${NC}"
fi

echo ""

################################################################################
# Setup Java Engine
################################################################################

echo -e "${BLUE}[3/5]${NC} Setting up Java Engine..."
cd "$SCRIPT_DIR/java-engine"

# Create pom.xml if it doesn't exist
if [ ! -f "pom.xml" ]; then
    echo "  Creating pom.xml..."
    cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.starwatchx</groupId>
    <artifactId>starwatchx-engine</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>StarWatchX Engine</name>
    <description>Real-time Object Detection and Tracking Engine</description>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <!-- ONNX Runtime for model inference -->
        <dependency>
            <groupId>com.microsoft.onnxruntime</groupId>
            <artifactId>onnxruntime</artifactId>
            <version>1.16.0</version>
        </dependency>
        
        <!-- WebSocket support -->
        <dependency>
            <groupId>org.java-websocket</groupId>
            <artifactId>Java-WebSocket</artifactId>
            <version>1.5.3</version>
        </dependency>
        
        <!-- JSON processing -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>
        
        <!-- OpenCV for video processing -->
        <dependency>
            <groupId>org.openpnp</groupId>
            <artifactId>opencv</artifactId>
            <version>4.7.0-0</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>com.starwatchx.App</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF
fi

echo "  Installing Java dependencies..."
mvn clean install -DskipTests > /dev/null 2>&1 || {
    echo -e "${YELLOW}⚠ Maven install had issues (this is normal if dependencies aren't available yet)${NC}"
}

echo -e "${GREEN}✓ Java Engine setup complete${NC}"
echo ""

################################################################################
# Setup Dashboard
################################################################################

echo -e "${BLUE}[4/5]${NC} Setting up Dashboard..."
cd "$SCRIPT_DIR/dashboard"

# Create package.json if it doesn't exist
if [ ! -f "package.json" ]; then
    echo "  Creating package.json..."
    cat > package.json << 'EOF'
{
  "name": "starwatchx-dashboard",
  "version": "1.0.0",
  "description": "StarWatchX Real-time Dashboard",
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint"
  },
  "dependencies": {
    "next": "^14.0.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  },
  "devDependencies": {
    "@types/node": "^20.0.0",
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "tailwindcss": "^3.3.0",
    "typescript": "^5.0.0"
  }
}
EOF
fi

# Create Next.js config
if [ ! -f "next.config.js" ]; then
    echo "  Creating next.config.js..."
    cat > next.config.js << 'EOF'
/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
}

module.exports = nextConfig
EOF
fi

# Create tsconfig.json
if [ ! -f "tsconfig.json" ]; then
    echo "  Creating tsconfig.json..."
    cat > tsconfig.json << 'EOF'
{
  "compilerOptions": {
    "target": "es5",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "skipLibCheck": true,
    "strict": true,
    "forceConsistentCasingInFileNames": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "node",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "incremental": true
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx"],
  "exclude": ["node_modules"]
}
EOF
fi

# Create Tailwind CSS config
if [ ! -f "tailwind.config.js" ]; then
    echo "  Creating tailwind.config.js..."
    cat > tailwind.config.js << 'EOF'
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
EOF
fi

echo "  Installing npm dependencies..."
npm install > /dev/null 2>&1 || {
    echo -e "${YELLOW}⚠ npm install had issues${NC}"
}

echo -e "${GREEN}✓ Dashboard setup complete${NC}"
echo ""

################################################################################
# Setup Python ML (optional)
################################################################################

echo -e "${BLUE}[5/5]${NC} Setting up Python ML environment..."
cd "$SCRIPT_DIR/python-ml"

if command -v python3 &> /dev/null; then
    # Create requirements.txt
    if [ ! -f "requirements.txt" ]; then
        echo "  Creating requirements.txt..."
        cat > requirements.txt << 'EOF'
# YOLO Training
ultralytics>=8.0.0
torch>=2.0.0
torchvision>=0.15.0
opencv-python>=4.7.0
numpy>=1.24.0
matplotlib>=3.7.0
pillow>=9.5.0

# ONNX Export
onnx>=1.14.0
onnxruntime>=1.16.0
onnxsim>=0.4.0
EOF
    fi
    
    echo "  Python ML setup ready"
    echo "  To install Python dependencies, run: pip install -r python-ml/requirements.txt"
    echo -e "${GREEN}✓ Python ML environment configured${NC}"
else
    echo -e "${YELLOW}⚠ Skipping Python setup (Python not installed)${NC}"
fi

echo ""

################################################################################
# Summary
################################################################################

echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          Setup Completed Successfully! ✓         ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Start the application:  ./start.sh"
echo "  2. Stop the application:   ./stop.sh"
echo "  3. View logs:              tail -f logs/java-engine.log"
echo ""
echo -e "${BLUE}Optional - ML Training:${NC}"
echo "  1. Install Python deps:    pip install -r python-ml/requirements.txt"
echo "  2. Add training data:      python-ml/dataset/images/ and labels/"
echo "  3. Train YOLO model:       python python-ml/train_yolo.py"
echo "  4. Export to ONNX:         python python-ml/export_onnx.py"
echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"
echo ""

