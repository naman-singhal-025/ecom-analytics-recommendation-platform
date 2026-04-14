#!/bin/bash

echo "🦙 Testing Ollama Installation..."
echo "================================="

# Test 1: Check if Ollama is running
echo "1️⃣ Checking Ollama service..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✅ Ollama is running!"
else
    echo "❌ Ollama is not running. Starting it..."
    brew services start ollama
    sleep 3
fi

# Test 2: List available models
echo -e "\n2️⃣ Available models:"
ollama list

# Test 3: Test embedding generation
echo -e "\n3️⃣ Testing embeddings with nomic-embed-text..."
curl -s -X POST http://localhost:11434/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "model": "nomic-embed-text",
    "prompt": "Hello, this is a test for embeddings"
  }' | head -c 200

echo -e "\n"

# Test 4: Test text generation
echo "4️⃣ Testing text generation with llama3.1:8b..."
curl -s -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3.1:8b",
    "prompt": "What is artificial intelligence?",
    "stream": false
  }' | head -c 300

echo -e "\n\n================================="
echo "🎯 Results:"
echo "- If you see embedding arrays: Embeddings work! ✅"
echo "- If you see text responses: Text generation works! ✅"
echo "- If you see errors: Check if models are downloaded"