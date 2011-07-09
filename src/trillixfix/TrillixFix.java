/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package trillixfix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PipedReader;
import java.io.PrintWriter;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * Formats the broken ActionScript sources produced by Trillix Decompiler 4.1
 * by removing the "package" and "class" prefixes on filenames and by removing
 * any comment lines before the package declaration.
 * @author Michael Archibald
 */
public class TrillixFix {

    private static boolean verbose = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String outStr = "";

        /* if osName is windows, it copies the part of the string that says windows. */
        String osName = System.getProperty("os.name").split(" ")[0];
        String pathSeperator = osName.equals("Windows") ? "\\" : "/";

        if (args.length <= 0) {
            System.out.println("Must specify root directory.");
            System.exit(0);
        }
        File rootDir = new File(args[0]);
        Stack<File> rootContents = recurseDir(rootDir);

        /* Seperatingt the dirs incase renaming them breaks the linke to the files */
        Stack<File> dirs = new Stack<File>();
        Stack<File> files = new Stack<File>();
        for (File file : rootContents) {
            if (file.isDirectory()) {
                dirs.push(file);
            } else {
                files.push(file);
            }
        }

        Stack<File> filesCopy = (Stack<File>) files.clone();

        /* Flips dirs stack so that the deepest dir is popped first */
        Stack<File> tempStack = new Stack<File>();
        for (File dir : dirs) {
            tempStack.push(dir);
        }
        dirs = new Stack<File>();
        for (File dir : tempStack) {
            dirs.push(dir);
        }


        /* find and rename fucked files */
        while (!files.empty()) {
            File crntFile = files.pop();
            String split[] = crntFile.getName().split(" ");

            if (split[0].equals("class") || split[0].equals("package")) {
                outStr = crntFile.getAbsolutePath() + " > ";

                crntFile.renameTo(new File(crntFile.getParent()
                        + pathSeperator + split[1]));

                outStr += split[1] + "\n";
                System.out.println(outStr);
            }
        }

        while (!dirs.empty()) {
            File crntDir = dirs.pop();
            String split[] = crntDir.getName().split(" ");

            if (split[0].equals("class") || split[0].equals("package")) {
                outStr = crntDir.getAbsolutePath() + " > ";

                crntDir.renameTo(new File(crntDir.getParent() + pathSeperator + split[1]));

                outStr += split[1] + "\n";
                System.out.println(outStr);
            }
        }

        for (File file : filesCopy) {
            if (verbose) {
                System.out.println("Removing comment line from " + file.getName() + "...");
            }
            if (removeFirstComment(file)) {
                if (verbose) {
                    System.out.println("Success!");
                }
            }
        }

    }

    static private Stack<File> recurseDir(File aStartingDir)
            throws FileNotFoundException {

//        Stack<File> files = new Stack<File>();
//        Stack<File> dirs = new Stack<File>();
//        
        Stack<File> fileStack = new Stack<File>();

        List<File> dirContents = Arrays.asList(aStartingDir.listFiles());
        for (File file : dirContents) {
            if (file.isFile()) {
                fileStack.push(file);
            } else {
                fileStack.push(file);

                Stack<File> deeperList = recurseDir(file);
                Stack<File> tempStack = new Stack<File>();

                for (File deepPop : deeperList) {
                    tempStack.push(deepPop);
                }
                for (File tempPop : tempStack) {
                    fileStack.push(tempPop);
                }
            }
        }
        return fileStack;
    }

    /**
     * Removes any comments above the package line as they cause an error
     * in ActionScript.
     * @param filePath The absolute path of the file.
     * @return true if found comment problem and fixed it.
     */
    public static boolean removeFirstComment(String filePath) throws IOException {
        return removeFirstComment(new File(filePath));
    }

    /**
     * Removes any comments above the package line as they cause an error
     * in ActionScript.
     * @param file file to fix.
     * @return true if found and fixed problem.
     */
    public static boolean removeFirstComment(File file) throws IOException {
        BufferedReader fileReader = null;
        List<String> fileRep = new ArrayList<String>();

        try {
            File tmp = File.createTempFile("tmp", ".txt", file.getParentFile());
            copyFile(file, tmp);

            fileRep = new ArrayList<String>();

            InputStreamReader inStream = new InputStreamReader(new FileInputStream(file),"UTF-16");
            inStream.getEncoding();
            fileReader = new BufferedReader(new LineNumberReader(inStream));
//            fileReader = new Scanner(file);

            String crntLine = "";
            while ((crntLine = fileReader.readLine()) != null) {
//                crntLine = fileReader.readLine();
//                crntLine = fileReader.readLine();
                fileRep.add(crntLine);
            }

            if (fileRep.size() > 0) {
                for (int i = 0; i < fileRep.size(); i++) {
                    String str = fileRep.get(i);
//                    str = str.replaceAll(" ", "");
                    str = str.toLowerCase();

//                    if (str.matches("(p a c k a g e)+")) {
                    if (str.split("//")[0].contains("package")) {
//                        for (int j = 0; j < i; j++) {
//                            fileRep.set(i, "\n");
                        fileRep = fileRep.subList(i, fileRep.size());
                        break;

//                        }
                    }

                }
            }
        } catch (IOException e) {
            return false;
        } finally {
//            fileReader.close();
        }
        fileReader.close();
        return writeFile(fileRep, file);
    }

    private static boolean containsPackage(String str) {
        str = str.toLowerCase();

        if (!str.contains("p")) {
            return false;
        }
        if (!str.contains("a")) {
            return false;
        }
        if (!str.contains("c")) {
            return false;
        }
        if (!str.contains("k")) {
            return false;
        }
        if (!str.contains("g")) {
            return false;
        }
        if (!str.contains("e")) {
            return false;
        }
        return true;
    }

    public static boolean writeFile(List<String> mem, File dest) throws IOException {
        BufferedWriter out = new BufferedWriter(new PrintWriter(dest));

        for (String crntLine : mem) {
            out.write(crntLine);
            out.newLine();
        }

        out.flush();
        out.close();
        return true;
    }

    private static void copyFile(String srFile, String dtFile) {
        copyFile(new File(srFile), new File(dtFile));
    }

    private static void copyFile(File srFile, File dtFile) {
        try {
            InputStream in = new FileInputStream(srFile);

            OutputStream out = new FileOutputStream(dtFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            if (verbose) {
                System.out.println(srFile.getName() + " copied to " + dtFile.getName() + ".");
            }
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " in the specified directory.");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
