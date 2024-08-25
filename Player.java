import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Player {
    // GUI
    //----
    JFrame f = new JFrame("MJJPEG Player");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Close");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel statLabel1 = new JLabel();
    JLabel statLabel2 = new JLabel();
    JLabel statLabel3 = new JLabel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;

    Timer timer; //timer used to play frames

    //Video constants:
    //------------------
    int imagenb = 0;                    //image nb of the image currently read
    static String VideoFileName;        //video file to play
    byte[] frame;                       //buffer used to store the video frames
    static VideoStream video;           //videoStream object used to access video frames
    static int VIDEO_LENGTH = 500;      //length of the video in frames

    //Statistics variables:
    //------------------
    double statTotalPlayTime;   //time in milliseconds of video playing since beginning
    double statDataRate;        //rate of video data received in bytes/s
    int statTotalBytes;         //total number of bytes received in a session
    double statStartTime;       //time in milliseconds when start is pressed

    FrameSynchronizer fsynch;
   
    //--------------------------
    //Constructor
    //--------------------------
    public Player() {    
        //Build GUI
        //--------------------------
        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());
        tearButton.addActionListener(new tearButtonListener());

        //Statistics
        statLabel1.setText("Total Bytes Received: 0");
        statLabel2.setText("Data Rate (bytes/sec): 0");     
        statLabel3.setText("Play frame #");

        //Image display label
        iconLabel.setIcon(null);
        
        //Frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        mainPanel.add(statLabel1);
        mainPanel.add(statLabel2);
        mainPanel.add(statLabel3);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);
        statLabel1.setBounds(0,330,380,20);
        statLabel2.setBounds(0,350,380,20);
        statLabel3.setBounds(0,370,380,20);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(380,420));
        f.setVisible(true);

        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                timer.stop();
                System.exit(0);
            }
        });

        //Init timer
        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //Allocate enough memory for the buffer used for frames
        frame = new byte[15000];    

        //Create the frame synchronizer
        fsynch = new FrameSynchronizer(100);
    }

    //------------------------------------
    // Main
    //------------------------------------
    public static void main(String argv[]) throws Exception {
        //get video filename to request:
        VideoFileName = argv[0];
        video = new VideoStream(VideoFileName);
        new Player();
    }
    
    
    //------------------------------------
    // Handler for timer
    //------------------------------------
    class timerListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
          
        //If the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH) {
            //Update current imagenb
            imagenb++;

            try {
                double curTime = System.currentTimeMillis();
                statTotalPlayTime += curTime - statStartTime; 
                statStartTime = curTime;

                //Get next frame to play, as well as its size
                int image_length = video.getnextframe(frame);
                
                //Get to total length of the full frame
                int packet_length = frame.length;

                statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
                statTotalBytes += packet_length;

                //Update GUI
                updateStatsLabel();
                
                //Get an Image object from the frame
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                fsynch.addFrame(toolkit.createImage(frame, 0, packet_length), imagenb);

                //Display the image as an ImageIcon object
                icon = new ImageIcon(fsynch.nextFrame());
                iconLabel.setIcon(icon);

                System.out.println("Play frame #" + imagenb + ", Frame size: " + image_length + " (" + packet_length + ")");
            }
            catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
                System.exit(0);
            }
            catch(Exception ex) {
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
        }
        else {
            //if we have reached the end of the video file, stop the timer
            timer.stop();
        }
        }
    }


    //------------------------------------
    // Handler for buttons
    //------------------------------------

    // Handler for Play button
    //-----------------------
    class playButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            //Start to save the time in stats
            statStartTime = System.currentTimeMillis();

            System.out.println("Play Button pressed!"); 

            //Start the timer
            timer.start();
        }
    }

    // Handler for Pause button
    //-----------------------
    class pauseButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e){

            System.out.println("Pause Button pressed!");   

            //Stop the timer
            timer.stop();
        }
    }

    // Handler for Teardown button
    //-----------------------
    class tearButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e){

            System.out.println("Teardown Button pressed !");  

            //Exit
            timer.stop();
            System.exit(0);
        }
    }

    //------------------------------------
    // Synchronize frames
    //------------------------------------
    class FrameSynchronizer {

        private ArrayDeque<Image> queue;
        private int bufSize;
        private int curSeqNb;
        private Image lastImage;

        public FrameSynchronizer(int bsize) {
            curSeqNb = 1;
            bufSize = bsize;
            queue = new ArrayDeque<Image>(bufSize);
        }

        //Synchronize frames based on their sequence number
        public void addFrame(Image image, int seqNum) {
            if (seqNum < curSeqNb) {
                queue.add(lastImage);
            }
            else if (seqNum > curSeqNb) {
                for (int i = curSeqNb; i < seqNum; i++) {
                    queue.add(lastImage);
                }
                queue.add(image);
            }
            else {
                queue.add(image);
            }
        }

        //Get the next synchronized frame
        public Image nextFrame() {
            curSeqNb++;
            lastImage = queue.peekLast();
            return queue.remove();
        }
    }

    private void updateStatsLabel() {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        statLabel1.setText("Total Bytes played: " + statTotalBytes + " bytes");
        statLabel2.setText("Data Rate (bytes/sec): " + formatter.format(statDataRate) + " bytes/s");
        statLabel3.setText("Play frame #" + imagenb);
    }
}
