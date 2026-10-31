# OSS-Project-Group-1

 Read <a href="https://github.com/Trossetti/OSS-Project-Group-1/blob/main/Current_Progress.md">Current_Progress.md</a> for current updates. <p>
 Neo Gentrics </p>

# Startup
Three options: 
- Login 
- Account Creation 
- Exit

# Login
- Ask user for credentials, send encrypted traffic to LDAP server to authenticate.
- Once logged in, user should be able to view, upload, or delete items they have permission to view and modify.
- All requests from the client user to the server should be encrypted.

# Account Creation
- Ask user for a username, check against current list of users in LDAP server's database. If a user already exists, ask for another username.
- Ask user for a password, password should be restricted to something hard to crack.
- If both are sufficient, create user on the server and return user to the main script so they may choose to login, create another user, etc.

# Exit
- Nothing, the program should just end.

# Building and Running
Requires Java 11 or newer and bash.

```
./FileManager.sh [working-directory]
```

The script compiles the Java sources into `build/` on first run (and whenever a `.java` file changes). Choosing **Login** starts `FileManager`, which authenticates against LDAP and then opens a text menu to list, view, write, create and delete files inside the working directory (default: the repo folder). Paths outside the working directory are refused.

LDAP settings are read from environment variables:

| Variable | Default | Notes |
|---|---|---|
| `LDAP_URL` | `ldaps://localhost:636` | Must be `ldaps://`; plain `ldap://` is rejected so passwords are never sent unencrypted. |
| `LDAP_USER_DN` | `uid=%s,ou=people,dc=example,dc=com` | `%s` is replaced with the (escaped) username. |

If the LDAP server uses a self-signed certificate, import it into a Java truststore and pass `-Djavax.net.ssl.trustStore=...` via `JAVA_TOOL_OPTIONS`.

## Source files
- `FileManager.sh` – main menu (Login / Create account / Exit).
- `FileManager.java` – login prompt (up to 3 attempts), then the file menu.
- `Ldap.java` – LDAPS authentication.
- `FileOperations.java` – the file menu and operations.

Account creation is still a TODO.
