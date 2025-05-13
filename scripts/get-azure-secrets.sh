#!/bin/bash

# Check if an argument is provided
if [ "$#" -ne 1 ]; then
    #echo "Usage: $0 <azureapp_name>"
    exit 1
fi

AZURE_APP_NAME="$1"
NAMESPACE="teammelosys"

# Debug info
kubectl config current-context &>/dev/null

# Get the secret name from the Azure app
SECRET_NAME=$(kubectl get azureapp "$AZURE_APP_NAME" -n "$NAMESPACE" --template='{{.spec.secretName}}')

if [ -z "$SECRET_NAME" ]; then
    exit 1
fi

kubectl get secret -n "$NAMESPACE" "$SECRET_NAME" -ojsonpath='{.data.AZURE_APP_CLIENT_SECRET}' | /usr/bin/base64 -d
