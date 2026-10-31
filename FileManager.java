import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Entry point for the Login option: authenticates against LDAP, then opens the file menu.
 *
 * Usage: java -cp build FileManager [working-directory]
 */
public class FileManager
{
    private static final int MAX_ATTEMPTS = 3;

    public static void main(String[] args)
    {
        Path workingDir = Path.of(args.length > 0 ? args[0] : ".");
        Scanner in = new Scanner(System.in);
        Console console = System.console();

        System.out.println("Logging in to " + Ldap.getUrl());
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)
        {
            System.out.print("Username: ");
            System.out.flush();
            if (!in.hasNextLine())
            {
                return;
            }
            String username = in.nextLine().trim();
            char[] password = readPassword(console, in);
            if (password == null)
            {
                return;
            }

            boolean ok;
            try
            {
                ok = Ldap.authenticate(username, password);
            }
            catch (Ldap.LdapConfigException e)
            {
                System.out.println("Login unavailable: " + e.getMessage());
                System.exit(2);
                return;
            }
            finally
            {
                Arrays.fill(password, '\0');
            }

            if (ok)
            {
                System.out.println("Welcome, " + username + "!");
                try
                {
                    new FileOperations(workingDir, in, System.out).runMenu();
                }
                catch (IOException e)
                {
                    System.out.println("Could not open working directory " + workingDir + ": " + e.getMessage());
                    System.exit(1);
                }
                System.out.println("Logged out.");
                return;
            }
            System.out.println("Incorrect username or password.");
        }
        System.out.println("Too many failed attempts.");
        System.exit(1);
    }

    /** Reads a password without echoing it when running in a real terminal. */
    private static char[] readPassword(Console console, Scanner in)
    {
        if (console != null)
        {
            return console.readPassword("Password: ");
        }
        System.out.print("Password (input will be visible): ");
        System.out.flush();
        return in.hasNextLine() ? in.nextLine().toCharArray() : null;
    }
}
