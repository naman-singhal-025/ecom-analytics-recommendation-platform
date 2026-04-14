#!/bin/bash

# Simple script to test OpenAI API connection
# Store your actual API key in .env file (never commit it):
#   echo "OPENAI_API_KEY=sk-proj-..." >> .env

# Load from .env if it exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

API_KEY=${OPENAI_API_KEY:-"your-openai-api-key-here"}

echo "Testing OpenAI API connection..."

# Test with the cheapest possible request - embeddings
curl -X POST "https://api.openai.com/v1/embeddings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  -d '{
    "model": "text-embedding-ada-002",
    "input": "test"
  }'

echo -e "\n\n🎯 If you see 'data' with numbers, your API key works!"
echo "💰 If you see 'insufficient_quota', you need to add billing to your OpenAI account"
