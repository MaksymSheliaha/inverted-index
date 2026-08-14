#!/bin/bash

echo "Starting Ollama server..."
ollama serve &

sleep 5

echo "Ollama is ready, pulling the lightweight model..."
ollama pull llama3.2:1b

wait