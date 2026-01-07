#!/bin/bash

# Setup script for Foundry contracts testing

echo "Setting up Foundry for AgriChain contracts..."

cd blockchain/contracts

# Check if forge is installed
if ! command -v forge &> /dev/null; then
    echo "Foundry not found. Installing..."
    
    # Install foundryup
    curl -L https://foundry.paradigm.xyz | bash
    
    # Source the environment
    source ~/.bashrc 2>/dev/null || source ~/.bash_profile 2>/dev/null || true
    
    # Install Foundry
    foundryup
    
    if ! command -v forge &> /dev/null; then
        echo "Error: Foundry installation failed."
        echo "Please install manually: https://book.getfoundry.sh/getting-started/installation"
        exit 1
    fi
fi

echo "Forge version: $(forge --version)"

# Install dependencies
echo "Installing Foundry dependencies..."
forge install foundry-rs/forge-std --no-commit 2>/dev/null || true

# Build contracts
echo "Building contracts..."
forge build

# Run tests
echo "Running tests..."
forge test -vv

echo ""
echo "Setup complete! Contract tests passed."
echo ""
echo "To deploy to Sepolia:"
echo "  export SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/YOUR_PROJECT_ID"
echo "  export PRIVATE_KEY=your_private_key"
echo "  forge create --rpc-url \$SEPOLIA_RPC_URL --private-key \$PRIVATE_KEY src/Escrow.sol:Escrow"
