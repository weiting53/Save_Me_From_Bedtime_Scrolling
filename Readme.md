# Save Me From Bedtime Scrolling

## Project Overview

Save Me From Bedtime Scrolling is an iOS application designed to help
users reduce excessive phone usage before sleep.

Instead of enforcing strict restrictions, the app provides gentle
interventions at critical moments to encourage users to stop scrolling
and transition into rest.

------------------------------------------------------------------------

## Motivation

Many users struggle with bedtime scrolling, often losing track of time
despite intending to sleep early.

This project aims to:

-   Introduce soft behavioral nudges rather than hard blocking\
-   Create a low-friction user experience that respects user autonomy\
-   Explore how design and timing can influence habit formation

------------------------------------------------------------------------

## Core Features

-   Onboarding Flow\
    Guides users to set a target sleep time and introduces the app's
    philosophy

-   Bedtime Reminder System\
    Sends notifications before the target sleep time with configurable
    lead time

-   Intervention Methods\
    Example: breathing guidance (e.g., 4-7-8 technique)

-   State Persistence\
    Stores user preferences and onboarding state

------------------------------------------------------------------------

## Technical Architecture

### iOS (Swift / SwiftUI)

-   MVVM pattern\
-   Key components:
    -   OnboardingViewModel\
    -   SettingsViewModel\
    -   AppSettings\
    -   NotificationManager

------------------------------------------------------------------------

### Python Tooling

-   uv for dependency management\
-   just for command orchestration\
-   ruff for linting and formatting\
-   pre-commit for code quality

------------------------------------------------------------------------

## Project Structure

Save_Me_From_Bedtime_Scrolling/ ├── ios/ │ └── WindDownSofa/ ├──
scripts/ ├── pyproject.toml ├── justfile └── README.md

------------------------------------------------------------------------

## Setup

``` bash
just setup
```

------------------------------------------------------------------------

## Lint

``` bash
just lint
```

------------------------------------------------------------------------

## Run

``` bash
just run
```

------------------------------------------------------------------------

## Design Philosophy

This project focuses on:

-   Awareness over enforcement\
-   Timing over restriction\
-   User agency over system authority

------------------------------------------------------------------------

## Future Work

-   Adaptive intervention timing\
-   Screen Time API exploration\
-   More intervention methods\
-   Habit analytics
