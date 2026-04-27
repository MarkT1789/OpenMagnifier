# Contributing Guidelines

Thank you for your interest in contributing to this project. To ensure high code quality and maintain compatibility with our existing infrastructure, please adhere to the following Java 7 development standards.

---

## 1. Core Development Standards

### Java 7 Compatibility
All code must be compatible with **Java 7 (JDK 1.7)**. 
* **No Lambdas or Streams:** Functional interfaces must be implemented via anonymous inner classes.
* **Try-with-resources:** Use the Java 7 try-with-resources statement for automatic resource management of `AutoCloseable` types.
* **Diamond Operator:** Use the diamond operator (`List<String> list = new ArrayList<>();`) to simplify generic type instantiation.
* **Multi-catch:** Use the multi-catch block feature (`catch (IOException | SQLException e)`) where applicable.

### Naming Conventions
Follow standard Java naming conventions:
* **Classes & Interfaces:** `UpperCamelCase` (e.g., `DataProcessor`).
* **Methods & Variables:** `lowerCamelCase` (e.g., `executeTask`, `userCount`).
* **Constants:** `UPPER_SNAKE_CASE` (e.g., `DEFAULT_TIMEOUT_MS`).
* **Packages:** Lowercase, dot-separated, starting with the organization domain (e.g., `com.project.utils`).

---

## 2. Documentation and Annotations

### JavaDocs
All public classes, interfaces, and methods must include descriptive JavaDocs. 
* Use `@param` for all method parameters.
* Use `@return` for non-void methods.
* Use `@throws` or `@exception` for checked exceptions.

### Annotations
* **@Override:** Mandatory for all methods that override a superclass method or implement an interface method.
* **@Deprecated:** Use when a method is being phased out. Always provide a JavaDoc comment explaining the replacement.
* **Third-party Annotations:** Avoid non-standard annotations unless they are already present in the project's dependency tree (e.g., JUnit's `@Test`).

---

## 3. Formatting Rules

* **Indentation:** 4 spaces per level. Do not use tabs.
* **Brace Style:** Use the "Egyptian" style (end-of-line opening braces).
    ```java
    public void example() {
        if (condition) {
            // code
        }
    }
    ```
* **Imports:** Explicit imports only. Do not use wildcards (`import java.util.*` is forbidden).
* **Line Length:** Maximum 120 characters per line. Wrap lines if necessary to improve readability.

---

## 4. Testing and Quality Assurance

### Unit Tests
* No pull request will be accepted without corresponding unit tests.
* Use **JUnit 4** (matching the Java 7 environment).
* Test classes should match the name of the class they are testing, suffixed with `Test` (e.g., `CalculationServiceTest`).
* Aim for high branch coverage.

### Test Structure
Follow the **Arrange-Act-Assert** pattern:
```java
@Test
public void testAddition() {
    // Arrange
    Calculator calc = new Calculator();
    
    // Act
    int result = calc.add(2, 2);
    
    // Assert
    assertEquals(4, result);
}
```

---

## 5. Submission Process

1. **Fork** the repository to your own account.
2. **Create** a feature branch (e.g., `git checkout -b feat-new-logic`).
3. **Commit** your changes with a descriptive message.
4. **Push** to your fork and open a **Pull Request**.

---

By contributing, you agree that your contributions will be licensed under the project's existing license.
