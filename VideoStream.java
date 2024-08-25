import java.io.*;

public class VideoStream {

    FileInputStream fis; //video file
    int frame_nb; //current frame nb

    //-----------------------------------
    // Constructor
    //-----------------------------------
    public VideoStream(String filename) throws Exception{

        //Init variables
        fis = new FileInputStream(filename);
        frame_nb = 0;
    }

    //-----------------------------------
    // Getnextframe
    // Returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
    public int getnextframe(byte[] frame) throws Exception
    {
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];

        //Read current frame length
        fis.read(frame_length,0,5);

        //Transform frame_length to integer
        length_string = new String(frame_length);
        length = Integer.parseInt(length_string);
        System.out.println("Frame "+frame_length);

        return(fis.read(frame,0,length));
    }
}
