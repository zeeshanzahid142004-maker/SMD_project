# Google Play Store Submission Requirements
## Learnify - AI-Powered Learning Companion

**Document Version:** 1.0  
**Last Updated:** November 2024  
**Prepared for:** Google Play Store Publication

---

## PART 1: APP FILES

### 1.1 App Bundle Information

| Field | Value |
|-------|-------|
| **File Format** | Android App Bundle (.aab) |
| **Package Name** | com.example.learnify |
| **Version Code** | 1 |
| **Version Name** | 1.0 |
| **Build Type** | Release |
| **File Name** | app-release.aab |
| **File Size** | ___________________ MB |
| **Upload Date** | ___________________ |

### 1.2 App Signing

| Field | Value |
|-------|-------|
| **Signing Method** | ☐ Google Play App Signing (Recommended) |
|                    | ☐ Self-signed |
| **Keystore File** | ___________________ |
| **Key Alias** | ___________________ |
| **Validity Period** | ___________________ |

### 1.3 Build Configuration

```
applicationId = "com.example.learnify"
minSdk = 26
targetSdk = 36
compileSdk = 36
versionCode = 1
versionName = "1.0"
```

---

## PART 2: VISUAL ASSETS

### 2.1 App Icon (Required)

| Specification | Requirement | Status |
|--------------|-------------|--------|
| **File Name** | ic_launcher-playstore.png | ☐ Available |
| **Dimensions** | 512 x 512 pixels | ☐ Verified |
| **Format** | PNG (32-bit, no alpha) | ☐ Verified |
| **Location** | `app/src/main/ic_launcher-playstore.png` | ☐ Exists |

### 2.2 Feature Graphic (Required)

| Specification | Requirement | Status |
|--------------|-------------|--------|
| **File Name** | ___________________ | ☐ Created |
| **Dimensions** | 1024 x 500 pixels | ☐ Verified |
| **Format** | JPEG or PNG (24-bit, no alpha) | ☐ Verified |
| **Content** | App branding, key features | ☐ Designed |

### 2.3 Screenshots (Required)

#### Phone Screenshots (Minimum 2, Maximum 8)

| # | Screen Name | Dimensions | Status |
|---|-------------|------------|--------|
| 1 | Splash/Onboarding | 1080 x 1920 (or 16:9) | ☐ Captured |
| 2 | Main Dashboard | 1080 x 1920 (or 16:9) | ☐ Captured |
| 3 | Quiz Interface | 1080 x 1920 (or 16:9) | ☐ Captured |
| 4 | Coding Exercise | 1080 x 1920 (or 16:9) | ☐ Captured |
| 5 | Recommendations | 1080 x 1920 (or 16:9) | ☐ Captured |
| 6 | History/Progress | 1080 x 1920 (or 16:9) | ☐ Captured |

#### Tablet Screenshots (Optional but Recommended)

| # | Screen Name | Dimensions | Status |
|---|-------------|------------|--------|
| 1 | ___________________ | 1920 x 1200 (or 16:10) | ☐ Captured |
| 2 | ___________________ | 1920 x 1200 (or 16:10) | ☐ Captured |

### 2.4 Promotional Video (Optional)

| Field | Value |
|-------|-------|
| **YouTube URL** | ___________________ |
| **Duration** | 30 seconds - 2 minutes |

### 2.5 Visual Assets Checklist

- [ ] App Icon (512 x 512 PNG) - Available at `app/src/main/ic_launcher-playstore.png`
- [ ] Feature Graphic (1024 x 500 JPEG/PNG)
- [ ] Minimum 2 phone screenshots
- [ ] Screenshots show actual app content
- [ ] No device frames in screenshots (unless using Device Art Generator)
- [ ] No alpha/transparency in graphics

---

## PART 3: APP INFORMATION

### 3.1 App Title

| Field | Value |
|-------|-------|
| **App Name** | Learnify |
| **Character Count** | 8 characters (Max: 30) |
| **Language** | English (US) - en-US |

### 3.2 Short Description

**Character Limit:** 80 characters maximum

```
AI-powered learning companion with quizzes, coding exercises & smart recommendations
```

**Character Count:** 83 characters

**Alternative (within limit):**
```
AI-powered learning companion with quizzes, coding exercises & recommendations
```
**Character Count:** 78 characters ✓

### 3.3 Full Description

**Character Limit:** 4,000 characters maximum

```
🎓 LEARNIFY - Your AI-Powered Learning Companion

Transform your learning journey with Learnify, the intelligent educational app designed to make studying more effective, engaging, and personalized.

📚 SMART QUIZ SYSTEM
• Take interactive quizzes across various subjects
• Get instant feedback on your answers
• Review your quiz results with detailed explanations
• Track your progress over time

💻 CODING EXERCISES
• Practice programming skills with hands-on exercises
• Learn coding concepts through practical examples
• Get real-time feedback on your code
• Build your programming portfolio

🤖 AI-POWERED FEATURES
• Receive personalized learning recommendations
• Get smart suggestions based on your performance
• Adaptive learning paths tailored to your needs
• AI tutor assistance for better understanding

📊 PROGRESS TRACKING
• Comprehensive history of all your activities
• View your learning statistics and achievements
• Monitor your improvement over time
• Set and track learning goals

📖 DOCUMENT HANDLING
• Import documents and images for study
• OCR text recognition for scanning notes
• Organize your study materials efficiently

✨ USER-FRIENDLY FEATURES
• Clean, intuitive interface
• Smooth onboarding experience
• Secure sign-in with Google authentication
• Cloud sync across devices via Firebase

🔒 PRIVACY & SECURITY
• Secure authentication with Firebase
• Your data is protected and encrypted
• No third-party ads
• Transparent data usage policies

Perfect for students, lifelong learners, and anyone looking to enhance their knowledge through an intelligent, personalized learning experience.

Download Learnify today and start your smarter learning journey!

Keywords: learning, education, quiz, coding, AI tutor, study, practice, personalized learning, educational app, smart recommendations
```

### 3.4 Category & Tags

| Field | Value |
|-------|-------|
| **Primary Category** | Education |
| **Secondary Category** | (Optional) ___________________ |
| **Content Rating** | Everyone |

**Keywords/Tags:**
- learning
- education
- quiz
- coding
- AI tutor
- study
- practice
- personalized learning
- educational app
- smart recommendations

### 3.5 Contact Information

| Field | Value |
|-------|-------|
| **Developer Name** | ___________________ |
| **Email Address** | rana.waqasali@nu.edu.pk |
| **Phone Number** | ___________________ |
| **Website** | ___________________ |
| **Physical Address** | ___________________ |

### 3.6 Privacy Policy

| Field | Value |
|-------|-------|
| **Privacy Policy URL** | ___________________ |
| **Status** | ☐ Required (App collects user data) |

> **Note:** A privacy policy URL is REQUIRED because this app:
> - Collects personal information (name, email via Firebase Auth)
> - Stores user activity data (quiz history, progress)
> - Uses Firebase/Google services for data processing

---

## PART 4: TECHNICAL REQUIREMENTS

### 4.1 SDK Requirements

| Specification | Value | Google Play Requirement |
|--------------|-------|------------------------|
| **Target API Level** | 36 (Android 15) | ✓ Meets requirement (33+) |
| **Minimum API Level** | 26 (Android 8.0 Oreo) | ✓ Acceptable |
| **Compile SDK** | 36 | ✓ Current |

### 4.2 Architecture Support

| Architecture | Supported |
|-------------|-----------|
| **ARM 32-bit (armeabi-v7a)** | ☐ Yes ☐ No |
| **ARM 64-bit (arm64-v8a)** | ☐ Yes (Required) |
| **x86** | ☐ Yes ☐ No |
| **x86_64** | ☐ Yes ☐ No |

> **Note:** Google Play requires 64-bit support for all apps.

### 4.3 Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| `INTERNET` | Required for Firebase authentication, quiz data sync, and AI features | Yes |
| `ACCESS_NETWORK_STATE` | To check network connectivity before making API calls | Yes |
| `READ_EXTERNAL_STORAGE` (maxSdkVersion=32) | To allow users to import documents and images (Android 12 and below) | Yes |
| `READ_MEDIA_IMAGES` | To access images for OCR text recognition (Android 13+) | Yes |
| `CAMERA` | To allow users to capture images for OCR scanning | Yes |

### 4.4 Hardware Features

| Feature | Required |
|---------|----------|
| `android.hardware.camera` | No (optional) |

### 4.5 Third-Party Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| **Firebase Auth** | Latest | User authentication |
| **Firebase Database** | Latest | Real-time data storage |
| **Firebase Firestore** | Latest | Cloud data storage |
| **Google Play Services Auth** | 20.7.0 | Google Sign-In |
| **Google Drive API** | v3-rev20230822-2.0.0 | Cloud storage integration |
| **Google Auth Library** | 1.19.0 | OAuth authentication |
| **Retrofit** | 2.9.0 | HTTP client for API calls |
| **OkHttp** | 4.12.0 | Networking |
| **Glide** | 4.15.1 | Image loading and caching |
| **ML Kit Text Recognition** | 16.0.0 | OCR functionality |
| **YouTube Player** | 12.1.0 | Video content playback |
| **PDFBox Android** | 2.0.27.0 | PDF document handling |
| **Apache POI** | 5.2.3 | Document processing (Word, Excel) |
| **Lottie** | 6.1.0 | Animations |
| **RichEditor** | 2.0.0 | Rich text editing for notes |
| **Konfetti** | 2.0.2 | Celebration effects |
| **Shimmer** | 0.5.0 | Loading effects |
| **Guava** | 31.1-android | Utility functions |
| **Gson** | 2.10.1 | JSON parsing |

---

## PART 5: DATA SAFETY & PRIVACY

### 5.1 Data Collection Declaration

| Question | Answer |
|----------|--------|
| **Does your app collect or share user data?** | ☐ Yes |
| **Is all collected data encrypted in transit?** | ☐ Yes |
| **Do you provide a way for users to request data deletion?** | ☐ Yes ☐ No |

### 5.2 Data Types Collected

#### Personal Information

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Name | ☐ Yes | ☐ No | User profile, personalization |
| Email Address | ☐ Yes | ☐ No | Account authentication |
| User IDs | ☐ Yes | ☐ No | Account identification |

#### App Activity

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| App interactions | ☐ Yes | ☐ No | Improve user experience |
| Quiz history | ☐ Yes | ☐ No | Progress tracking |
| Learning progress | ☐ Yes | ☐ No | Personalized recommendations |

#### Device Information

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Device identifiers | ☐ Yes | ☐ No | Analytics, crash reporting |

### 5.3 Data Usage

| Purpose | Applicable |
|---------|------------|
| App functionality | ☐ Yes |
| Analytics | ☐ Yes |
| Developer communications | ☐ Yes |
| Fraud prevention, security | ☐ Yes |
| Personalization | ☐ Yes |
| Account management | ☐ Yes |

### 5.4 Third-Party Data Sharing

| Service | Data Shared | Purpose |
|---------|-------------|---------|
| Firebase (Google) | User account info, app usage | Authentication, data storage |
| Google Play Services | Authentication tokens | Sign-in functionality |
| ML Kit (Google) | Images (processed locally) | OCR text recognition |

### 5.5 Data Retention & Deletion

| Policy | Description |
|--------|-------------|
| **Data Retention** | User data is retained while account is active |
| **Data Deletion** | Users can request deletion via: ___________________ |
| **Account Deletion** | Users can delete their account through: ___________________ |

---

## PART 6: CONTENT RATING

### 6.1 Content Rating Questionnaire

| Category | Response |
|----------|----------|
| **Violence** | ☐ No |
| **Fear** | ☐ No |
| **Sexuality** | ☐ No |
| **Nudity** | ☐ No |
| **Hate Speech** | ☐ No |
| **Drugs** | ☐ No |
| **Alcohol** | ☐ No |
| **Tobacco** | ☐ No |
| **Gambling** | ☐ No |
| **Profanity/Crude Humor** | ☐ No |
| **User Interaction** | ☐ Yes (Account features, no direct user-to-user interaction) |
| **Shares Location** | ☐ No |
| **Allows Purchases** | ☐ No |

### 6.2 Expected Content Rating

| Rating Body | Expected Rating |
|-------------|-----------------|
| **ESRB** | Everyone |
| **PEGI** | 3 |
| **GRAC** | All |
| **IARC** | Generic |

### 6.3 Target Audience

| Question | Answer |
|----------|--------|
| **Target Age Group** | ☐ Everyone (All ages) |
| **Appeals to Children?** | ☐ Yes ☐ No |
| **Teacher Approved?** | ☐ Applying for badge ☐ Not applying |

### 6.4 Ads Declaration

| Question | Answer |
|----------|--------|
| **Contains Ads?** | ☐ No |
| **Ad SDK integrated?** | ☐ No |

---

## PART 7: TESTING INFORMATION

### 7.1 Test Credentials

| Field | Value |
|-------|-------|
| **Test Email** | ___________________ |
| **Test Password** | ___________________ |
| **Google Account** | ___________________ |

> **Note:** Provide valid test credentials if the app requires login to access features.

### 7.2 Testing Instructions

**Step-by-Step Flow:**

1. **Launch App**
   - App displays splash screen
   - Automatically navigates to onboarding or main screen

2. **Onboarding (First-time users)**
   - Swipe through onboarding screens
   - Learn about app features

3. **Sign Up / Sign In**
   - Create new account with email/password, OR
   - Sign in with existing credentials, OR
   - Use Google Sign-In

4. **Main Dashboard**
   - View available quizzes and activities
   - Access different learning modules

5. **Quiz Feature**
   - Select a quiz topic
   - Answer questions
   - View results and review answers

6. **Coding Exercises**
   - Select a coding exercise
   - Write and submit code
   - View feedback

7. **Recommendations**
   - View personalized learning suggestions
   - Access recommended content

8. **History**
   - View past quiz attempts
   - Review learning progress

### 7.3 Known Issues / Limitations

| Issue | Description | Severity |
|-------|-------------|----------|
| ___________________ | ___________________ | Low/Medium/High |
| ___________________ | ___________________ | Low/Medium/High |
| ___________________ | ___________________ | Low/Medium/High |

### 7.4 Device Testing Matrix

| Device/Emulator | Android Version | Status |
|-----------------|-----------------|--------|
| ___________________ | ___________________ | ☐ Tested |
| ___________________ | ___________________ | ☐ Tested |
| ___________________ | ___________________ | ☐ Tested |

---

## PART 8: LEGAL & COMPLIANCE

### 8.1 Declarations

By submitting this application, I declare that:

- [ ] The app complies with Google Play Developer Program Policies
- [ ] The app complies with Google Play Developer Distribution Agreement
- [ ] The app does not contain any prohibited content
- [ ] The app does not infringe on any third-party intellectual property
- [ ] All permissions requested are necessary for app functionality
- [ ] The privacy policy accurately describes data collection and usage
- [ ] The app has been tested on multiple devices and configurations
- [ ] The app bundle is signed with a valid signing key
- [ ] All third-party libraries are properly licensed
- [ ] The app does not contain malware or deceptive behavior

### 8.2 Export Compliance

| Question | Answer |
|----------|--------|
| **Does the app use encryption?** | ☐ Yes (HTTPS, Firebase) |
| **Is encryption exempt from export regulations?** | ☐ Yes (Standard encryption protocols) |

### 8.3 Children's Privacy (COPPA)

| Question | Answer |
|----------|--------|
| **Is the app directed at children under 13?** | ☐ No |
| **Does the app collect data from children?** | ☐ No (requires account) |

### 8.4 Signatures

| Field | Value |
|-------|-------|
| **Student Name** | ___________________ |
| **Student ID** | ___________________ |
| **Signature** | ___________________ |
| **Date** | ___________________ |

| Field | Value |
|-------|-------|
| **Supervisor/Instructor Name** | ___________________ |
| **Signature** | ___________________ |
| **Date** | ___________________ |

---

## SUBMISSION CHECKLIST

### Pre-Submission Checklist

#### App Files
- [ ] Android App Bundle (.aab) generated successfully
- [ ] App is signed with release key
- [ ] Version code and version name are set correctly
- [ ] 64-bit architecture support verified

#### Visual Assets
- [ ] App Icon (512 x 512 PNG) - `app/src/main/ic_launcher-playstore.png`
- [ ] Feature Graphic (1024 x 500 JPEG/PNG)
- [ ] Minimum 2 phone screenshots uploaded
- [ ] Screenshots are clear and representative

#### Store Listing
- [ ] App title entered (Learnify)
- [ ] Short description (max 80 characters)
- [ ] Full description (comprehensive)
- [ ] Category selected (Education)
- [ ] Contact email provided
- [ ] Privacy policy URL added

#### Technical
- [ ] Target API level meets requirements (36)
- [ ] All permissions explained
- [ ] Third-party libraries documented

#### Data Safety
- [ ] Data collection questionnaire completed
- [ ] Data sharing declarations accurate
- [ ] Privacy policy matches declarations

#### Content Rating
- [ ] Rating questionnaire completed
- [ ] Expected rating: Everyone

#### Legal
- [ ] All declarations checked and signed
- [ ] No policy violations

#### Testing
- [ ] Test credentials provided (if applicable)
- [ ] Testing instructions documented
- [ ] Known issues listed

---

## SUBMISSION DETAILS

| Field | Value |
|-------|-------|
| **Submission Email** | rana.waqasali@nu.edu.pk |
| **Submission Date** | ___________________ |
| **Submitted By** | ___________________ |
| **Reviewer** | ___________________ |

---

## APPENDIX A: FILE LOCATIONS

| Asset | Location in Repository |
|-------|----------------------|
| App Module | `/app` |
| Build Configuration | `/app/build.gradle.kts` |
| Android Manifest | `/app/src/main/AndroidManifest.xml` |
| App Icon (Play Store) | `/app/src/main/ic_launcher-playstore.png` |
| App Icons (Various DPI) | `/app/src/main/res/mipmap-*/` |
| String Resources | `/app/src/main/res/values/strings.xml` |
| Activities | `/app/src/main/java/com/example/learnify/` |

---

## APPENDIX B: APP ACTIVITIES SUMMARY

| Activity | Description | Exported |
|----------|-------------|----------|
| SplashActivity | App launch screen | Yes (LAUNCHER) |
| Welcome_Page_Activity | Welcome page | No |
| OnboardingActivity | User onboarding flow | No |
| signin_activity | User sign in | No |
| signup_activity | User registration | No |
| ForgotPasswordActivity | Password recovery | No |
| CreateNewPasswordActivity | Password reset | No |
| MainActivity | Main app dashboard | Yes |
| QuizActivity | Quiz functionality | No |
| QuizReviewActivity | Quiz results review | No |
| CodingExerciseActivity | Coding exercises | No |
| RecommendationsActivity | Personalized recommendations | No |
| HistoryActivity | User activity history | No |

---

**Document End**

*This document contains all the information required for Google Play Store submission of the Learnify application. Please fill in all blank fields marked with "___________________" before final submission.*
