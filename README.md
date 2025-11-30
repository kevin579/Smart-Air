# Smart Air - Asthma Management for Kids & Parents

Smart Air is a kid-friendly Android application designed to help children (ages 6–16) manage their asthma effectively. It empowers families to track symptoms, practice good inhaler techniques, and safely share data with healthcare providers.

---

## Table of Contents
* [Project Overview](#-project-overview)
* [User Roles](#-user-roles)
* [Key Features](#-key-features)
* [Technical Architecture](#-technical-architecture)
* [Setup & Installation](#-setup--installation)
* [Safety Disclaimer](#️-safety-disclaimer)

---

## Project Overview
* **Target Audience:** Children (6-16), Parents, and Healthcare Providers.
* **Core Goal:** To bridge the gap between home care and clinical oversight through gamified logging, real-time alerts, and granular privacy controls.

---

## User Roles

### 1. Child
**Capabilities:**
* Log rescue and controller medicine.
* View "Today's Zone" (Green/Yellow/Red).
* Gamified technique helper with step-by-step prompts.
* Earn badges and streaks for adherence.
* Report symptoms and triggers via a simple UI.

### 2. Parent
**Capabilities:**
* Link and manage multiple child profiles.
* Dashboard: View charts, last rescue time, and weekly summaries.
* Inventory: Track medication levels and expiry dates.
* Control: Set Personal Best (PEF) values and configure Action Plans.
* Sharing: Granular control over what data is shared with Providers (real-time toggles).
* Alerts: Receive notifications for "Red Zone" days, rapid rescue usage ( $\ge$ 3 in 3 hours), or low canister levels.

### 3. Provider
**Capabilities:**
* Read-only access.
* View summaries and reports only when explicitly enabled by the Parent.
* Access via one-time invite codes or exported PDF/CSV reports.

---

## Key Features

### R1. Authentication & Onboarding
* **Secure Sign-in:** Email/Password authentication.
* **Role-Based Routing:** Dedicated homes for Child, Parent, and Provider.
* **Session Security:** Auto-logout after 10 minutes of inactivity.

### R2. Privacy & Sharing
* **Granular Toggles:** Parents can toggle sharing for specific data types (e.g., Share Symptoms but hide Triggers).
* **Default Privacy:** By default, nothing is shared with Providers.
* **Revocable Access:** Invite codes expire in 7 days and access can be revoked instantly.

### R3. Medicine & Motivation
* **Technique Helper:** Interactive guide (seal lips, hold breath) with visual feedback.
* **Gamification:** Streaks for controller adherence; Badges for perfect weeks or good technique.
* **Inventory Tracking:** Automatic decrementing of dose counts.

### R4. Safety & Triage
* **PEF Zones:** Calculates Green ( $\ge$ 80%), Yellow (50-79%), or Red ( < 50%) zones based on Personal Best.
* **One-Tap Triage:** "Having trouble breathing?" feature guides users through red-flag checks and suggests action plan steps (or calling emergency).
* **Incident Log:** Records triage events and outcomes.

### R5. Data & Reporting
* **History Browser:** Filter logs by symptom, trigger, or date range (3-6 months).
* **PDF Exports:** Generate printable reports for clinical visits.

---

## 🛠 Technical Architecture
This project follows the Model-View-Presenter (MVP) architectural pattern to ensure separation of concerns and testability.

| Component | Details |
| :--- | :--- |
| **Language** | Java |
| **IDE** | Android Studio |
| **Backend** | Firebase Realtime Database (User data, Sync) |
| **Auth** | Firebase Authentication |

### Code Structure Highlights
* **Contracts (\*`Contract.java`):** Define the interface between View and Presenter.
* **Presenters (\*`Presenter.java`):** Handle business logic (e.g., `LoginPresenter` handles validation and async auth calls).
* **Views (Fragments/Activities):** Handle UI rendering and user input (e.g., `LoginFragment`, `ParentDashboardFragment`).
* **Utilities:** `SessionTimeoutManager` (Singleton handling inactivity timers), `MenuHelper` (Standardized menu handling).

---

## Application Structure

The application's navigation is segmented by user role to provide a focused experience and enforce security boundaries. 

* **1. `LoginActivity`:** This is the entry point. It handles user authentication via Firebase.
* **2. Role-Based Routing:** Upon successful login, the user is routed to a specific **Dashboard Activity** based on their role:
    * **`ChildDashboardActivity`:** For core symptom tracking and gamification features.
    * **`ParentDashboardActivity`:** For configuration, inventory, and sharing controls.
    * **`ProviderDashboardActivity`:** For read-only report viewing.
* **3. Fragments for Modularity:** Each Dashboard Activity acts as a **container** for various **Fragments**. This allows for a flexible UI where different views (e.g., Charts, Inventory, Technique Helper) can be swapped in and out without recreating the entire Activity, optimizing memory and user flow.

