import java.security.MessageDigest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;


public class FileHelper {
    public static final String initialCommit = "0000000000000000000000000000000000000000000000000000000000000000.commit"; //ASK IF WE CAN USE SHA256 IDs???

    public static String getHash256(String filename) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(filename);
  
        byte[] data = new byte[1024];
        int read = 0; 
        while ((read = fis.read(data)) != -1) {
            sha256.update(data, 0, read);
        };
        byte[] hashBytes = sha256.digest();
  
        StringBuffer sb = new StringBuffer();
        //char[] conversionArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        for (int i = 0; i < hashBytes.length; i++) {
            sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
            //sb.append(Integer.toString(conversionArray[hashBytes[i]]));
            //System.out.println((hashBytes[i] & 0xff) + 0x100);
        }
        
        fis.close();
        String fileHash = sb.toString();
        return fileHash;
    }
    
    /*public static String treeToObject (String srcDir, String destDir) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        String srcPath, hashFile, destPath, hashTree, dir;
        Stack<String> dirs = new Stack<String>();
        dirs.push("");

        File tempFile = File.createTempFile("tree", ".tmp");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile.toString(), false)));

        while (!dirs.empty()) {
            dir = dirs.pop();
            File folder = new File(srcDir + "/" + dir);
            File[] subFiles = folder.listFiles();

            for (int i = 0; i < subFiles.length; i++) {
                  if (subFiles[i].isFile()) {
                      srcPath =  subFiles[i].toString();
                      hashFile = getHash256(srcPath) + ".file";
                      destPath = destDir + "/.gitlet/files/" + hashFile;
                      copyFile(srcPath, destPath);

                      out.println(hashFile + " " + dir + "/" + subFiles[i].getName());
                  } else { //subFiles[i] = directory
                      String path = subFiles[i].toString();
                      dirs.push(path.substring(srcDir.length(), path.length()).replace("\\", "/"));
                  }
               }
           }
           out.close();
           hashTree = getHash256(tempFile.toString()) + ".tree";
           destPath = destDir + "/.gitlet/trees/" + hashTree;
           copyFile(tempFile.toString(), destPath);

           tempFile.delete();
           return hashTree;
    }

    public static void objectToTree(String srcDir, String firstTreeName, String destDir) throws IOException, FileNotFoundException {
        BufferedReader in = new BufferedReader(new FileReader(srcDir + "/.gitlet/trees/" + firstTreeName));
        String line, hashName, hashExtension, fileName, srcPath, destPath;
        File newFolder;

        while ((line = in.readLine()) != null) {
            hashName = line.substring(0, 69); //as12d2.file
            hashExtension = line.substring(65, 69); //.tree
            fileName = line.substring(70, line.length()); //dir2
            srcPath = srcDir + "/.gitlet/files/" + hashName;
            destPath = destDir + "/" + fileName;

            copyFile(srcPath, destPath);
        }
        in.close();
    } */

    public static void copyFile(String srcPath, String destPath) throws IOException, FileNotFoundException {
//    	System.out.println(srcPath + "->"+destPath);
        File destFile = new File(destPath);
        if (!destFile.exists()) { 
            (Paths.get(destPath)).getParent().toFile().mkdirs();
        }

        InputStream in = new FileInputStream(srcPath);
        OutputStream out = new FileOutputStream(destPath);

        byte[] nextSector = new byte[4096];
        int length;
        while((length = in.read(nextSector)) > 0) {
            out.write(nextSector, 0, length);
        }

        in.close();
        out.close();
    }

    public static String createTree(String prevTreeName, ArrayList<String> addFileNames, ArrayList<String> delFileNames) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        String line, hashTree, hashName, fileName, destPath, hashFile, srcPath;
        File tempFile = File.createTempFile("tree", ".tmp");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile.toString(), false)));

        if(!prevTreeName.equals("")) {
            String prevTreePath = State.srcDir + "/.gitlet/trees/" + prevTreeName;
            BufferedReader in = new BufferedReader(new FileReader(prevTreePath));

            while ((line = in.readLine()) != null) {
                hashName = line.substring(0, 69); //xxxxx.file
                fileName = line.substring(70, line.length()); //dir2
                
                if (delFileNames.contains(fileName) || addFileNames.contains(fileName)) {
                    continue;
                } else {
                    out.println(line);
                }
            }
            in.close();
        }
        
        for(String s : addFileNames) {
            srcPath = State.srcDir + "/" + s;
            hashFile = getHash256(srcPath) + ".file";
            destPath = State.srcDir + "/.gitlet/files/" + hashFile;
            copyFile(srcPath, destPath);

            out.println(hashFile + " " + s);
        }

        out.close();
        hashTree = getHash256(tempFile.toString()) + ".tree";
        destPath = State.srcDir + "/.gitlet/trees/" + hashTree;
        copyFile(tempFile.toString(), destPath);

        tempFile.delete();
        return hashTree;
    }

    public static String createTree(HashMap<String, String> files) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        String hashTree, destPath, hashFile;
        File tempFile = File.createTempFile("tree", ".tmp");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile.toString(), false)));
        
        for(String file : files.keySet()) {
            hashFile = files.get(file);
            out.println(hashFile + " " + file);
        }

        out.close();
        hashTree = getHash256(tempFile.toString()) + ".tree";
        destPath = State.srcDir + "/.gitlet/trees/" + hashTree;
        copyFile(tempFile.toString(), destPath);

        tempFile.delete();
        return hashTree;
    }

    public static String commit(String message) throws IOException, FileNotFoundException, NoSuchAlgorithmException {
        String hashTree, hashCommit, destPath, parentCommit, parentTree;
        File tempFile = File.createTempFile("tree", ".tmp");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile.toString(), false)));

        File checkInitial = new File(State.srcDir + "/.gitlet/commits/" + initialCommit);
        if (checkInitial.exists()) {
            parentCommit = getCurrCommit();
            if (parentCommit.equals(initialCommit)) {
                hashTree = createTree("", State.addFiles, State.delFiles);
            } else {
                parentTree = getTree(parentCommit);
                hashTree = createTree(parentTree, State.addFiles, State.delFiles);
            }
            out.println(parentCommit);
            out.println(hashTree);
            out.println(message);
            out.close();
            hashCommit = getHash256(tempFile.toString()) + ".commit";
        } else {
            out.println("");
            out.println("");
            out.println(message);
            out.close();
            hashCommit = initialCommit;
        }

        destPath = State.srcDir + "/.gitlet/commits/" + hashCommit;
        copyFile(tempFile.toString(), destPath);
        tempFile.delete();

        return hashCommit;
    }

    public static String commitFromTree(String parentCommit, String treeName, String message) throws IOException, FileNotFoundException, NoSuchAlgorithmException {
        String hashCommit, destPath;
        File tempFile = File.createTempFile("tree", ".tmp");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile.toString(), false)));

        out.println(parentCommit);
        out.println(treeName); 
        out.println(message);
        out.close();

        hashCommit = getHash256(tempFile.toString()) + ".commit";

        destPath = State.srcDir + "/.gitlet/commits/" + hashCommit;
        copyFile(tempFile.toString(), destPath);
        tempFile.delete();

        return hashCommit;
    }

    public static void updateBranch(String branchName, String commitName) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(State.srcDir + "/.gitlet/branches/" + branchName + ".branch"));
        out.write(commitName, 0, commitName.length());
        out.close();
    }

    public static String getCommitLine(String commitName, int lineNumber) throws FileNotFoundException, IOException {
        String commitPath = State.srcDir + "/.gitlet/commits/" + commitName;

        BufferedReader in = new BufferedReader(new FileReader(commitPath));
        for(int i = 1; i < lineNumber; i++) {
            in.readLine();
        }

        String commitLine = in.readLine(); //returns xxxxx.commit
        in.close();
        return commitLine;
    }

    public static String getCommit(String branchName) throws IOException, FileNotFoundException {
        String branchPath = State.srcDir + "/.gitlet/branches/" + branchName + ".branch";
        BufferedReader in = new BufferedReader(new FileReader(branchPath));
        String commit = in.readLine();
        in.close();
        return commit; 
    } //returns commitName

    public static String getCurrCommit() throws IOException, FileNotFoundException {
        return getCommit(State.head);
    } //returns commitName

    public static String getPrevCommit(String commitName) throws IOException, FileNotFoundException {
        return getCommitLine(commitName, 1);
    } //returns commitName

    public static String getTree(String commitName) throws IOException, FileNotFoundException { //commitName includes ".commit"
        return getCommitLine(commitName, 2);
    }

    public static String getCommitMessage(String commitName) throws IOException, FileNotFoundException { 
        return getCommitLine(commitName, 3);
    }

    public static void printCommit(String commitName) throws IOException {
        System.out.println("Commit " + commitName.substring(0, 64) + ".");
        Path commit = Paths.get(State.srcDir + "/.gitlet/commits/" + commitName); 
        String creationTime = Files.readAttributes(commit, BasicFileAttributes.class).creationTime().toString();
        String printTime = creationTime.substring(0, 10) + " " + creationTime.substring(11, 19);
        System.out.println(printTime);
        System.out.println(FileHelper.getCommitMessage(commitName));
        System.out.println("");
    }

    public static ArrayList<String> getCommitTree(String startCommit, String endCommit) throws IOException {
        ArrayList<String> commitTree = new ArrayList<String>();
        String prevCommit = startCommit;
        if (!startCommit.equals(endCommit)) {
            do {
                commitTree.add(prevCommit);
                prevCommit = getPrevCommit(prevCommit);
            } while (!prevCommit.equals(endCommit));
        }
        commitTree.add(endCommit);
        return commitTree;
    }

    public static boolean isFileInCommit(String fileName) throws IOException, NoSuchAlgorithmException, FileNotFoundException { 
        String currCommit = getCurrCommit();
        if(currCommit.equals(initialCommit)) {
            return false;
        }

        String treePath = State.srcDir + "/.gitlet/trees/" + getTree(currCommit);
        BufferedReader in = new BufferedReader(new FileReader(treePath));
        String line;

        String filePath = State.srcDir + "/" + fileName;
        String fileHash = getHash256(filePath);
        
        while ((line = in.readLine()) != null) {
            if(fileHash.equals(line.substring(0, 64)) && fileName.equals(line.substring(70, line.length()))) {
                return true;
            }
        }
        return false;
    }

    public static String getCommonAncestor(String firstCommit, String secondCommit) throws IOException {
        ArrayList<String> firstCommitTree = getCommitTree(firstCommit, initialCommit);
        ArrayList<String> secondCommitTree = getCommitTree(secondCommit, initialCommit);
        firstCommitTree.retainAll(secondCommitTree);
        return firstCommitTree.get(0);
    }

    public static HashMap<String, String> listFilesInCommit(String commitName) throws FileNotFoundException, IOException {
        HashMap<String, String> files = new HashMap<String, String>();
        if(!commitName.equals(initialCommit)) {
            String treePath = State.srcDir + "/.gitlet/trees/" + getTree(commitName);
            BufferedReader in = new BufferedReader(new FileReader(treePath));
            String line, hashName, fileName;

            while ((line = in.readLine()) != null) {
                hashName = line.substring(0, 69); //with .tree
                fileName = line.substring(70, line.length());
                files.put(fileName, hashName);
            }
        }
        return files;
    }

    public static void test() throws IOException, NoSuchAlgorithmException {
        String workingDir = System.getProperty("user.dir");
        System.out.println(workingDir); 
        //String dirPath = workingDir + "/dir";
        String objectsPath = workingDir + "/objects";
        String testPath = workingDir + "/test";
        String firstTree = "7bef7b4f2b820c1ccd327f15c1d88dda4b143998cd4fce1f3164c313dc216189.tree";
        //treeToObject("C:/temp/dir", "C:/temp/proj");
        //objectToTree("C:/temp/proj", firstTree, "C:/temp/dir2");
        ArrayList<String> addedFiles = new ArrayList<String>();
        addedFiles.add("/wug.txt");
        ArrayList<String> delFiles = new ArrayList<String>();
        //delFiles.add("/hug.txt");
        //createTree("C:/temp/dir", "C:/temp/proj", firstTree, addedFiles, delFiles);
        String firstCommit = commit("initial commit");
        addedFiles.clear();
        addedFiles.add("/dir2/tug.txt");
        delFiles.add("/wug.txt");
        commit("second commit");
    }

    public static void main(String[] args) {
        try {
            test();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
}