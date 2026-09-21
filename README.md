# My Tuition Manager

**The Math Guide**  
Offline tuition and student management application for Android.

## Overview

My Tuition Manager is an offline-first Android app designed to help tutors manage students, tuition payments, attendance, academic records, batches, routines, notes, library items, daily expenses, receipts, and backups from one place.

## Features

- Student management
- Student search and filtering
- Tuition payment and due tracking
- Payment history
- PDF payment receipts
- Receipt sharing through Android sharing apps
- Attendance management
- Academic records
- Batch management
- Batch routines and notes
- Library/resource management
- Daily expense tracking
- Tuition profile settings
- Security PIN and protected actions
- Theme and appearance settings
- Local JSON backup
- Restore from backup
- Backup sharing through the Android share sheet
- Offline operation

## Data & Privacy

The application is designed to keep tuition data locally on the device.

Personal tutor information is entered through the app's settings/profile screen rather than being embedded as repository defaults.

Backup files may contain sensitive tuition information. Store them securely and avoid sharing them publicly.

## Backup & Restore

Use the app's backup feature to create a local JSON backup of your records.

Recommended practice:

1. Create backups regularly.
2. Keep a copy in a secure location.
3. Do not publish backup files or screenshots containing private student information.
4. After restoring a backup, restart the app if prompted so all restored records are reloaded.

## Technology

- Kotlin
- Jetpack Compose
- Android SDK
- Local device storage
- JSON-based backup and restore

## Development

The project is developed as an offline-first Android application. The stable `main` branch should be kept separate from feature/refactoring work.

## Important

This repository contains application source code only. Do not commit:

- Student personal information
- Phone numbers or addresses
- Tuition/payment records
- Backup JSON files
- Private credentials or PINs
- Private photos or documents
- Generated APKs containing private test data

## License

This project is intended for personal use and development.
