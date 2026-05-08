#!/bin/bash

# Configuration
REPO_OWNER="Erro22"
REPO_NAME="WebDownloader-OfflineReader"
TOKEN="ВАШ_ТОКЕН_ЗДЕСЬ"

echo "🔍 Fetching latest artifacts from GitHub..."

# Get the ID of the most recent artifact
ARTIFACT_JSON=$(curl -s -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/artifacts?per_page=1")

ARTIFACT_ID=$(echo "$ARTIFACT_JSON" | jq -r '.artifacts[0].id')
ARTIFACT_NAME=$(echo "$ARTIFACT_JSON" | jq -r '.artifacts[0].name')

if [ "$ARTIFACT_ID" == "null" ]; then
    echo "❌ Error: No artifacts found!"
    exit 1
fi

echo "🚀 Found latest artifact: $ARTIFACT_NAME (ID: $ARTIFACT_ID)"
echo "📥 Downloading..."

# Download the artifact
curl -L -H "Authorization: Bearer $TOKEN" \
  -o "latest_build.zip" \
  "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/artifacts/$ARTIFACT_ID/zip"

if [ $? -ne 0 ]; then
    echo "❌ Download failed!"
    exit 1
fi

echo "📦 Extracting..."
unzip -o "latest_build.zip" -d "temp_build"

# Find the APK file
APK_PATH=$(find temp_build -name "*.apk" | head -n 1)

if [ -z "$APK_PATH" ]; then
    echo "❌ No APK found in the archive!"
    rm -rf latest_build.zip temp_build
    exit 1
fi

echo "📲 Installing $APK_PATH on device..."
adb install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo "✅ Success! Application installed and updated."
else
    echo "❌ Installation failed!"
fi

# Cleanup
echo "🧹 Cleaning up..."
rm -rf latest_build.zip temp_build

echo "✨ Done."
