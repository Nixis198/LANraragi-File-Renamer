import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileRenamer {
    static String folder;
    static String doneFolder;
    static int timeOut;
    public static void main(String[] args) throws Exception {
        // java FileRenamer source destination timeout
        doneFolder = args[1] + "\\";
        timeOut = Integer.parseInt(args[2]);
        run(args[0] + "\\");
    }

    public static void run(String m_folder) throws InterruptedException {
        folder = m_folder;
        String[] list = getFileNames(m_folder);
        for(int i = 0; i < list.length; i++) {
            renameFiles(list[i], runRegex(extractZipComment(list[i])));
            TimeUnit.SECONDS.sleep(timeOut);
        }
    }

    public static void renameFiles(String file, String ID) {
        File oldFile = new File(folder + file);
        File newFile = new File(doneFolder + ID + ". " + file);
        if(oldFile.renameTo(newFile)) {
            System.out.println("Renamed: " + newFile.getName());
        }else {
            System.out.println("Error");
        }
    }

    public static String[] getFileNames(String folder) {
        File f = new File(folder);
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(f.list()));
        String[] array = names.toArray(new String[names.size()]);
        return array;
    }

    public static String runRegex(String comment) {
        Pattern p1 = Pattern.compile("[\n\r][ \t]*URL: [ \t]*([^\n\r]*)");
        Matcher m1 = p1.matcher(comment);
        m1.find();
        Pattern p2 = Pattern.compile("[0-9]+");
        Matcher m2 = p2.matcher(m1.group());
        m2.find();
        return m2.group();
    }

    public static String extractZipComment(String filename) {
        filename = folder + filename;
        String retStr = null;
        try{
            File file = new File(filename);
            int fileLen = (int)file.length();

            FileInputStream in = new FileInputStream(file);

            byte[] buffer =  new byte[Math.min(fileLen, 8192)];
            int len;

            in.skip(fileLen - buffer.length);

            if((len = in.read(buffer)) > 0) {
                retStr = getZipCommentFromBuffer(buffer, len);
            }

            in.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return retStr;
    }

    private static String getZipCommentFromBuffer(byte[] buffer, int len) {
        byte[] magicDirEnd = {0x50, 0x4b, 0x05, 0x06};
        int buffLen = Math.min(buffer.length, len);

        // Check the buffer from the end
        for(int i = buffLen - magicDirEnd.length - 22; i >= 0; i--) {
            boolean isMagicStart = true;
            
            for(int k = 0; k < magicDirEnd.length; k++) {

                if(buffer[i + k] != magicDirEnd[k]) {
                    isMagicStart = false;
                    break;
                }
            }

            if (isMagicStart) {
                // Magic Start found!
                int commentLen = buffer[i + 20] + buffer[i + 21] * 512;
                int realLen = buffLen - i - 22;
                // System.out.println ("ZIP comment found at buffer position " + (i + 22) + " with len = " + commentLen + ", good!");
        
                // if(commentLen != realLen) {
                //     System.out.println ("WARNING! ZIP comment size mismatch: " + "directory says len is " + commentLen + ", but file ends after " + realLen + " bytes!");
                // }
        
                String comment = new String (buffer, i + 22, Math.min(commentLen, realLen));
                return comment;
            }
        }

        System.out.println("ERROR! ZIP comment NOT found!");
        return null;
    }
}
