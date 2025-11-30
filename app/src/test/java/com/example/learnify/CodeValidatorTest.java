package com.example.learnify;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for CodeValidator anti-cheating validation.
 */
public class CodeValidatorTest {

    // ========== Tests for hardcoded output detection ==========

    @Test
    public void testDetectsHardcodedPrintOutput() {
        String code = "print(\"15\")";
        String question = "Print a greeting message";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should reject hardcoded print output", result.isValid);
        assertTrue("Should be a warning", result.isWarning);
        assertNotNull("Should have error message", result.errorMessage);
    }

    @Test
    public void testDetectsHardcodedConsoleLogOutput() {
        String code = "console.log(\"42\")";
        String question = "Print your age";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should reject hardcoded console.log output", result.isValid);
    }

    @Test
    public void testAcceptsCodeWithActualLogic() {
        String code = "int sum = 0;\nfor (int i = 1; i <= 5; i++) {\n  sum += i;\n}\nprint(sum);";
        String question = "Calculate the sum of 1 to 5";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept code with actual logic", result.isValid);
    }

    // ========== Tests for loop requirement ==========

    @Test
    public void testRequiresLoopWhenQuestionMentionsLoop() {
        String code = "int result = 15;\nint x = result;\nprint(result);";
        String question = "Write a loop to calculate the sum of 1 to 5";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require loop when question mentions 'loop'", result.isValid);
        assertTrue("Error message should mention loop", result.errorMessage.toLowerCase().contains("loop"));
    }

    @Test
    public void testRequiresLoopWhenQuestionMentionsIterate() {
        String code = "int result = 120;\nint x = result;\nreturn result;";
        String question = "Iterate through numbers and calculate factorial of 5";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require loop when question mentions 'iterate'", result.isValid);
    }

    @Test
    public void testRequiresLoopWhenQuestionMentionsSum() {
        String code = "int x = 55;\nint y = x;\nreturn x;";
        String question = "Find the sum of first 10 natural numbers";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require loop when question mentions 'sum'", result.isValid);
    }

    @Test
    public void testRequiresLoopWhenQuestionMentionsFactorial() {
        String code = "int result = 24;\nint temp = result;\nreturn result;";
        String question = "Write code to find factorial of a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require loop when question mentions 'factorial'", result.isValid);
    }

    @Test
    public void testAcceptsForLoop() {
        String code = "int sum = 0;\nfor (int i = 1; i <= 5; i++) { sum += i; }\nreturn sum;";
        String question = "Write a loop to calculate the sum of 1 to 5";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept code with for loop", result.isValid);
    }

    @Test
    public void testAcceptsWhileLoop() {
        String code = "int sum = 0;\nint i = 1;\nwhile (i <= 5) { sum += i; i++; }\nreturn sum;";
        String question = "Write a loop to calculate the sum of 1 to 5";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept code with while loop", result.isValid);
    }

    // ========== Tests for function requirement ==========

    @Test
    public void testRequiresFunctionWhenQuestionMentionsFunction() {
        String code = "int x = 5;\nint y = x * x;\nint z = y;\nprint(y);";
        String question = "Write a function to calculate the square of a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require function when question mentions 'function'", result.isValid);
        assertTrue("Error message should mention function", result.errorMessage.toLowerCase().contains("function"));
    }

    @Test
    public void testRequiresFunctionWhenQuestionMentionsMethod() {
        String code = "int result = 10;\nint temp = result;\nreturn result;";
        String question = "Create a method that doubles a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require function when question mentions 'method'", result.isValid);
    }

    @Test
    public void testAcceptsPythonDef() {
        String code = "def square(n):\n    return n * n\n\nprint(square(5))";
        String question = "Define a function to calculate the square of a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept Python function definition", result.isValid);
    }

    @Test
    public void testAcceptsJavaMethod() {
        String code = "public int square(int n) {\n    return n * n;\n}";
        String question = "Write a function to calculate the square of a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept Java method definition", result.isValid);
    }

    @Test
    public void testAcceptsJavaScriptFunction() {
        String code = "function square(n) {\n    return n * n;\n}\nconsole.log(square(5));";
        String question = "Write a function to calculate the square of a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept JavaScript function definition", result.isValid);
    }

    // ========== Tests for conditional requirement ==========

    @Test
    public void testRequiresConditionalWhenQuestionMentionsCheck() {
        String code = "int num = 5;\nint result = 1;\nreturn result;";
        String question = "Check whether a number is positive";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require conditional when question mentions 'check'", result.isValid);
        assertTrue("Error message should mention conditional or if",
                result.errorMessage.toLowerCase().contains("conditional") ||
                        result.errorMessage.toLowerCase().contains("if"));
    }

    @Test
    public void testRequiresConditionalWhenQuestionMentionsCondition() {
        String code = "int num = 5;\nint x = num;\nprint(x);";
        String question = "Write code with a condition to show a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require conditional when question mentions 'condition'", result.isValid);
    }

    @Test
    public void testRequiresConditionalWhenQuestionMentionsEvenOdd() {
        String code = "int num = 4;\nint result = 1;\nreturn result;";
        String question = "Determine whether a number is even or odd";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require conditional when question mentions 'even'/'odd'", result.isValid);
    }

    @Test
    public void testAcceptsIfStatement() {
        String code = "int n = 5;\nif (n > 0) {\n    print(\"positive\");\n} else {\n    print(\"negative\");\n}";
        String question = "Check whether a number is positive or negative";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept code with if statement", result.isValid);
    }

    @Test
    public void testAcceptsTernaryOperator() {
        String code = "int n = 5;\nString result = n > 0 ? \"positive\" : \"negative\";\nprint(result);";
        String question = "Determine whether a number is positive or negative";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept code with ternary operator", result.isValid);
    }

    // ========== Tests for minimum code length ==========

    @Test
    public void testRejectsTooShortCode() {
        String code = "x = 5";
        String question = "Print something";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should reject code that is too short", result.isValid);
        assertTrue("Should be a warning", result.isWarning);
    }

    @Test
    public void testAcceptsMinimumLengthCode() {
        String code = "int x = 5;\nint y = 10;\nprint(x + y);";
        String question = "Add two numbers";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept code meeting minimum length", result.isValid);
    }

    // ========== Tests for output statements ==========

    @Test
    public void testRequiresOutputStatement() {
        String code = "int x = 5;\nint y = x * 2;\nint z = y;";
        String question = "Double a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should require output statement", result.isValid);
        assertTrue("Error message should mention output",
                result.errorMessage.toLowerCase().contains("output") ||
                        result.errorMessage.toLowerCase().contains("print") ||
                        result.errorMessage.toLowerCase().contains("return"));
    }

    @Test
    public void testAcceptsPrint() {
        String code = "int x = 5;\nint y = x * 2;\nprint(y);";
        String question = "Double a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept code with print", result.isValid);
    }

    @Test
    public void testAcceptsReturn() {
        String code = "int x = 5;\nint y = x * 2;\nreturn y;";
        String question = "Double a number";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept code with return", result.isValid);
    }

    // ========== Tests for empty/null code ==========

    @Test
    public void testRejectsEmptyCode() {
        String code = "";
        String question = "Write something";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should reject empty code", result.isValid);
    }

    @Test
    public void testRejectsNullCode() {
        String code = null;
        String question = "Write something";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should reject null code", result.isValid);
    }

    @Test
    public void testRejectsWhitespaceOnlyCode() {
        String code = "   \n\t\n   ";
        String question = "Write something";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should reject whitespace-only code", result.isValid);
    }

    // ========== Tests for code complexity ==========

    @Test
    public void testRejectsTooSimpleCode() {
        String code = "print(\"hello\")";
        String question = "Write a simple program";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertFalse("Should reject overly simple code", result.isValid);
    }

    @Test
    public void testAcceptsComplexCode() {
        String code = "int sum = 0;\nfor (int i = 0; i < 10; i++) {\n    if (i % 2 == 0) {\n        sum += i;\n    }\n}\nprint(sum);";
        String question = "Print the result";

        CodeValidator.ValidationResult result = CodeValidator.validateCode(code, question);

        assertTrue("Should accept complex code", result.isValid);
    }

    // ========== Tests for ValidationResult ==========

    @Test
    public void testValidationResultSuccess() {
        CodeValidator.ValidationResult result = CodeValidator.ValidationResult.success();

        assertTrue("Success result should be valid", result.isValid);
        assertNull("Success result should have no error message", result.errorMessage);
        assertFalse("Success result should not be warning", result.isWarning);
    }

    @Test
    public void testValidationResultError() {
        CodeValidator.ValidationResult result = CodeValidator.ValidationResult.error("Test error");

        assertFalse("Error result should not be valid", result.isValid);
        assertEquals("Error message should match", "Test error", result.errorMessage);
        assertFalse("Error result should not be warning", result.isWarning);
    }

    @Test
    public void testValidationResultWarning() {
        CodeValidator.ValidationResult result = CodeValidator.ValidationResult.warning("Test warning");

        assertFalse("Warning result should not be valid", result.isValid);
        assertEquals("Warning message should match", "Test warning", result.errorMessage);
        assertTrue("Warning result should be warning", result.isWarning);
    }
}
