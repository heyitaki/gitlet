import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.io.File;
import java.util.Scanner; 

public class Gitlet {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Please enter a command.");
                Commands.help();
                return;
            }

            State.load();
            if (!args[0].equals("init") && !checkGitlet()) { //LENGTH
                String error = "A gitlet version control system does" 
                    + " not exist in the current directory.";
                System.out.println(error);
                return;
            }

            gitletMain(args);
        } catch (IOException | NoSuchAlgorithmException | ClassNotFoundException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    private static void doInit(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 1) {
            Commands.help();
            return;
        }
        Commands.init();
    }

    private static void doAdd(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        Commands.add(inputs[1]);
    }

    private static void doCommit(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        Commands.commit(inputs[1]);
    }

    private static void doStatus(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 1) {
            Commands.help();
            return;
        }
        Commands.status();
    }

    private static void doRm(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        Commands.remove(inputs[1]);
    }

    private static void doLog(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 1) {
            Commands.help();
            return;
        }
        Commands.log();
    }

    private static void doGlobalLog(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 1) {
            Commands.help();
            return;
        }
        Commands.globalLog();
    }

    private static void doFind(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        Commands.find(inputs[1]);
    }

    private static void doCheckout(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length < 2 || inputs.length > 3) {
            Commands.help();
            return;
        }
        if (warnUser()) {
            if (inputs.length == 3) {
                Commands.checkout(inputs[1], inputs[2]);
            } else {
                Commands.checkout(inputs[1]);
            }
        }
    }

    private static void doBranch(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        Commands.branch(inputs[1]);
    }

    private static void doRmBranch(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        Commands.removeBranch(inputs[1]);
    }

    private static void doReset(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        if (warnUser()) {
            Commands.reset(inputs[1]);
        }
    }
    
    private static void doMerge(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        if (warnUser()) {
            Commands.merge(inputs[1]);
        }
    }
    
    private static void doRebase(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        if (warnUser()) {
            Commands.rebase(inputs[1], false);
        }
    }
    
    private static void doiRebase(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        if (warnUser()) {
            Commands.rebase(inputs[1], true);
        }
    }

    private static void doAddRemote(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 5) {
            Commands.help();
            return;
        }
        Remote.addRemote(inputs[1], inputs[2], inputs[3], inputs[4]);
    }

    private static void doRmRemote(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        Remote.rmRemote(inputs[1]);
    }

    private static void doPush(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 3) {
            Commands.help();
            return;
        }
        Remote.push(inputs[1], inputs[2]);
    }

    private static void doPull(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 3) {
            Commands.help();
            return;
        }
        if (warnUser()) {
            Remote.pull(inputs[1], inputs[2]);
        }
    }
    
    private static void doClone(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 2) {
            Commands.help();
            return;
        }
        if (warnUser()) {
            Remote.clone(inputs[1]);
        }
    }

    private static void doClear(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 1) {
            Commands.help();
            return;
        }
        Commands.clear(new File(State.srcDir + "/.gitlet"));
    }

    private static void doPrint(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        if (inputs.length != 1) {
            Commands.help();
            return;
        }
        Commands.print();
    }

    private static void gitletMain(String[] inputs) 
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        switch (inputs[0]) {
            case "init":
                doInit(inputs);
                break;
            case "add":
                doAdd(inputs);
                break;
            case "commit":
                doCommit(inputs);
                break;
            case "status":
                doStatus(inputs);
                break;
            case "rm":
                doRm(inputs);
                break;
            case "log":
                doLog(inputs);
                break;
            case "global-log":
                doGlobalLog(inputs);
                break;
            case "find":
                doFind(inputs);
                break;
            case "checkout":
                doCheckout(inputs);
                break;
            case "branch":
                doBranch(inputs);
                break;
            case "rm-branch":
                doRmBranch(inputs);
                break;
            case "reset":
                doReset(inputs);
                break;
            case "merge":
                doMerge(inputs);
                break;
            case "rebase":
                doRebase(inputs);
                break;
            case "i-rebase":
                doiRebase(inputs);
                break;
            case "add-remote":
                doAddRemote(inputs);
                break;
            case "rm-remote":
                doRmRemote(inputs);
                break;
            case "push":
                doPush(inputs);
                break;
            case "pull":
                doPull(inputs);
                break;
            case "clone":
                doClone(inputs);
                break;
            case "clear":
                doClear(inputs);
                break;
            case "print":
                doPrint(inputs);
                break;
            default:
                System.out.println("Invalid command: " + inputs[0]);
                Commands.help();
                break;
        }
    }

    private static boolean warnUser() {
        Scanner reader = new Scanner(System.in);
        while (true) { //LENGTH
            String warning = "Warning: The command you entered may alter the files in your " 
                + "working directory. Uncommitted changes may be lost. Are you sure you want "
                + "to continue? (yes/no)";
            System.out.println(warning);
            String input = reader.nextLine();
            if (input.equals("yes")) {
                return true;
            } else if (input.equals("no")) {
                return false;
            }
        }
    }

    private static String constructString(String[] words) {
        StringBuilder builder = new StringBuilder();
        for (String s : words) {
            builder.append(s);
        }
        return builder.toString();
    }

    private static String[] ignoreFirstElement(String[] words) {
        return Arrays.copyOfRange(words, 1, words.length);
    }

    private static boolean checkGitlet() {
        File gitlet = new File("./.gitlet");
        return gitlet.exists();
    }
}
