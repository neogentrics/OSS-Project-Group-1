import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * Authenticates a user against an LDAP server over an encrypted (LDAPS) connection.
 *
 * Configuration comes from environment variables so no server details or credentials
 * are hardcoded in the source:
 *
 *   LDAP_URL          e.g. ldaps://ldap.example.com:636   (must start with ldaps://)
 *   LDAP_USER_DN      e.g. uid=%s,ou=people,dc=example,dc=com   (%s is replaced by the username)
 */
public class Ldap
{
    public static final String DEFAULT_URL = "ldaps://localhost:636";
    public static final String DEFAULT_USER_DN = "uid=%s,ou=people,dc=example,dc=com";

    /** Thrown when the LDAP settings are wrong or the server can't be reached. */
    public static class LdapConfigException extends Exception
    {
        private static final long serialVersionUID = 1L;

        public LdapConfigException(String message)
        {
            super(message);
        }
    }

    public static String getUrl()
    {
        String url = System.getenv("LDAP_URL");
        return (url == null || url.isEmpty()) ? DEFAULT_URL : url;
    }

    public static String getUserDnTemplate()
    {
        String template = System.getenv("LDAP_USER_DN");
        return (template == null || template.isEmpty()) ? DEFAULT_USER_DN : template;
    }

    /**
     * Returns true if the username/password are accepted by the LDAP server,
     * false if the credentials are wrong.
     */
    public static boolean authenticate(String username, char[] password) throws LdapConfigException
    {
        String url = getUrl();
        if (!url.toLowerCase().startsWith("ldaps://"))
        {
            throw new LdapConfigException("LDAP_URL must use ldaps:// so credentials are encrypted (got " + url + ")");
        }

        // An empty password makes LDAP do an anonymous bind, which "succeeds" for anyone.
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0)
        {
            return false;
        }

        String userDn = String.format(getUserDnTemplate(), escapeDnValue(username.trim()));

        Hashtable<String, Object> environment = new Hashtable<String, Object>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, url);
        environment.put(Context.SECURITY_PROTOCOL, "ssl");
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        environment.put(Context.SECURITY_PRINCIPAL, userDn);
        environment.put(Context.SECURITY_CREDENTIALS, password.clone());
        environment.put("com.sun.jndi.ldap.connect.timeout", "5000");

        try
        {
            DirContext context = new InitialDirContext(environment);
            context.close();
            return true;
        }
        catch (AuthenticationNotSupportedException exception)
        {
            throw new LdapConfigException("The authentication is not supported by the server");
        }
        catch (AuthenticationException exception)
        {
            return false;
        }
        catch (NamingException exception)
        {
            throw new LdapConfigException("Could not connect to " + url + ": " + exception.getMessage());
        }
        finally
        {
            Arrays.fill((char[]) environment.get(Context.SECURITY_CREDENTIALS), '\0');
        }
    }

    /** Escapes characters that have special meaning in a DN (RFC 4514), so usernames can't inject DN parts. */
    static String escapeDnValue(String value)
    {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            boolean special = ",+\"\\<>;=".indexOf(c) >= 0
                || (i == 0 && (c == ' ' || c == '#'))
                || (i == value.length() - 1 && c == ' ');
            if (special)
            {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
