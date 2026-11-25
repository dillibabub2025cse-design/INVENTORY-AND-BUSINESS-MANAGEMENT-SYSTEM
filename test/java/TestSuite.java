package test;

/**
 * Main Test Suite runner for unit tests and live database integration tests.
 */
public class TestSuite {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("     INVENTORY & BUSINESS MANAGEMENT SYSTEM - COMPREHENSIVE TEST SUITE    ");
        System.out.println("==========================================================================");

        int passed = 0;
        int failed = 0;

        try {
            PasswordUtilTest.runTests();
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] PasswordUtilTest: " + t.getMessage());
            failed++;
        }

        try {
            ProductValidationTest.runTests();
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] ProductValidationTest: " + t.getMessage());
            failed++;
        }

        try {
            BusinessLogicTest.runTests();
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] BusinessLogicTest: " + t.getMessage());
            failed++;
        }

        try {
            DatabaseIntegrationTest.runTests();
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] DatabaseIntegrationTest: " + t.getMessage());
            failed++;
        }

        System.out.println("==========================================================================");
        System.out.printf("TEST SUITE SUMMARY: Total Test Suites: %d | PASSED: %d | FAILED: %d\n", (passed + failed), passed, failed);
        System.out.println("==========================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
