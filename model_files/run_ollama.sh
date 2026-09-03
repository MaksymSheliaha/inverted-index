#!/bin/bash

echo "Starting Ollama server..."
ollama serve &

sleep 5

echo "Ollama is ready, pulling the model..."
ollama pull llama3.2:3b

wait