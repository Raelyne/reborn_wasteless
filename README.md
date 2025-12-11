# Testing Guide for WasteLess App

This document provides an overview of the unit tests and instrumentation tests created for the WasteLess app.

## Test Structure

### Unit Tests (`app/src/test/`)

Unit tests run on the JVM and don't require an Android device/emulator. They test business logic, utilities, and ViewModels with mocked dependencies.

#### Test Files Created:

1. **ValidationUtilsTest.kt**
   - Tests `isValidEmail()` function with various valid and invalid email formats
   - Tests `isValidPassword()` function with various password requirements (uppercase, lowercase, digits, special chars, length)

2. **DataAnalyticsUtilTest.kt**
   - Tests `aggregateByWeek()` function for grouping logs by week
   - Tests `getTopWastedItems()` function for finding top 5 wasted items
   - Tests edge cases like empty lists and sorting

3. **LoginViewModelTest.kt**
   - Tests login validation (empty email, invalid email, empty password)
   - Tests successful login flow
   - Tests error handling for various Firebase exceptions
   - Tests state management (Idle, Loading, Success, Error)

4. **SignUpViewModelTest.kt**
   - Tests registration validation (username, email, password requirements)
   - Tests password matching validation
   - Tests username availability checking
   - Tests error handling for Firebase exceptions
   - Tests successful registration flow

5. **HomeViewModelTest.kt**
   - Tests username extraction from email
   - Tests greeting generation

6. **UserRepositoryTest.kt**
   - Tests `extractUsernameFromEmail()` utility function

7. **MappersTest.kt**
   - Tests `toSummary()` mapper function
   - Tests waste type mapping (single, multiple, mixed)
   - Tests weight formatting
   - Tests date formatting

### Instrumentation Tests (`app/src/androidTest/`)

Instrumentation tests run on Android devices/emulators and test UI components and integration.

#### Test Files Created:

1. **LoginFragmentTest.kt**
   - Tests that login fragment displays correctly
   - Tests user input in email and password fields
   - Tests button interactions

2. **SignUpFragmentTest.kt**
   - Tests that sign up fragment displays all required fields
   - Tests user input in registration form
   - Tests button interactions

3. **MainActivityTest.kt**
   - Tests MainActivity launches successfully
   - Tests navigation setup

4. **ValidationUtilsInstrumentedTest.kt**
   - Runs validation utility tests on Android environment
   - Tests edge cases that might behave differently on Android

5. **AuthFlowIntegrationTest.kt**
   - Integration tests for authentication flow
   - Tests navigation between screens
   - Tests form interactions

## Running Tests

### Run All Unit Tests
```bash
./gradlew test
```

### Run All Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Run Specific Test Class
```bash
# Unit test
./gradlew test --tests "com.reborn.wasteless.utils.ValidationUtilsTest"

# Instrumentation test
./gradlew connectedAndroidTest --tests "com.reborn.wasteless.ui.login.LoginFragmentTest"
```

### Run Tests from Android Studio
1. Right-click on a test file or test method
2. Select "Run 'TestName'"
3. View results in the Run window

## Test Dependencies Added

The following test dependencies were added to `app/build.gradle.kts`:

- **Mockito Core & Inline**: For mocking dependencies in unit tests
- **Kotlinx Coroutines Test**: For testing coroutines
- **Arch Core Testing**: For testing LiveData and ViewModels
- **Espresso Core & Contrib**: For UI testing
- **Fragment Testing**: For testing fragments in isolation
- **Navigation Testing**: For testing navigation components

## Test Coverage

### Covered Components:
- ✅ Validation utilities (email, password)
- ✅ Data analytics utilities
- ✅ ViewModels (Login, SignUp, Home)
- ✅ Repository utility functions
- ✅ Mappers
- ✅ UI Fragments (Login, SignUp)
- ✅ MainActivity

### Areas for Future Testing:
- More ViewModels (DiaryViewModel, LoggingViewModel, etc.)
- More Repository methods (with Firebase mocking)
- Adapter classes
- More UI fragments
- End-to-end user flows

## Notes

1. **Firebase Testing**: Some repository tests require Firebase mocking, which can be complex. The current tests focus on testable utility functions. For full Firebase testing, consider using Firebase Test Lab or mocking Firebase services.

2. **Async Operations**: Some tests use `Thread.sleep()` to wait for async operations. In production, consider using more robust waiting mechanisms or coroutines test utilities.

3. **Resource IDs**: Make sure resource IDs in instrumentation tests match your actual layout files. The tests have been updated to use snake_case IDs (e.g., `text_email` instead of `textEmail`).

4. **Test Data**: Tests use mock data. For integration tests, you may want to set up test Firebase projects or use Firebase emulators.


## Troubleshooting

### Tests fail with "No tests found"
- Ensure test classes are in the correct package structure
- Check that test methods are annotated with `@Test`
- Verify test class names end with "Test"

### Instrumentation tests fail
- Ensure an emulator or device is connected
- Check that the app is properly installed on the device
- Verify resource IDs match your layout files

### Mockito errors
- Ensure you're using `mockito-inline` for final class mocking
- Check that you're using `MockitoAnnotations.openMocks(this)` in `@Before` methods
