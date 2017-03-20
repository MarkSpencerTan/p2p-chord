import java.rmi.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.nio.file.*;

public class ChordUser
{
     int port;

    /** Creates an md5 hashed key for the guid **/
    private long md5(String objectName)
    {
        try
        {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(objectName.getBytes());
            BigInteger bigInt = new BigInteger(1,m.digest());
            return Math.abs(bigInt.longValue());
        }
        catch(NoSuchAlgorithmException e)
        {
                e.printStackTrace();
                
        }
        return 0;
    }
    
    
     public ChordUser(int p) {
         port = p;
        
         Timer timer1 = new Timer();
         timer1.scheduleAtFixedRate(new TimerTask() {
            @Override
             public void run() {
                 try {
                     long guid = md5("" + port);
                     Chord chord = new Chord(port, guid);
                     try{
                         Files.createDirectories(Paths.get(guid+"/repository"));
                     }
                     catch(IOException e)
                     {
                         e.printStackTrace();
                         
                     }
                     System.out.println("Usage: \n\tjoin <ip> <port>\n\twrite <file> (the file must be an integer stored in the working directory, i.e, ./"+guid+"/file");
                     System.out.println("\tread <file>\n\tdelete <file>\n\tprint\n\tleave");
        
                     Scanner scan= new Scanner(System.in);
                     String delims = "[ ]+";
                     String command = "";
                     while (true)
                     {
                         String text= scan.nextLine();
                         String[] tokens = text.split(delims);
                         if (tokens[0].equals("join") && tokens.length == 3) {
                             try {
                                 int portToConnect = Integer.parseInt(tokens[2]);
                                 
                                 chord.joinRing(tokens[1], portToConnect);
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                         }
                         if (tokens[0].equals("print")) {
                             chord.Print();
                         }
                         if  (tokens[0].equals("write") && tokens.length == 2) {
                             try {
                                 String path;
                                 String fileName = tokens[1];
                                 long guidObject = md5(fileName);
                                 // If you are using windows you have to use
                                 // path = ".\\"+  guid +"\\"+fileName; // path to file
                                 path = "./"+  guid +"/"+fileName; // path to file
                                 FileStream file = new FileStream(path);
                                 ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                 peer.put(guidObject, file); // put file into ring
                             } catch (IOException e) {
                                 e.printStackTrace();
                             } catch (Exception e){
                                 System.out.println("File not found");
                             }
                         }
                         // Downloads a file from a chord to the local filesystem
                         if  (tokens[0].equals("read") && tokens.length == 2) {
                             try {
                                 String filename = tokens[1];
                                 Path path = Paths.get("./" + guid + "/" + filename);
                                 long guidObject = md5(filename);
                                 // get a chord that is responsible for the file
                                 ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                 // open a stream to copy content to stream
                                 InputStream stream = peer.get(guidObject);
                                 // Outputs stream content to a file
                                 Files.copy(stream, path);
                             }catch(IOException e){
                                 e.printStackTrace();
                             }
                        }
                        // Deletes a file stored from a Chord
                        if  (tokens[0].equals("delete") && tokens.length == 2) {
                            try {
                                String filename = tokens[1];
                                long guidObject = md5(filename);
                                // get a chord that is responsible for the file
                                ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                peer.delete(guidObject);
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }

                        // Leaves the ring and passes all files to nearest chord
                        if (tokens[0].equals("leave") && tokens.length == 1){
                            // Get the current path to repo
                            Path path = Paths.get("./" + guid + "/repository");
                            //get nearest chord neighbor that will inherit the file
                            ChordMessageInterface neighbor = chord.closestPrecedingNode(guid);
                            //Copy the files to the nearest chord's repo
                            Path neighbor_path = Paths.get("./" + neighbor.getId() + "/repository");
                            copyFolder(path, neighbor_path);
                            //update successors/routing table??
                        }
                     }
                 }
                 catch(RemoteException e)
                 {
                        System.out.println(e);
                 }
             }
         }, 1000, 1000);
    }
    public static void copyFolder(Path src, Path dest) {
        File src_dir = src.toFile();
        String files[] = src_dir.list();
        for (String file : files) {
            try {
                Path srcPath = Paths.get(src + "/" + file);
                Path destPath = Paths.get(dest + "/" + file);
                Files.copy(srcPath, destPath);
            } catch (IOException e) {
                System.out.println("Invalid Folder paths");
            }
        }
    }


    public static void main(String args[])
    {
        if (args.length < 1 ) {
            throw new IllegalArgumentException("Parameter: <port>");
        }
        try{
            ChordUser chordUser=new ChordUser( Integer.parseInt(args[0]));
        }
        catch (Exception e) {
           e.printStackTrace();
           System.exit(1);
        }
     } 
}
