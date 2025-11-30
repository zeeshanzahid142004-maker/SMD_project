package com.example.learnify;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates code submissions to detect cheating attempts and ensure
 * code meets minimum requirements based on the question context.
 */
public class CodeValidator {

    // Minimum code length threshold
    private static final int MIN_CODE_LENGTH = 15;
    private static final int MIN_CODE_LENGTH_FOR_COMPLEX_PROBLEMS = 30;

    // Keywords that indicate loop requirement
    private static final String[] LOOP_KEYWORDS = {"loop", "iterate", "sum", "factorial", "repeat", "times", "each", "all", "every"};

    // Keywords that indicate function requirement
    private static final String[] FUNCTION_KEYWORDS = {"function", "method", "define", "create a function", "write a function", "implement"};

    // Keywords that indicate conditional requirement
    private static final String[] CONDITIONAL_KEYWORDS = {"if", "check", "condition", "whether", "determine", "even", "odd", "positive", "negative", "greater", "less", "equal"};

    /**
     * Result of code validation containing success status and error message.
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        public final boolean isWarning;

        private ValidationResult(boolean isValid, String errorMessage, boolean isWarning) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.isWarning = isWarning;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, false);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, false);
        }

        public static ValidationResult warning(String message) {
            return new ValidationResult(false, message, true);
        }
    }

    /**
     * Validates the submitted code against the question requirements.
     *
     * @param code         The submitted code
     * @param questionText The question text to determine required constructs
     * @return ValidationResult containing success status and error message if any
     */
    public static ValidationResult validateCode(String code, String questionText) {
        if (code == null || code.trim().isEmpty()) {
            return ValidationResult.error("Please write some code first!");
        }

        String trimmedCode = code.trim();
        String lowerCode = trimmedCode.toLowerCase();
        String lowerQuestion = questionText != null ? questionText.toLowerCase() : "";

        // Check for minimum code length
        ValidationResult lengthResult = checkMinimumLength(trimmedCode, lowerQuestion);
        if (!lengthResult.isValid) {
            return lengthResult;
        }

        // Check for hardcoded outputs
        ValidationResult hardcodedResult = checkHardcodedOutputs(trimmedCode, lowerCode);
        if (!hardcodedResult.isValid) {
            return hardcodedResult;
        }

        // Check for required loops
        ValidationResult loopResult = checkRequiredLoops(lowerCode, lowerQuestion);
        if (!loopResult.isValid) {
            return loopResult;
        }

        // Check for required functions
        ValidationResult functionResult = checkRequiredFunctions(lowerCode, lowerQuestion);
        if (!functionResult.isValid) {
            return functionResult;
        }

        // Check for required conditionals
        ValidationResult conditionalResult = checkRequiredConditionals(lowerCode, lowerQuestion);
        if (!conditionalResult.isValid) {
            return conditionalResult;
        }

        // Check for output statements
        ValidationResult outputResult = checkOutputStatements(lowerCode);
        if (!outputResult.isValid) {
            return outputResult;
        }

        // Check minimum code complexity
        ValidationResult complexityResult = checkCodeComplexity(trimmedCode, lowerCode);
        if (!complexityResult.isValid) {
            return complexityResult;
        }

        return ValidationResult.success();
    }

    /**
     * Checks if code meets minimum length requirements.
     */
    private static ValidationResult checkMinimumLength(String code, String lowerQuestion) {
        int minLength = MIN_CODE_LENGTH;

        // Complex problems require more code
        if (containsAnyKeyword(lowerQuestion, LOOP_KEYWORDS) ||
                containsAnyKeyword(lowerQuestion, FUNCTION_KEYWORDS)) {
            minLength = MIN_CODE_LENGTH_FOR_COMPLEX_PROBLEMS;
        }

        if (code.length() < minLength) {
            return ValidationResult.warning("Your code seems too short for this problem. Please write a more complete solution.");
        }

        return ValidationResult.success();
    }

    /**
     * Detects hardcoded outputs without real logic.
     * Catches patterns like print("15") or System.out.println("answer")
     * without any actual computation.
     */
    private static ValidationResult checkHardcodedOutputs(String code, String lowerCode) {
        // Pattern to match print statements with hardcoded values
        Pattern printPattern = Pattern.compile(
                "(print\\s*\\(|println\\s*\\(|console\\.log\\s*\\(|echo\\s+|puts\\s+)" +
                        "\\s*[\"']\\s*\\d+\\s*[\"']\\s*\\)?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher printMatcher = printPattern.matcher(code);
        if (printMatcher.find()) {
            // Check if there's any actual logic in the code
            if (!hasActualLogic(lowerCode)) {
                return ValidationResult.warning("Your code appears to have hardcoded output without actual logic. Please implement the solution properly.");
            }
        }

        // Check for simple return with hardcoded value and no logic
        Pattern returnPattern = Pattern.compile(
                "return\\s+[\"']?\\d+[\"']?\\s*;?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher returnMatcher = returnPattern.matcher(code);
        if (returnMatcher.find()) {
            if (!hasActualLogic(lowerCode)) {
                return ValidationResult.warning("Your code appears to return a hardcoded value without computation. Please implement the actual logic.");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Checks if code has actual logic (variables, operations, loops, conditionals).
     */
    private static boolean hasActualLogic(String lowerCode) {
        // Check for variable assignments
        boolean hasAssignment = lowerCode.contains("=") && !lowerCode.contains("==");

        // Check for arithmetic operations
        boolean hasArithmetic = lowerCode.contains("+") || lowerCode.contains("-") ||
                lowerCode.contains("*") || lowerCode.contains("/") ||
                lowerCode.contains("%");

        // Check for loops
        boolean hasLoop = lowerCode.contains("for") || lowerCode.contains("while");

        // Check for conditionals
        boolean hasConditional = lowerCode.contains("if");

        // Check for function calls (excluding print statements)
        boolean hasFunctionCalls = countFunctionCalls(lowerCode) > 1;

        return (hasAssignment && hasArithmetic) || hasLoop || hasConditional || hasFunctionCalls;
    }

    /**
     * Counts function calls in the code.
     */
    private static int countFunctionCalls(String lowerCode) {
        Pattern functionCallPattern = Pattern.compile("\\w+\\s*\\(");
        Matcher matcher = functionCallPattern.matcher(lowerCode);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Checks if loops are required based on question keywords.
     */
    private static ValidationResult checkRequiredLoops(String lowerCode, String lowerQuestion) {
        if (containsAnyKeyword(lowerQuestion, LOOP_KEYWORDS)) {
            boolean hasLoop = lowerCode.contains("for") ||
                    lowerCode.contains("while") ||
                    lowerCode.contains("foreach") ||
                    lowerCode.contains(".map(") ||
                    lowerCode.contains(".foreach(") ||
                    lowerCode.contains(".reduce(") ||
                    lowerCode.contains("range(");

            if (!hasLoop) {
                return ValidationResult.warning("This problem requires a loop (for/while). Your code doesn't seem to contain any iteration.");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Checks if functions are required based on question keywords.
     */
    private static ValidationResult checkRequiredFunctions(String lowerCode, String lowerQuestion) {
        if (containsAnyKeyword(lowerQuestion, FUNCTION_KEYWORDS)) {
            // Check for actual function/method definitions, not just type keywords
            boolean hasFunction = lowerCode.contains("function ") ||
                    lowerCode.contains("function(") ||
                    lowerCode.contains("def ") ||
                    (lowerCode.contains("void ") && lowerCode.contains("(")) ||
                    (lowerCode.contains("public ") && lowerCode.contains("(")) ||
                    (lowerCode.contains("private ") && lowerCode.contains("(")) ||
                    (lowerCode.contains("static ") && lowerCode.contains("(")) ||
                    lowerCode.contains("=>") ||
                    lowerCode.contains("func ") ||
                    lowerCode.contains("fn ");

            // Additional check: look for function definition patterns
            if (!hasFunction) {
                // Check for Java/C++ style: return_type method_name(
                boolean hasMethodPattern = lowerCode.matches("(?s).*\\b(int|void|string|boolean|double|float)\\s+\\w+\\s*\\(.*");
                hasFunction = hasMethodPattern;
            }

            if (!hasFunction) {
                return ValidationResult.warning("This problem asks you to create a function/method. Please define a function in your solution.");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Checks if conditionals are required based on question keywords.
     */
    private static ValidationResult checkRequiredConditionals(String lowerCode, String lowerQuestion) {
        if (containsAnyKeyword(lowerQuestion, CONDITIONAL_KEYWORDS)) {
            boolean hasConditional = lowerCode.contains("if") ||
                    lowerCode.contains("else") ||
                    lowerCode.contains("switch") ||
                    lowerCode.contains("case") ||
                    lowerCode.contains("?") ||
                    lowerCode.contains("&&") ||
                    lowerCode.contains("||");

            if (!hasConditional) {
                return ValidationResult.warning("This problem requires conditional logic (if/else). Your code doesn't contain any conditions.");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Checks if code has output statements.
     */
    private static ValidationResult checkOutputStatements(String lowerCode) {
        boolean hasOutput = lowerCode.contains("print") ||
                lowerCode.contains("println") ||
                lowerCode.contains("console.log") ||
                lowerCode.contains("echo") ||
                lowerCode.contains("puts") ||
                lowerCode.contains("write") ||
                lowerCode.contains("return");

        if (!hasOutput) {
            return ValidationResult.warning("Your code doesn't produce any output. Make sure to print or return the result.");
        }

        return ValidationResult.success();
    }

    /**
     * Checks overall code complexity.
     */
    private static ValidationResult checkCodeComplexity(String code, String lowerCode) {
        // Count meaningful constructs
        int complexityScore = 0;

        // Variable declarations/assignments
        if (lowerCode.contains("=")) complexityScore++;

        // Loops
        if (lowerCode.contains("for") || lowerCode.contains("while")) complexityScore += 2;

        // Conditionals
        if (lowerCode.contains("if")) complexityScore++;

        // Functions
        if (lowerCode.contains("function") || lowerCode.contains("def ") ||
                lowerCode.contains("void ") || lowerCode.contains("public ")) complexityScore += 2;

        // Arithmetic operations
        if (lowerCode.contains("+") || lowerCode.contains("-") ||
                lowerCode.contains("*") || lowerCode.contains("/")) complexityScore++;

        // Count lines of actual code (excluding empty lines and comments)
        int codeLines = countCodeLines(code);

        if (complexityScore < 2 && codeLines < 3) {
            return ValidationResult.warning("Your code seems too simple. Please implement a more complete solution.");
        }

        return ValidationResult.success();
    }

    /**
     * Counts lines of actual code (excluding empty lines and comments).
     */
    private static int countCodeLines(String code) {
        String[] lines = code.split("\n");
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() &&
                    !trimmed.startsWith("//") &&
                    !trimmed.startsWith("#") &&
                    !trimmed.startsWith("/*") &&
                    !trimmed.startsWith("*")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if text contains any of the given keywords.
     */
    private static boolean containsAnyKeyword(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
