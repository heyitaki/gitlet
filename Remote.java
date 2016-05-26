import java.util.ArrayList;
import java.util.HashMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;

public class Remote {
    private static void init(String src) throws IOException, NoSuchAlgorithmException {
        File gitlet = new File(src);
        File commits = new File(src + "/commits");
        File branches = new File(src + "/branches");
        File trees = new File(src + "/trees");
        File files = new File(src + "/files");
        File remotes = new File(src + "/remotes");

        gitlet.mkdir();
        commits.mkdir();
        branches.mkdir();
        trees.mkdir();
        files.mkdir();
        remotes.mkdir();

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(commits.toString() + "/" + FileHelper.initialCommit, false)));
        out.println("");
        out.println("");
        out.println("initial commit");
        out.close();

        out = new PrintWriter(new BufferedWriter(new FileWriter(branches.toString() + "/master.branch")));
        out.println(FileHelper.initialCommit);
        out.close();
    }
 
    public static void addRemote (String remoteName, String userName, String serverName, String src) throws IOException {
        File remoteFile = new File(State.srcDir + "/.gitlet/remotes/" + remoteName + ".remote");
        if (remoteFile.exists()) {
            System.out.println("A remote with that name already exists.");
        } else {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(remoteFile.toString())));
            out.println(userName);
            out.println(serverName);
            out.println(src);
            out.close();
        }
    }
 
    public static void rmRemote (String remoteName) throws IOException {
        File remoteFile = new File(State.srcDir + "/.gitlet/remotes/" + remoteName + ".remote");
        if (remoteFile.exists()) {
        	remoteFile.delete();
        } else {
            System.out.println("A remote with that name does not exist.");
        }
    }
 
    public static void push (String remoteName, String remoteBranchName) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        File remoteFile = new File(State.srcDir + "/.gitlet/remotes/" + remoteName + ".remote");

        if (remoteFile.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(remoteFile.toString()));
            String userName = in.readLine();
            String serverName = in.readLine();
            String src = in.readLine();
            in.close();

            if (execute("ssh " + userName + "@" + serverName + " 'ls " + src + "'", null) == null) {
                execute("ssh " + userName + "@" + serverName + " 'mkdir -p " + src + "'", null);
            }

            String gitlet = src + "/.gitlet";
            if (execute("ssh " + userName + "@" + serverName + " 'ls " + gitlet + "'", null) == null) {
                 execute("scp -r './.gitlet' " + userName + "@" + serverName + ":" + gitlet, null);
            } else {
                String branch = gitlet + "/branches/" + remoteBranchName + ".branch";
                if (execute("ssh " + userName + "@" + serverName + " 'ls " + branch + "'", null) == null) {
                    String tempFile = new File("_x.tmp").toString();
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile, false)));
                    out.println(FileHelper.initialCommit);
                    out.close();

                    execute("scp " + tempFile + " " + userName + "@" + serverName + ":" + branch, null);
                    new File(tempFile).delete();
                }

                BufferedReader br = execute("ssh " + userName + "@" + serverName + " 'cat " + branch + "'", null);
                String remoteCommit = br.readLine();
                br.close();
                ArrayList<String> localCommits = FileHelper.getCommitTree(FileHelper.getCommit(State.head), FileHelper.initialCommit);

                if (localCommits.contains(remoteCommit)) {
                    ArrayList<String> commits = FileHelper.getCommitTree(FileHelper.getCommit(State.head), remoteCommit);
                    for (String c : commits) {
                        execute("scp ./.gitlet/commits/" + c + " " + userName + "@" + serverName + ":" + gitlet + "/commits/" + c, null);
                        if (!c.equals(FileHelper.initialCommit)) {
                            String tree = FileHelper.getTree(c);
                            execute("scp ./.gitlet/trees/" + tree + " " + userName + "@" + serverName + ":" + gitlet + "/trees/" + tree, null);
                            HashMap<String, String> files = FileHelper.listFilesInCommit(c);
                            for (String fileHash : files.values()) {
                                execute("scp ./.gitlet/files/" + fileHash + " " + userName + "@" + serverName + ":" + gitlet + "/files/" + fileHash, null);
                            }
                        }
                    }

                    String tempFile = new File("_x.tmp").toString();
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile, false)));
                    out.println(FileHelper.getCommit(State.head));
                    out.close();

                    execute("scp " + tempFile + " " + userName + "@" + serverName + ":" + branch, null);
                    new File(tempFile).delete();
                } else {
                    System.out.println("Please pull down remote changes before pushing.");
                }
            }
        }   
    }

    public static void pull(String remoteName, String remoteBranchName) throws IOException, FileNotFoundException, NoSuchAlgorithmException {
        File remoteFile = new File(State.srcDir + "/.gitlet/remotes/" + remoteName + ".remote");
        if (remoteFile.exists()) {
           BufferedReader in = new BufferedReader(new FileReader(remoteFile.toString()));
           String userName = in.readLine();
           String serverName = in.readLine();
           String src = in.readLine();
           in.close();

           String gitlet = src + "/.gitlet";
           String branch = gitlet + "/branches/" + remoteBranchName + ".branch";
           BufferedReader br = execute("ssh " + userName + "@" + serverName + " 'cat " + branch + "'", null);

           if (br == null) {
               System.out.println("That remote does not have that branch.");
            } else {
                String remoteCommit = br.readLine();
                br.close();
                String localCommit = FileHelper.getCommit(State.head);               
                ArrayList<String> localCommits = FileHelper.getCommitTree(localCommit, FileHelper.initialCommit);
                if (localCommits.contains(remoteCommit)) {
                    System.out.println("Already up-to-date.");
                    return;
                }

                ArrayList<String> remoteCommits = getRemoteCommitTree(userName, serverName, src, remoteCommit, FileHelper.initialCommit);
                if (remoteCommits.contains(localCommit)) {
                    ArrayList<String> commits = getRemoteCommitTree(userName, serverName, src, remoteCommit, localCommit);
                    for (int i = commits.size() - 2; i >= 0; i--) {
                        String commit = commits.get(i);                   
                        execute("scp " + userName + "@" + serverName + ":" + gitlet + "/commits/" + commit + " " + "./.gitlet/commits/" + commit, null);
                        if (!commit.equals(FileHelper.initialCommit)) {
                            String tree = FileHelper.getTree(commit);
                            execute("scp " + userName + "@" + serverName + ":" + gitlet + "/trees/" + tree + " " + "./.gitlet/trees/" + tree, null);
                            HashMap<String, String> files = FileHelper.listFilesInCommit(commit);
                            for (String fileHash : files.values()) {
                                execute("scp " + userName + "@" + serverName + ":" + gitlet + "/files/" + fileHash + " " + "./.gitlet/files/" + fileHash, null);
                            }
                        }

                        if (i == 0) {
                            Commands.reset(commit.substring(0,  64));
                            //FileHelper.updateBranch(State.head, commit);
                            return;
                        }
                    }
                } else {
                	remoteCommits.retainAll(localCommits);
                    String commonAncestor = remoteCommits.get(0);

                    ArrayList<String> commits = getRemoteCommitTree(userName, serverName, src, remoteCommit, commonAncestor);
                    for (int i = commits.size() - 2; i >= 0; i--) {
                        String commit = commits.get(i);

                    	execute("scp " + userName + "@" + serverName + ":" + gitlet + "/commits/" + commit + " " + "./.gitlet/commits/" + commit, null);

                        String tree = FileHelper.getTree(commit);
                        execute("scp " + userName + "@" + serverName + ":" + gitlet + "/trees/" + tree + " " + "./.gitlet/trees/" + tree, null);
                        HashMap<String, String> files = FileHelper.listFilesInCommit(commit);
                        for (String fileHash : files.values()) {
                            execute("scp " + userName + "@" + serverName + ":" + gitlet + "/files/" + fileHash + " " + "./.gitlet/files/" + fileHash, null);
                        }
                    }
                    

                    HashMap<String, String> commonAncestorFiles = FileHelper.listFilesInCommit(commonAncestor);
                    HashMap<String, String> localCommitFiles = FileHelper.listFilesInCommit(localCommit);
                    HashMap<String, String> modifiedFiles = new HashMap<String, String>();

                    for (String fileName : localCommitFiles.keySet()) {
                       String ancestorFileHash = commonAncestorFiles.get(fileName);
                       String localFileHash = localCommitFiles.get(fileName);
                       if (ancestorFileHash == null || !ancestorFileHash.equals(localFileHash)) {
                           modifiedFiles.put(fileName, localFileHash);
                       }
                   }

                    if (!modifiedFiles.isEmpty())
                    {
		                HashMap<String, String> CommitFiles = FileHelper.listFilesInCommit(remoteCommit);
		                for (String fileName : modifiedFiles.keySet()) {
		                    CommitFiles.put(fileName, localCommitFiles.get(fileName));
		                }
	                    String treeName = FileHelper.createTree(CommitFiles);
	                    
	                    String commit = FileHelper.commitFromTree(remoteCommit, treeName, FileHelper.getCommitMessage(localCommit) + " - merged");
                    	Commands.reset(commit.substring(0,  64));                	
                    } else {
                    	Commands.reset(remoteCommit.substring(0,  64));
                    }
                }
            }
        }
    }

    public static void clone(String remoteName) throws IOException, FileNotFoundException, ClassNotFoundException {
        File remoteFile = new File(State.srcDir + "/.gitlet/remotes/" + remoteName + ".remote");
        if (remoteFile.exists()) {
           BufferedReader in = new BufferedReader(new FileReader(remoteFile.toString()));
           String userName = in.readLine();
           String serverName = in.readLine();
           String src = in.readLine();
           in.close();

			String gitlet = src + "/.gitlet";
			File localDir = new File("./" + remoteName);
			if (!localDir.exists()) {
				localDir.mkdirs();
			}

			execute("scp -r " + userName + "@" + serverName + ":" + gitlet + " ./" + remoteName + "/.gitlet", null);
			System.setProperty("user.dir", new File(remoteName).getAbsolutePath());
			State.load();

			String commit = FileHelper.getCommit(State.head);
            Commands.reset(commit.substring(0, 64));
        } else {
             System.out.println("A remote with that name does not exist.");
        }
    }

    private static BufferedReader execute (String command, File workingDirectory) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(command, null, workingDirectory);
        
        String s;
        boolean error = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        while ((s = br.readLine()) != null) {
           error = true;
        }

        br.close();
        if (error) {
           return null;
        } else {
           return new BufferedReader(new InputStreamReader(proc.getInputStream()));
        }
    }

    public static ArrayList<String> getRemoteCommitTree(String userName, String serverName, String src, String startCommit, String endCommit) throws IOException {
        ArrayList<String> commitTree = new ArrayList<String>();
        String prevCommit = startCommit;
		String gitletCommit = src + "/.gitlet/commits/";

		while (true) { 	
			commitTree.add(prevCommit);
			if (prevCommit.equals(endCommit)) {
				break;
			}
			BufferedReader br = execute("ssh " + userName + "@" + serverName + " 'cat " + gitletCommit + prevCommit + "'", null);
			prevCommit = br.readLine();
			br.close();
		}
        return commitTree;
    }
}
