#!/bin/bash

# Klaat Development Build & Install Script
# This script builds Klaat and installs it to a local bin directory

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="$HOME/.local/bin"
GRADLEW="./gradlew"

echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     Klaat Development Build & Install         ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""

# Check if local.properties exists
if [ ! -f "$PROJECT_DIR/local.properties" ]; then
    echo -e "${YELLOW}⚠ Warning: local.properties not found${NC}"
    echo -e "${YELLOW}   Google Docs export will not work without OAuth credentials${NC}"
    echo ""
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}✗ Installation cancelled${NC}"
        exit 1
    fi
    echo ""
fi

# Step 1: Clean previous builds
echo -e "${BLUE}🧹 Cleaning previous builds...${NC}"
cd "$PROJECT_DIR"
$GRADLEW clean --quiet

# Step 2: Build the project (dev variant)
echo -e "${BLUE}🔨 Building Klaat (dev variant)...${NC}"
$GRADLEW build -PbuildVariant=dev --quiet

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Step 3: Create distribution
echo -e "${BLUE}📦 Creating dev distribution...${NC}"
$GRADLEW installDist -PbuildVariant=dev --quiet

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Distribution creation failed${NC}"
    exit 1
fi

# Step 4: Create install directory if it doesn't exist
if [ ! -d "$INSTALL_DIR" ]; then
    echo -e "${BLUE}📁 Creating install directory: $INSTALL_DIR${NC}"
    mkdir -p "$INSTALL_DIR"
fi

# Step 5: Copy executable to install directory
echo -e "${BLUE}📋 Installing klaat-dev to $INSTALL_DIR${NC}"

# Check if dev build exists
DEV_BIN="$PROJECT_DIR/build/install/klaat-dev/bin/klaat-dev"
if [ ! -f "$DEV_BIN" ]; then
    echo -e "${RED}✗ Dev build not found at: $DEV_BIN${NC}"
    exit 1
fi

# Remove old installation if it exists
if [ -f "$INSTALL_DIR/klaat-dev" ]; then
    rm "$INSTALL_DIR/klaat-dev"
fi

# Copy the new executable
cp "$DEV_BIN" "$INSTALL_DIR/klaat-dev"
chmod +x "$INSTALL_DIR/klaat-dev"

# Step 6: Check if install directory is in PATH
if [[ ":$PATH:" != *":$INSTALL_DIR:"* ]]; then
    echo ""
    echo -e "${YELLOW}⚠ $INSTALL_DIR is not in your PATH${NC}"
    echo ""
    echo -e "${YELLOW}Add this to your shell profile (~/.bashrc, ~/.zshrc, etc.):${NC}"
    echo ""
    echo -e "${GREEN}    export PATH=\"\$PATH:$INSTALL_DIR\"${NC}"
    echo ""
    echo -e "${YELLOW}Then run: source ~/.bashrc (or ~/.zshrc)${NC}"
    echo ""
else
    echo -e "${GREEN}✓ $INSTALL_DIR is already in your PATH${NC}"
fi

# Step 7: Verify installation
echo ""
echo -e "${BLUE}🔍 Verifying installation...${NC}"

if [ -x "$INSTALL_DIR/klaat-dev" ]; then
    VERSION=$("$INSTALL_DIR/klaat-dev" version 2>/dev/null || echo "unknown")
    echo -e "${GREEN}✓ Klaat-dev installed successfully!${NC}"
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║            Installation Complete!              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  ${BLUE}Executable:${NC} $INSTALL_DIR/klaat-dev"
    echo -e "  ${BLUE}Version:${NC} $VERSION"
    echo ""
    echo -e "${BLUE}Usage:${NC}"
    echo -e "  klaat-dev export examples/simple-test.md -o output/"
    echo -e "  klaat-dev serve -d output/"
    echo ""
else
    echo -e "${RED}✗ Installation verification failed${NC}"
    exit 1
fi
