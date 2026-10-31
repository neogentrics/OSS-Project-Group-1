import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Text-menu file operations, limited to one root directory.
 * This combines the old BasicCreateFile / BasicReadFile / BasicWriteFile /
 * BasicDeleteFile / BasicDeleteFolder / BasicGetFileInfo examples into one place.
 */
public class FileOperations
{
    private final Path root;
    private final Scanner in;
    private final PrintStream out;

    public FileOperations(Path root, Scanner in, PrintStream out) throws IOException
    {
        this.root = root.toAbsolutePath().normalize();
        this.in = in;
        this.out = out;
        Files.createDirectories(this.root);
    }

    /** Shows the file menu until the user logs out (or input ends). */
    public void runMenu()
    {
        while (true)
        {
            out.println();
            out.println("Working directory: " + root);
            out.println("[1] List files");
            out.println("[2] File info");
            out.println("[3] Read file");
            out.println("[4] Write to file (overwrite)");
            out.println("[5] Append to file");
            out.println("[6] Create empty file");
            out.println("[7] Create folder");
            out.println("[8] Delete file");
            out.println("[9] Delete folder (must be empty)");
            out.println("[0] Logout");
            String choice = prompt("Choice: ");
            if (choice == null || choice.equals("0"))
            {
                return;
            }

            try
            {
                switch (choice)
                {
                    case "1": listFiles(); break;
                    case "2": fileInfo(askPath()); break;
                    case "3": readFile(askPath()); break;
                    case "4": writeFile(askPath(), false); break;
                    case "5": writeFile(askPath(), true); break;
                    case "6": createFile(askPath()); break;
                    case "7": createFolder(askPath()); break;
                    case "8": deleteFile(askPath()); break;
                    case "9": deleteFolder(askPath()); break;
                    default: out.println("Error: please enter a number from 0 to 9.");
                }
            }
            catch (IllegalArgumentException e)
            {
                out.println("Error: " + e.getMessage());
            }
            catch (NoSuchFileException e)
            {
                out.println("Error: does not exist: " + relative(Path.of(e.getFile())));
            }
            catch (FileAlreadyExistsException e)
            {
                out.println("Error: already exists: " + relative(Path.of(e.getFile())));
            }
            catch (DirectoryNotEmptyException e)
            {
                out.println("Error: folder is not empty.");
            }
            catch (IOException e)
            {
                out.println("An error occurred: " + e);
            }
        }
    }

    private String prompt(String message)
    {
        out.print(message);
        out.flush();
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    private Path askPath()
    {
        String name = prompt("Path (relative to working directory): ");
        if (name == null || name.isEmpty())
        {
            throw new IllegalArgumentException("no path given.");
        }
        return resolve(name);
    }

    /** Resolves a user-supplied path and refuses anything outside the root (e.g. "../secret"). */
    Path resolve(String name)
    {
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root))
        {
            throw new IllegalArgumentException("path is outside the working directory.");
        }
        return path;
    }

    private String relative(Path path)
    {
        Path abs = path.toAbsolutePath().normalize();
        return abs.startsWith(root) ? root.relativize(abs).toString() : path.toString();
    }

    void listFiles() throws IOException
    {
        try (Stream<Path> entries = Files.list(root))
        {
            List<Path> sorted = entries.sorted().collect(Collectors.toList());
            if (sorted.isEmpty())
            {
                out.println("(empty)");
            }
            for (Path p : sorted)
            {
                out.println((Files.isDirectory(p) ? "[dir]  " : "       ") + p.getFileName());
            }
        }
    }

    void fileInfo(Path path) throws IOException
    {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        out.println("File name: " + path.getFileName());
        out.println("Absolute path: " + path);
        out.println("Type: " + (attrs.isDirectory() ? "folder" : "file"));
        out.println("Size in bytes: " + attrs.size());
        out.println("Last modified: " + new Date(attrs.lastModifiedTime().toMillis()));
        out.println("Readable: " + Files.isReadable(path));
        out.println("Writable: " + Files.isWritable(path));
    }

    void readFile(Path path) throws IOException
    {
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8))
        {
            out.println(line);
        }
    }

    void writeFile(Path path, boolean append) throws IOException
    {
        if (Files.isDirectory(path))
        {
            throw new IllegalArgumentException("that is a folder, not a file.");
        }
        out.println("Enter text. Finish with a line containing only a single '.'");
        StringBuilder text = new StringBuilder();
        String line;
        while (in.hasNextLine() && !(line = in.nextLine()).equals("."))
        {
            text.append(line).append(System.lineSeparator());
        }
        if (append)
        {
            Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        else
        {
            Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8));
        }
        out.println("Successfully wrote to " + relative(path));
    }

    void createFile(Path path) throws IOException
    {
        Files.createFile(path);
        out.println("File created: " + relative(path));
    }

    void createFolder(Path path) throws IOException
    {
        Files.createDirectory(path);
        out.println("Folder created: " + relative(path));
    }

    void deleteFile(Path path) throws IOException
    {
        if (Files.isDirectory(path))
        {
            throw new IllegalArgumentException("that is a folder; use 'Delete folder'.");
        }
        Files.delete(path);
        out.println("Deleted the file: " + relative(path));
    }

    void deleteFolder(Path path) throws IOException
    {
        if (path.equals(root))
        {
            throw new IllegalArgumentException("cannot delete the working directory itself.");
        }
        if (Files.exists(path) && !Files.isDirectory(path))
        {
            throw new IllegalArgumentException("that is a file; use 'Delete file'.");
        }
        Files.delete(path);
        out.println("Deleted the folder: " + relative(path));
    }
}
