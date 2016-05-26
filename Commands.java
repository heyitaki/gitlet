import java.io.File;
import java.nio.file.Path;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;

public class Commands {
    private static final int HASH_SIZE = 64;
    private static final int HASH_END = 69;
    private static final int FILE_START = 70;


    public static void help() { //LENGTH
        String help = "List of commands: init, add, commit, rm, log, global-log, find, status," 
            + " checkout, branch, rm-branch, reset, merge, rebase, i-rebase";
        System.out.println();
    }

    public static void init() 
            throws IOException, NoSuchAlgorithmException {
        File gitlet = new File("./.gitlet");
        File commits = new File("./.gitlet/commits");
        File branches = new File("./.gitlet/branches");
        File trees = new File("./.gitlet/trees");
        File files = new File("./.gitlet/files");
        File remotes = new File("./.gitlet/remotes");
        if (!gitlet.exists()) {
            gitlet.mkdir();
            commits.mkdir();
            branches.mkdir();
            trees.mkdir();
            files.mkdir();
            remotes.mkdir();

            State.head = "master";
            FileHelper.commit("initial commit");
            FileHelper.updateBranch("master", FileHelper.initialCommit);
            State.save();
        } else {
            System.out.println("A gitlet version control system already" 
                + " exists in the current directory.");
        } 
    } 

    public static void add(String fileName) 
            throws IOException, NoSuchAlgorithmException {
        boolean isSame = FileHelper.isFileInCommit(fileName); 
        File fileToAdd = new File(State.srcDir + "/" + fileName);

        if (fileToAdd.exists()) {
            if (isSame || State.addFiles.contains(fileName)) { 
                System.out.println("File has not been modified since the last commit.");
            } else {
                State.addFiles.add(fileName);
                State.delFiles.remove(fileName); //unmark for deletion
                State.save();
            }
        } else {
            System.out.println("File does not exist.");
        }
    }

    public static void commit(String message) 
            throws IOException, NoSuchAlgorithmException {
        String commit = FileHelper.commit(message);
        FileHelper.updateBranch(State.head, commit);
        State.addFiles.clear();
        State.delFiles.clear();
        State.save();
    }

    public static void remove(String fileName) 
            throws IOException, NoSuchAlgorithmException {
        File fileToDel = new File(State.srcDir + "/" + fileName);
        if (fileToDel.exists()) {
            if (FileHelper.isFileInCommit(fileName) || State.addFiles.contains(fileName)) {
                State.delFiles.add(fileName);
                State.addFiles.remove(fileName); //unmark for staging
                State.save();
            } else {
                System.out.println("No reason to remove the file.");
            }
        } else {
            System.out.println("File does not exist.");
        }    
    }

    public static void log() 
            throws IOException {
        String currCommit = FileHelper.getCurrCommit();
        while (true) {
            System.out.println("====");
            FileHelper.printCommit(currCommit);

            if (currCommit.equals(FileHelper.initialCommit)) {
                break; 
            }

            currCommit = FileHelper.getPrevCommit(currCommit);
        }
    }

    public static void globalLog() 
            throws IOException {
        File[] commits = new File(State.srcDir + "/.gitlet/commits").listFiles();
        String currCommit;
        Path commit;
        for (int i = 0; i < commits.length; i++) {
            currCommit = commits[i].getName();
            System.out.println("====");
            FileHelper.printCommit(currCommit);
        }
    }

    public static void find(String message) 
            throws IOException {
        File commitRoot = new File(State.srcDir + "/.gitlet/commits");
        File[] commits = commitRoot.listFiles();
        String currCommit, commitMsg;
        boolean hasFound = false;
        for (int i = 0; i < commits.length; i++) {
            currCommit = commits[i].getName();
            commitMsg = FileHelper.getCommitMessage(currCommit);
            if (commitMsg.contains(message)) { 
                System.out.println(currCommit.substring(0, HASH_SIZE)); 
            }
        }
    }

    public static void status() {
        System.out.println("=== Branches ===");
        File branchPath = new File(State.srcDir + "/.gitlet/branches");
        File[] branches = branchPath.listFiles();
        for (File f : branches) {
            String branchName = f.getName();
            branchName = branchName.substring(0, branchName.length() - 7); //".branch" removed
            if (branchName.equals(State.head)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }

        System.out.println("");
        System.out.println("=== Staged Files ===");
        for (int i = 0; i < State.addFiles.size(); i++) {
            System.out.println(State.addFiles.get(i));
        }

        System.out.println("");
        System.out.println("=== Files Marked for Removal ===");
        for (int i = 0; i < State.delFiles.size(); i++) {
            System.out.println(State.delFiles.get(i));
        }
    }

    public static void checkout(String input) 
            throws FileNotFoundException, IOException {
        String branchPath = State.srcDir + "/.gitlet/branches/" + input + ".branch";
        File branch = new File(branchPath);
        String hashName, fileName, srcPath, destPath;
        if (branch.exists()) {
            if (input.equals(State.head)) {
                System.out.println("No need to checkout the current branch.");
            } else {
                BufferedReader in = new BufferedReader(new FileReader(branchPath));
                String path = State.srcDir + "/.gitlet/trees/"; //LENGTH
                String treePath = path + FileHelper.getTree(in.readLine());
                in = new BufferedReader(new FileReader(treePath));
                String line;

                while ((line = in.readLine()) != null) {
                    hashName = line.substring(0, HASH_END); 
                    fileName = line.substring(FILE_START, line.length());

                    srcPath = State.srcDir + "/.gitlet/files/" + hashName;
                    destPath = State.srcDir + "/" + fileName;  
                    FileHelper.copyFile(srcPath, destPath);
                }
                in.close();                
            }
            State.head = input;
            State.save();
        } else {
            checkout(FileHelper.getCurrCommit().substring(0, HASH_SIZE), input);
        }
    }

    public static void checkout(String commitID, String fileNameInput) 
            throws FileNotFoundException, IOException {
        String commitName = commitID + ".commit";
        File commit = new File(State.srcDir + "/.gitlet/commits/" + commitName);
        if (commit.exists()) {
            String treeName = FileHelper.getTree(commitName);
            String path = State.srcDir + "/.gitlet/trees/" + treeName; //LENGTH
            BufferedReader in = new BufferedReader(new FileReader(path));
            String line, hashName, fileName, srcPath, destPath;
            boolean fileExists = false;

            while ((line = in.readLine()) != null) {
                hashName = line.substring(0, HASH_END); 
                fileName = line.substring(FILE_START, line.length());

                if (fileName.equals(fileNameInput)) {
                    fileExists = true;
                    srcPath = State.srcDir + "/.gitlet/files/" + hashName;
                    destPath = State.srcDir + "/" + fileName;  
                    FileHelper.copyFile(srcPath, destPath);
                    break;
                }
            }
            in.close();

            if (!fileExists) {
                if (commitName.equals(FileHelper.getCurrCommit())) {
                    System.out.println("File does not exist in the most recent "
                        + "commit, or no such branch exists.");
                } else {
                    System.out.println("File does not exist in that commit.");
                }     
            } 
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    public static void branch(String branchName) 
            throws IOException {
        String branchPath = State.srcDir + "/.gitlet/branches/" + branchName + ".branch";
        File branch = new File(branchPath);
        if (!branch.createNewFile()) {
            System.out.println("A branch with that name already exists.");
        } else {
            PrintWriter out = new PrintWriter(
                new BufferedWriter(new FileWriter(branchPath, false))); //LENGTH
            out.println(FileHelper.getCurrCommit());
            out.close();
        }
    }

    public static void removeBranch(String branchName) {
        File branch = new File(State.srcDir + "/.gitlet/branches/" + branchName + ".branch"); 
        if (branchName.equals(State.head)) {
            System.out.println("Cannot remove the current branch.");
        } else if (branch.exists()) {
            branch.delete();
        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }

    public static void reset(String commitID) 
            throws IOException, FileNotFoundException { 
        String commitName = commitID + ".commit";
        File commit = new File(State.srcDir + "/.gitlet/commits/" + commitName); 
        String hashName, fileName, treePath, srcPath, destPath;
        if (commitName.equals(FileHelper.initialCommit)) { 
            return;
        }
        if (commit.exists()) {
            treePath = State.srcDir + "/.gitlet/trees/" + FileHelper.getTree(commitName);
            BufferedReader in = new BufferedReader(new FileReader(treePath));
            String line;

            while ((line = in.readLine()) != null) {
                hashName = line.substring(0, HASH_END); 
                fileName = line.substring(FILE_START, line.length());

                srcPath = State.srcDir + "/.gitlet/files/" + hashName;
                destPath = State.srcDir + "/" + fileName;  
                FileHelper.copyFile(srcPath, destPath);
            }
            in.close();                
            String branchPath = State.srcDir + "/.gitlet/branches/" + State.head + ".branch";
            FileWriter out = new FileWriter(branchPath, false);
            out.write(commitName); //maybe add new line
            out.close();
            State.save();
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    public static void merge(String branchName) 
            throws IOException {
        if (branchName.equals(State.head)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        File branch = new File(State.srcDir + "/.gitlet/branches/" + branchName + ".branch");
        if (branch.exists()) { //can make faster implementation???
            String srcPath, destPath, ancestorFileHash, currCommitFileHash, givenCommitFileHash;
            String currCommit = FileHelper.getCurrCommit();
            String givenCommit = FileHelper.getCommit(branchName);
            String commonAncestor = FileHelper.getCommonAncestor(currCommit, givenCommit);

            HashMap<String, String> ancestorFiles = 
                FileHelper.listFilesInCommit(commonAncestor); //LENGTH
            HashMap<String, String> currCommitFiles = FileHelper.listFilesInCommit(currCommit);
            HashMap<String, String> givenCommitFiles = FileHelper.listFilesInCommit(givenCommit);

            for (String commitFileName : givenCommitFiles.keySet()) {
                ancestorFileHash = ancestorFiles.get(commitFileName);
                currCommitFileHash = currCommitFiles.get(commitFileName);
                givenCommitFileHash = givenCommitFiles.get(commitFileName);

                if (currCommitFileHash == null) {
                    srcPath = State.srcDir + "/.gitlet/files/" + givenCommitFileHash;
                    destPath = State.srcDir + "/" + commitFileName;
                    FileHelper.copyFile(srcPath, destPath);
                } else {
                    if (currCommitFileHash.equals(ancestorFileHash) //LENGTH
                        && givenCommitFileHash.equals(ancestorFileHash)) { //nothing changed
                        String doNothing = ""; //DELETE
                    } else if (currCommitFileHash.equals(ancestorFileHash)) { //given changed
                        srcPath = State.srcDir + "/.gitlet/files/" + givenCommitFileHash;
                        destPath = State.srcDir + "/" + commitFileName;
                        FileHelper.copyFile(srcPath, destPath);
                    } else if (givenCommitFileHash.equals(ancestorFileHash)) { //keep current
                        String doNothing = ""; //DELETE
                    } else { //both changed
                        srcPath = State.srcDir + "/.gitlet/files/" + givenCommitFileHash;
                        destPath = State.srcDir + "/" + commitFileName + ".conflicted";
                        FileHelper.copyFile(srcPath, destPath);
                    }
                }
            }  
        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }

    public static void rebase(String branchName, boolean interactive) 
            throws IOException, FileNotFoundException, NoSuchAlgorithmException { //LENGTH SPACE
        if (branchName.equals(State.head)) {
            System.out.println("Cannot rebase a branch onto itself.");
            return;
        }
        File branch = new File(State.srcDir + "/.gitlet/branches/" + branchName + ".branch");
        if (branch.exists()) { 
            String currCommit = FileHelper.getCurrCommit();
            String givenCommit = FileHelper.getCommit(branchName);
            String commonAncestor = FileHelper.getCommonAncestor(currCommit, givenCommit);
            String currCommitFileHash, givenCommitFileHash, ancestorFileHash;
            if (givenCommit.equals(commonAncestor)) {
                System.out.println("Already up-to-date.");
                return;
            } else if (currCommit.equals(commonAncestor)) {
                FileHelper.updateBranch(State.head, givenCommit);
                reset(givenCommit.substring(0, HASH_SIZE));
                return;
            } 
            HashMap<String, String> ancestorFiles = FileHelper.listFilesInCommit(commonAncestor); 
            HashMap<String, String> currCommitFiles;
            HashMap<String, String> givenCommitFiles = FileHelper.listFilesInCommit(givenCommit);
            HashMap<String, String> modifiedFiles = new HashMap<String, String>();
            for (String givenFileName : givenCommitFiles.keySet()) {
                ancestorFileHash = ancestorFiles.get(givenFileName);
                givenCommitFileHash = givenCommitFiles.get(givenFileName);
                if (ancestorFileHash == null || !ancestorFileHash.equals(givenCommitFileHash)) {
                    modifiedFiles.put(givenFileName, givenCommitFileHash);
                }
            }
            ArrayList<String> currCommits = FileHelper.getCommitTree(currCommit, commonAncestor);
            String treeName, commit, message, input, lastCommit = "";
            Scanner reader = new Scanner(System.in);
            for (int i = currCommits.size() - 2; i >= 0; i--) {
                commit = currCommits.get(i);
                message = FileHelper.getCommitMessage(commit);
                input = "";
                if (interactive) {
                    System.out.println("Currently replaying:");
                    FileHelper.printCommit(commit); 
                    while (!input.equals("c") && !input.equals("s")) { 
                        System.out.println("Would you like to (c)ontinue, (s)kip "
                            + "this commit, or change this commit's (m)essage?");
                        input = reader.nextLine();
                        if (input.equals("s") && (i == 0 || i == currCommits.size() - 2)) { 
                            input = "";
                            continue;
                        } else if (input.equals("m")) { 
                            System.out.println("Please enter a new message for this commit.");
                            message = reader.nextLine();
                        }
                    }
                    if (input.equals("s")) { //skips, continues for loop
                        continue;
                    }
                }
                currCommitFiles = FileHelper.listFilesInCommit(commit);
                boolean treeModified = false;
                for (String modFileName : modifiedFiles.keySet()) {
                    ancestorFileHash = ancestorFiles.get(modFileName);
                    currCommitFileHash = currCommitFiles.get(modFileName);
                    if (currCommitFileHash == null || currCommitFileHash.equals(ancestorFileHash)) {
                        currCommitFiles.put(modFileName, modifiedFiles.get(modFileName));
                        treeModified = true;
                    } 
                }
                if (treeModified) {
                    treeName = FileHelper.createTree(currCommitFiles);
                } else {
                    treeName = FileHelper.getTree(commit);
                } 
                lastCommit = FileHelper.commitFromTree(givenCommit, treeName, message);
                givenCommit = lastCommit; 
            }
            FileHelper.updateBranch(State.head, lastCommit);
            reset(lastCommit.substring(0, HASH_SIZE));
        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }

    //EXTRA==============================================================================

    public static void clear(File dir) 
            throws IOException {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                clear(f);
            }
        }
        dir.delete();
    }

    public static void print() {
        System.out.println(State.head);
    }
}
