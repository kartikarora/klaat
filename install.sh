#!/bin/bash

# Klaat Production Build & Install Script
# This script builds Klaat (production) and installs it to a local bin directory

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
echo -e "${BLUE}║     Klaat Production Build & Install          ║${NC}"
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

# Step 2: Build the project (prod variant)
echo -e "${BLUE}🔨 Building Klaat (production)...${NC}"
$GRADLEW build -PbuildVariant=prod --quiet

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Step 3: Create distribution
echo -e "${BLUE}📦 Creating production distribution...${NC}"
$GRADLEW installDist -PbuildVariant=prod --quiet

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
echo -e "${BLUE}📋 Installing klaat to $INSTALL_DIR${NC}"

# Check if prod build exists
PROD_BIN="$PROJECT_DIR/build/install/klaat/bin/klaat"
if [ ! -f "$PROD_BIN" ]; then
    echo -e "${RED}✗ Production build not found at: $PROD_BIN${NC}"
    exit 1
fi

# Remove old installation if it exists
if [ -f "$INSTALL_DIR/klaat" ]; then
    rm "$INSTALL_DIR/klaat"
fi

# Copy the new executable
cp "$PROD_BIN" "$INSTALL_DIR/klaat"
chmod +x "$INSTALL_DIR/klaat"

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

if [ -x "$INSTALL_DIR/klaat" ]; then
    VERSION=$("$INSTALL_DIR/klaat" version 2>/dev/null || echo "unknown")
    echo -e "${GREEN}✓ Klaat installed successfully!${NC}"
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║            Installation Complete!              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  ${BLUE}Executable:${NC} $INSTALL_DIR/klaat"
    echo -e "  ${BLUE}Version:${NC} $VERSION"
    echo -e "  ${BLUE}Variant:${NC} Production"
    echo ""
    echo -e "${BLUE}Usage:${NC}"
    echo -e "  klaat export examples/simple-test.md -o output/"
    echo -e "  klaat serve -d output/"
    echo ""
else
    echo -e "${RED}✗ Installation verification failed${NC}"
    exit 1
fi
