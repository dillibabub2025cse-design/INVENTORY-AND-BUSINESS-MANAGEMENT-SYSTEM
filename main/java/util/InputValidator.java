package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * Robust console input reader and validator that prevents application crashes from invalid inputs.
 */
public class InputValidator {

    private static final BufferedReader READER = new BufferedReader(new InputStreamReader(System.in));
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9()\\-\\s]{7,25}$");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static {
        DATE_FORMAT.setLenient(false);
    }

    /**
     * Reads a mandatory non-empty line of text.
     */
    public static String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String input = READER.readLine();
                if (input == null) {
                    return "";
                }
                input = input.trim();
                if (!input.isEmpty()) {
                    return input;
                }
                System.out.println("  [!] Input cannot be empty. Please enter a valid value.");
            } catch (IOException e) {
                System.out.println("  [!] Error reading input. Try again.");
            }
        }
    }

    /**
     * Reads an optional line of text (may be empty).
     */
    public static String readOptionalString(String prompt, String defaultValue) {
        System.out.print(prompt + (defaultValue != null && !defaultValue.isEmpty() ? " [" + defaultValue + "]: " : ": "));
        try {
            String input = READER.readLine();
            if (input == null || input.trim().isEmpty()) {
                return defaultValue;
            }
            return input.trim();
        } catch (IOException e) {
            return defaultValue;
        }
    }

    /**
     * Reads an integer within a specified range [min, max].
     */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = READER.readLine();
                if (line == null) {
                    return min;
                }
                line = line.trim();
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  [!] Please enter a whole number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number format. Please enter a valid integer.");
            } catch (IOException e) {
                System.out.println("  [!] Error reading input.");
                return min;
            }
        }
    }

    /**
     * Reads a positive integer (min >= 1).
     */
    public static int readPositiveInt(String prompt) {
        return readInt(prompt, 1, Integer.MAX_VALUE);
    }

    /**
     * Reads a non-negative integer (min >= 0).
     */
    public static int readNonNegativeInt(String prompt) {
        return readInt(prompt, 0, Integer.MAX_VALUE);
    }

    /**
     * Reads a BigDecimal with a minimum value bound.
     */
    public static BigDecimal readBigDecimal(String prompt, BigDecimal min, boolean allowZero) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = READER.readLine();
                if (line == null) {
                    return min;
                }
                line = line.trim();
                BigDecimal val = new BigDecimal(line);
                if (val.compareTo(min) < 0) {
                    System.out.println("  [!] Value must be at least " + min + ".");
                    continue;
                }
                if (!allowZero && val.compareTo(BigDecimal.ZERO) == 0) {
                    System.out.println("  [!] Value must be greater than zero.");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid decimal number. Please enter a valid monetary/numeric amount (e.g., 29.99).");
            } catch (IOException e) {
                System.out.println("  [!] Error reading input.");
                return min;
            }
        }
    }

    /**
     * Reads a valid email address.
     */
    public static String readEmail(String prompt) {
        while (true) {
            String email = readString(prompt);
            if (EMAIL_PATTERN.matcher(email).matches()) {
                return email;
            }
            System.out.println("  [!] Invalid email format (e.g. name@domain.com). Please try again.");
        }
    }

    /**
     * Reads an optional valid email address.
     */
    public static String readOptionalEmail(String prompt, String defaultValue) {
        while (true) {
            String email = readOptionalString(prompt, defaultValue);
            if (email == null || email.isEmpty()) {
                return email;
            }
            if (EMAIL_PATTERN.matcher(email).matches()) {
                return email;
            }
            System.out.println("  [!] Invalid email format. Leave blank or enter a valid email.");
        }
    }

    /**
     * Reads a valid phone number.
     */
    public static String readPhone(String prompt) {
        while (true) {
            String phone = readString(prompt);
            if (PHONE_PATTERN.matcher(phone).matches()) {
                return phone;
            }
            System.out.println("  [!] Invalid phone number. Please enter digits, +, -, or spaces (min 7 chars).");
        }
    }

    /**
     * Reads a SQL Date in yyyy-MM-dd format, or defaults to current date if user presses Enter.
     */
    public static Date readDate(String prompt, boolean defaultToday) {
        String fullPrompt = prompt + (defaultToday ? " (yyyy-MM-dd or Enter for today): " : " (yyyy-MM-dd): ");
        while (true) {
            System.out.print(fullPrompt);
            try {
                String line = READER.readLine();
                if (line == null || line.trim().isEmpty()) {
                    if (defaultToday) {
                        return new Date(System.currentTimeMillis());
                    }
                    System.out.println("  [!] Date is required.");
                    continue;
                }
                line = line.trim();
                java.util.Date parsed = DATE_FORMAT.parse(line);
                return new Date(parsed.getTime());
            } catch (ParseException e) {
                System.out.println("  [!] Invalid date format. Please use yyyy-MM-dd (e.g., 2026-11-03).");
            } catch (IOException e) {
                System.out.println("  [!] Error reading input.");
            }
        }
    }

    /**
     * Prompts for Yes/No confirmation.
     */
    public static boolean readConfirmation(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            try {
                String line = READER.readLine();
                if (line == null) {
                    return false;
                }
                line = line.trim().toLowerCase();
                if (line.equals("y") || line.equals("yes")) {
                    return true;
                }
                if (line.equals("n") || line.equals("no")) {
                    return false;
                }
                System.out.println("  [!] Please enter 'y' for Yes or 'n' for No.");
            } catch (IOException e) {
                System.out.println("  [!] Error reading input.");
                return false;
            }
        }
    }

    /**
     * Waits for user to press Enter to continue.
     */
    public static void pressEnterToContinue() {
        System.out.print("\nPress [Enter] to return to menu...");
        try {
            READER.readLine();
        } catch (IOException ignored) {}
    }
}

