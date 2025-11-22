#!/bin/bash

# Script to uninstall, rebuild, and reinstall the debug APK

echo "🗑️  Uninstalling old version..."
adb uninstall com.example.workapp

echo "🔨 Building new APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "📲 Installing new APK..."
    adb install app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ Successfully installed! You can now test the edit/delete features."
        echo ""
        echo "To see edit and delete icons:"
        echo "1. Open the app"
        echo "2. Sign in with your account"
        echo "3. Tap the 'My Jobs' tab in the bottom navigation"
        echo "4. You should see edit (pencil) and delete (trash) icons next to your job listings"
    else
        echo "❌ Installation failed"
    fi
else
    echo "❌ Build failed"
fi