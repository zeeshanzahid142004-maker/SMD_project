package com.example.learnify;

import java.util.regex.Pattern;

/**
 * Anti-cheating code validator that checks for:
 * - Hardcoded outputs
 * - Required loops for iterative problems
 * - Required functions for function-based problems
 * - Required conditionals for decision-based problems
 * - Minimum code complexity
 * - Output statements
 */
public class CodeValidator {

    private static final int MIN_CODE_LENGTH = 15;

    /**
     * Result class containing validation status and error message
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Validates the submitted code against the question requirements
     *
     * @param code     The submitted code
     * @param question The question text to determine requirements
     * @return ValidationResult with status and error message if invalid
     */
    public static ValidationResult validateCode(String code, String question) {
        if (code == null || code.trim().isEmpty()) {
            return ValidationResult.failure("Please write some code first!");
        }

        String trimmedCode = code.trim();
        String lowerCode = trimmedCode.toLowerCase();
        String lowerQuestion = question != null ? question.toLowerCase() : "";

        // Check for hardcoded outputs
        ValidationResult hardcodedCheck = checkForHardcodedOutput(trimmedCode, lowerCode);
        if (!hardcodedCheck.isValid) {
            return hardcodedCheck;
        }

        // Check minimum code complexity
        ValidationResult complexityCheck = checkCodeComplexity(trimmedCode, lowerCode);
        if (!complexityCheck.isValid) {
            return complexityCheck;
        }

        // Check for required loop constructs
        ValidationResult loopCheck = checkLoopRequirement(lowerCode, lowerQuestion);
        if (!loopCheck.isValid) {
            return loopCheck;
        }

        // Check for required function/method definitions
        ValidationResult functionCheck = checkFunctionRequirement(lowerCode, lowerQuestion);
        if (!functionCheck.isValid) {
            return functionCheck;
        }

        // Check for required conditionals
        ValidationResult conditionalCheck = checkConditionalRequirement(lowerCode, lowerQuestion);
        if (!conditionalCheck.isValid) {
            return conditionalCheck;
        }

        // Check for output statements
        ValidationResult outputCheck = checkOutputStatements(lowerCode);
        if (!outputCheck.isValid) {
            return outputCheck;
        }

        return ValidationResult.success();
    }

    /**
     * Detects hardcoded outputs like print("15") without any logic
     */
    private static ValidationResult checkForHardcodedOutput(String code, String lowerCode) {
        // Pattern to detect simple print statements with just a number or string literal
        // Matches: print("15"), print('15'), System.out.println("15"), console.log("15"), etc.
        Pattern hardcodedPrintPattern = Pattern.compile(
                "(print|println|console\\.log|system\\.out\\.print|echo|puts|write)\\s*\\(\\s*[\"']?\\d+[\"']?\\s*\\)",
                Pattern.CASE_INSENSITIVE
        );

        // Check if code is just a simple print statement with a literal value
        boolean hasOnlyPrintStatements = hardcodedPrintPattern.matcher(lowerCode).find();
        boolean hasLogic = containsLogicConstructs(lowerCode);

        if (hasOnlyPrintStatements && !hasLogic) {
            // Additional check: count meaningful lines (excluding empty lines and comments)
            String[] lines = code.split("\n");
            int meaningfulLines = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() &&
                        !trimmed.startsWith("//") &&
                        !trimmed.startsWith("#") &&
                        !trimmed.startsWith("/*") &&
                        !trimmed.startsWith("*")) {
                    meaningfulLines++;
                }
            }

            if (meaningfulLines <= 2) {
                return ValidationResult.failure("Hardcoded answer detected. Please write actual logic to solve the problem.");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Checks if code contains any logic constructs
     */
    private static boolean containsLogicConstructs(String lowerCode) {
        return lowerCode.contains("for") ||
                lowerCode.contains("while") ||
                lowerCode.contains("if") ||
                lowerCode.contains("else") ||
                lowerCode.contains("switch") ||
                lowerCode.contains("case") ||
                lowerCode.contains("def ") ||
                lowerCode.contains("function") ||
                lowerCode.contains("void ") ||
                lowerCode.contains("int ") ||
                lowerCode.contains("return") ||
                lowerCode.contains("class") ||
                lowerCode.contains("=") ||
                lowerCode.contains("+") ||
                lowerCode.contains("-") ||
                lowerCode.contains("*") ||
                lowerCode.contains("/");
    }

    /**
     * Checks minimum code complexity
     */
    private static ValidationResult checkCodeComplexity(String code, String lowerCode) {
        if (code.length() < MIN_CODE_LENGTH) {
            return ValidationResult.failure("Solution too simple. Please provide a complete solution.");
        }

        // Count meaningful constructs
        int constructCount = 0;

        if (lowerCode.contains("for") || lowerCode.contains("while")) constructCount++;
        if (lowerCode.contains("if") || lowerCode.contains("else")) constructCount++;
        if (lowerCode.contains("def ") || lowerCode.contains("function") ||
                lowerCode.contains("void ") || lowerCode.contains("public ")) constructCount++;
        if (lowerCode.contains("return")) constructCount++;
        if (lowerCode.contains("=")) constructCount++;

        // Check for variable assignments or arithmetic operations
        boolean hasVariables = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*\\s*=").matcher(code).find();
        if (hasVariables) constructCount++;

        return ValidationResult.success();
    }

    /**
     * Checks if loop is required based on question keywords
     */
    private static ValidationResult checkLoopRequirement(String lowerCode, String lowerQuestion) {
        // Check for explicit loop requirements
        boolean requiresLoop = lowerQuestion.contains("loop") ||
                lowerQuestion.contains("iterate") ||
                lowerQuestion.contains("factorial") ||
                lowerQuestion.contains("repeat") ||
                lowerQuestion.contains("traverse") ||
                lowerQuestion.contains("1 to n") ||
                lowerQuestion.contains("from 1 to") ||
                lowerQuestion.contains("from 0 to") ||
                lowerQuestion.contains("all elements") ||
                lowerQuestion.contains("each element") ||
                lowerQuestion.contains("every element") ||
                // Sum with iterative context
                (lowerQuestion.contains("sum") && (
                        lowerQuestion.contains("numbers") ||
                        lowerQuestion.contains("array") ||
                        lowerQuestion.contains("list") ||
                        lowerQuestion.contains("1 to") ||
                        lowerQuestion.contains("using a loop") ||
                        lowerQuestion.contains("using loop")
                ));

        if (requiresLoop) {
            boolean hasLoop = lowerCode.contains("for") ||
                    lowerCode.contains("while") ||
                    lowerCode.contains("foreach") ||
                    lowerCode.contains("do {") ||
                    lowerCode.contains("do{") ||
                    lowerCode.contains(".map(") ||
                    lowerCode.contains(".foreach(") ||
                    lowerCode.contains(".reduce(") ||
                    lowerCode.contains(".filter(");

            if (!hasLoop) {
                return ValidationResult.failure("This problem requires a loop. Use for, while, or similar constructs.");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Checks if function/method is required based on question keywords
     */
    private static ValidationResult checkFunctionRequirement(String lowerCode, String lowerQuestion) {
        boolean requiresFunction = lowerQuestion.contains("function") ||
                lowerQuestion.contains("method") ||
                lowerQuestion.contains("define") ||
                lowerQuestion.contains("create a function") ||
                lowerQuestion.contains("write a function") ||
                lowerQuestion.contains("implement a method");

        if (requiresFunction) {
            boolean hasFunction = lowerCode.contains("def ") ||
                    lowerCode.contains("function ") ||
                    lowerCode.contains("function(") ||
                    Pattern.compile("(public|private|protected)?\\s*(static)?\\s*(void|int|string|boolean|float|double|long|char)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(").matcher(lowerCode).find() ||
                    Pattern.compile("(const|let|var)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*=\\s*(\\([^)]*\\)|[a-zA-Z_][a-zA-Z0-9_]*)\\s*=>").matcher(lowerCode).find() ||
                    lowerCode.contains("func ") ||
                    lowerCode.contains("fn ");

            if (!hasFunction) {
                return ValidationResult.failure("This problem requires a function/method definition.");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Checks if conditionals are required based on question keywords
     */
    private static ValidationResult checkConditionalRequirement(String lowerCode, String lowerQuestion) {
        boolean requiresConditional = lowerQuestion.contains("if ") ||
                lowerQuestion.contains("check") ||
                lowerQuestion.contains("condition") ||
                lowerQuestion.contains("whether") ||
                lowerQuestion.contains("determine") ||
                lowerQuestion.contains("compare") ||
                lowerQuestion.contains("greater") ||
                lowerQuestion.contains("less") ||
                lowerQuestion.contains("equal") ||
                lowerQuestion.contains("odd") ||
                lowerQuestion.contains("even") ||
                lowerQuestion.contains("positive") ||
                lowerQuestion.contains("negative");

        if (requiresConditional) {
            boolean hasConditional = lowerCode.contains("if") ||
                    lowerCode.contains("else") ||
                    lowerCode.contains("switch") ||
                    lowerCode.contains("case") ||
                    lowerCode.contains("?") ||  // ternary operator
                    lowerCode.contains("&&") ||
                    lowerCode.contains("||");

            if (!hasConditional) {
                return ValidationResult.failure("This problem requires conditional statements (if/else, switch, etc.).");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Checks if code has output statements
     */
    private static ValidationResult checkOutputStatements(String lowerCode) {
        boolean hasOutput = lowerCode.contains("print") ||
                lowerCode.contains("console.log") ||
                lowerCode.contains("system.out") ||
                lowerCode.contains("echo") ||
                lowerCode.contains("puts") ||
                lowerCode.contains("write") ||
                lowerCode.contains("cout") ||
                lowerCode.contains("printf") ||
                lowerCode.contains("return");  // return can be considered as output in functions

        if (!hasOutput) {
            return ValidationResult.failure("Your code should have output statements (print, console.log, etc.).");
        }

        return ValidationResult.success();
    }
}
