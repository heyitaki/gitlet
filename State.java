import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class State {
    public static ArrayList<String> addFiles;
    public static ArrayList<String> delFiles;
    public static String head;
    public static String srcDir;

    @SuppressWarnings("unchecked")
    public static void load() throws IOException, FileNotFoundException, ClassNotFoundException {
        srcDir = getCurrentFolder();
        String filePath = srcDir + "/.gitlet/state.ser";
        if (Files.exists(Paths.get(filePath))) {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath));
            head = (String) in.readObject();
            addFiles = (ArrayList<String>) in.readObject();
            delFiles = (ArrayList<String>) in.readObject();
            in.close();
        } else {
            addFiles = new ArrayList<String>();
            delFiles = new ArrayList<String>();
        }
    }

    public static void save() throws IOException, FileNotFoundException { //LENGTH
        String path = srcDir + "/.gitlet/state.ser";
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
        out.writeObject(head);
        out.writeObject(addFiles);
        out.writeObject(delFiles);
        out.close();
    }

    private static String getCurrentFolder() {
        return System.getProperty("user.dir");
    }
}
