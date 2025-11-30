package com.example.learnify;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for CodeValidator anti-cheating functionality
 */
public class CodeValidatorTest {

    // ============ Hardcoded Output Detection Tests ============

    @Test
    public void testHardcodedOutputDetected() {
        // Simple hardcoded print statement should be rejected
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "print(\"15\")",
                "Calculate the sum of 5 and 10"
        );
        assertFalse("Hardcoded output should be detected", result.isValid);
        assertTrue("Error message should mention hardcoded",
                result.errorMessage.toLowerCase().contains("hardcoded"));
    }

    @Test
    public void testHardcodedOutputWithSystemOutPrintln() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "System.out.println(15)",
                "Calculate the sum"
        );
        assertFalse("Hardcoded System.out.println should be detected", result.isValid);
    }

    @Test
    public void testValidCodeWithLogic() {
        // Valid code with actual logic should pass
        String validCode = "int a = 5;\nint b = 10;\nint sum = a + b;\nprint(sum);";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                validCode,
                "Calculate the sum of two numbers"
        );
        assertTrue("Valid code with logic should pass", result.isValid);
    }

    // ============ Loop Requirement Tests ============

    @Test
    public void testLoopRequiredForSumQuestion() {
        // Code without loop for a sum question that requires iteration
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "int result = 15;\nprint(result);",
                "Write code to calculate the sum of numbers from 1 to 5 using a loop"
        );
        assertFalse("Loop should be required for sum question with iteration context", result.isValid);
        assertTrue("Error message should mention loop",
                result.errorMessage.toLowerCase().contains("loop"));
    }

    @Test
    public void testSimpleSumDoesNotRequireLoop() {
        // Simple sum question without iteration context shouldn't require loop
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "int a = 5;\nint b = 10;\nint sum = a + b;\nprint(sum);",
                "Calculate the sum of two numbers"
        );
        assertTrue("Simple sum should not require a loop", result.isValid);
    }

    @Test
    public void testLoopRequiredForFactorial() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "int result = 120;\nprint(result);",
                "Calculate the factorial of 5"
        );
        assertFalse("Loop should be required for factorial", result.isValid);
    }

    @Test
    public void testLoopProvidedForFactorial() {
        String codeWithLoop = "int n = 5;\nint result = 1;\nfor(int i=1; i<=n; i++) { result *= i; }\nprint(result);";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                codeWithLoop,
                "Calculate the factorial of 5"
        );
        assertTrue("Code with loop should pass for factorial", result.isValid);
    }

    @Test
    public void testWhileLoopAccepted() {
        String codeWithWhile = "int i = 0;\nint sum = 0;\nwhile(i < 10) { sum += i; i++; }\nprint(sum);";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                codeWithWhile,
                "Calculate sum using loop"
        );
        assertTrue("While loop should be accepted", result.isValid);
    }

    // ============ Function Requirement Tests ============

    @Test
    public void testFunctionRequiredWhenMentioned() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "int x = 5;\nint y = x * 2;\nprint(y);",
                "Write a function to double a number"
        );
        assertFalse("Function should be required when mentioned", result.isValid);
        assertTrue("Error message should mention function",
                result.errorMessage.toLowerCase().contains("function"));
    }

    @Test
    public void testFunctionProvidedPython() {
        String codeWithDef = "def double(x):\n    return x * 2\n\nresult = double(5)\nprint(result)";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                codeWithDef,
                "Write a function to double a number"
        );
        assertTrue("Python function with def should pass", result.isValid);
    }

    @Test
    public void testFunctionProvidedJavaScript() {
        String codeWithFunction = "function double(x) {\n    return x * 2;\n}\nconsole.log(double(5));";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                codeWithFunction,
                "Write a function to double a number"
        );
        assertTrue("JavaScript function should pass", result.isValid);
    }

    @Test
    public void testMethodRequiredWhenMentioned() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "int x = 10;\nprint(x);",
                "Define a method to calculate area"
        );
        assertFalse("Method should be required when mentioned", result.isValid);
    }

    // ============ Conditional Requirement Tests ============

    @Test
    public void testConditionalRequiredForCheckQuestion() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "int num = 5;\nprint(\"odd\");",
                "Check if a number is odd or even"
        );
        assertFalse("Conditional should be required for check question", result.isValid);
        assertTrue("Error message should mention conditional",
                result.errorMessage.toLowerCase().contains("conditional"));
    }

    @Test
    public void testConditionalProvidedWithIf() {
        String codeWithIf = "int num = 5;\nif(num % 2 == 0) {\n    print(\"even\");\n} else {\n    print(\"odd\");\n}";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                codeWithIf,
                "Check if a number is odd or even"
        );
        assertTrue("Code with if-else should pass", result.isValid);
    }

    @Test
    public void testConditionalRequiredForCompare() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "print(5);",
                "Compare two numbers and print the greater one"
        );
        assertFalse("Conditional should be required for compare question", result.isValid);
    }

    // ============ Minimum Complexity Tests ============

    @Test
    public void testCodeTooShort() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "x=5",
                "Calculate something"
        );
        assertFalse("Very short code should be rejected", result.isValid);
        assertTrue("Error message should mention simple",
                result.errorMessage.toLowerCase().contains("simple"));
    }

    @Test
    public void testEmptyCode() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "",
                "Write some code"
        );
        assertFalse("Empty code should be rejected", result.isValid);
    }

    @Test
    public void testNullCode() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                null,
                "Write some code"
        );
        assertFalse("Null code should be rejected", result.isValid);
    }

    // ============ Output Statement Tests ============

    @Test
    public void testMissingOutputStatement() {
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                "int x = 5;\nint y = 10;\nint sum = x + y;",
                "Calculate the sum"
        );
        assertFalse("Code without output should be rejected", result.isValid);
        assertTrue("Error message should mention output",
                result.errorMessage.toLowerCase().contains("output"));
    }

    @Test
    public void testReturnAsOutput() {
        // Return statement should count as output
        String codeWithReturn = "int calculate(int a, int b) {\n    return a + b;\n}";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                codeWithReturn,
                "Create a function to add two numbers"
        );
        assertTrue("Return statement should count as output", result.isValid);
    }

    @Test
    public void testConsoleLogAsOutput() {
        String codeWithConsoleLog = "let x = 5;\nlet y = 10;\nconsole.log(x + y);";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                codeWithConsoleLog,
                "Add two numbers"
        );
        assertTrue("console.log should be valid output", result.isValid);
    }

    // ============ Edge Cases ============

    @Test
    public void testNullQuestion() {
        // Should handle null question gracefully
        String validCode = "int x = 5;\nprint(x);";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                validCode,
                null
        );
        assertTrue("Should handle null question", result.isValid);
    }

    @Test
    public void testEmptyQuestion() {
        String validCode = "int x = 5;\nprint(x);";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                validCode,
                ""
        );
        assertTrue("Should handle empty question", result.isValid);
    }

    @Test
    public void testComplexValidCode() {
        String complexCode = "def factorial(n):\n" +
                "    if n <= 1:\n" +
                "        return 1\n" +
                "    result = 1\n" +
                "    for i in range(2, n+1):\n" +
                "        result *= i\n" +
                "    return result\n" +
                "\n" +
                "print(factorial(5))";
        CodeValidator.ValidationResult result = CodeValidator.validateCode(
                complexCode,
                "Write a function to calculate factorial using a loop"
        );
        assertTrue("Complex valid code should pass", result.isValid);
    }
}
