/*
Pupose:

Functionality:

Citations:

*/

import java.io.*;
import java.net.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Client {

    public static void main(String args[]) {
        // Primitives to store the initial tcp port, store the confirmation integer, and store the new UDP port.
        int tcpPort = Integer.parseInt(args[1] );
        int confInt = 259 ;
        int newUDPPort = 0 ;

        // Creates a string to store the file name the user wants to send to the server.
        // This string is then converted to a file object.
        String clientFileName = args[2] ;
        File fileToSend = new File(clientFileName) ;

        // Initializes the tcp socket to null ;
        Socket initialTCPSocket = null;

        // If statement which compares the first command line argument. If this is equal to local host the
        // getLocalHost method is called to set the socket address to the local IP address of the machine.
        if (args[0].equals("localhost")) {
            try {
                // Note that we use the port specified by the User's input to the command line.
                initialTCPSocket = new Socket(InetAddress.getLocalHost(), tcpPort);

            } catch (IOException e) {
                e.printStackTrace();
            }
        // Otherwise the socket address is set to that as specified by the User.
        } else if (!args[0].equals("localhost")) {
            try {

                initialTCPSocket = new Socket(args[0], tcpPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Initializes the output stream which will send the confInt to the server.
        DataOutputStream tcpOutputStream = null;
        try {
            // Makes the output stream that of the initial TCP socket.
            tcpOutputStream = new DataOutputStream(initialTCPSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            // Writes the confirmation Int to the output stream which will be received by the server.
            tcpOutputStream.writeInt(confInt) ;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Executes the while loop until the client receives the new UDP connection port from the Server.
        while(newUDPPort == 0)
        {
            // Initializes the data input stream which will be used to get the random port sent from the Server.
            DataInputStream tcpInputStream = null;
            try {
                // Assigns the input stream object to the input stream of the initial TCP socket.
                tcpInputStream = new DataInputStream(initialTCPSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                // Reads the random port integer sent by the Server from the input stream initialized earlier.
                newUDPPort = tcpInputStream.readInt() ;
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Exits while loop if a random port is assigned.
        }

        // Closes the TCP socket after the while loop iterates. Prints the stack trace if an error occurs.
        try {
            initialTCPSocket.close() ;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Enters the If statement if a random UDP port has been assinged.
        if(newUDPPort !=0)
        {
            // Initialises a new UDP socket to be used to send and receive acknowledgements from the Server.
            DatagramSocket udpSocket = null;
            try {
                // Opens the datagram socket (UDP socket) and prints the Random port the datagram packets will be sent.
                udpSocket = new DatagramSocket();
                System.out.println("\nRandom Port: " + newUDPPort + "\n");
            } catch (SocketException e) {
                e.printStackTrace();
            }

            // Enters the If statement if the UDP connection socket has been set.
            if (udpSocket != null) {
                // Creates a new byte array which will be used to store the contents of the file to be sent.
                // Initializes the file input stream which will be used to read the contents of the file into byte [].
                byte[] udpFileToSend = new byte[(int) fileToSend.length()];
                FileInputStream udpFileStream = null;
                try {
                    // Sets the file input stream for the file we want to send. Reads from stream and stores
                    // the output in the byte array. Closes the stream once complete.
                    udpFileStream = new FileInputStream(fileToSend);
                    udpFileStream.read(udpFileToSend); //read file into udpFileToSend []
                    udpFileStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    // Initializes integer which will be used to step through the udpFileToSend [].
                    int i = 0 ;

                    // Do while loop which iterates until i is greater than the udpFileToSend arrays length.
                    do
                    {
                        // Initializes a new byte array to store chunks of the file in 4 byte increments to be sent
                        // to the server.
                        byte[] chunkArray = new byte[4];

                        // For loop iterates through the chunkArray until the end of the array is reached.
                        for(int j=0; j<chunkArray.length; j+=1)
                        {
                            // If statement to only store data in the chunkArray if the udpFileToSend arrays length
                            // has not yet been reached. Prevents us from sending extra garbage bytes to the Server.
                            if(i < udpFileToSend.length)
                            {
                                // Sets the byte of the chunck array equal to that of the udpFileToSend array.
                                // Increments i.
                                chunkArray[j] = udpFileToSend[i] ;
                                i+=1 ;
                            }
                        }

                        // Creates the datagram packet to send to the server, giving it the necessary parameters to set
                        // its length to that of the chunkArray, store the chunkArray and sets the address to that of
                        // the host and the port to the one sent back by the server. Sends the packet.
                        DatagramPacket udpPacket = new DatagramPacket(chunkArray, chunkArray.length, InetAddress.getLocalHost(), newUDPPort) ;
                        udpSocket.send(udpPacket) ;

                        // Re-initializes the packet. Receives the packet from the Server (This should contain the ack).
                        // Stores this in a byte array which is then printed to the screen for the User.
                        udpPacket = new DatagramPacket(chunkArray, chunkArray.length) ;
                        udpSocket.receive(udpPacket);
                        byte[] data = udpPacket.getData() ;
                        System.out.println(new String(data));

                        // Exits while loop once the end of the udpFileToSend [] is reached.
                    } while(i < udpFileToSend.length) ;

                    // Creates a byte buffer which we will use to store our eof int. The integer 0
                    // is placed in the buffer. This will be used to store more complex eof indicators in the future.
                    ByteBuffer eofByteBuffer = ByteBuffer.allocate(4);
                    eofByteBuffer.putInt(0) ;

                    // A new eofByte array reads from the byte buffer, stores this in a new datagramPacket to be sent
                    // to the server. The packet is then sent to the server.
                    byte[] eofByte = eofByteBuffer.array() ;
                    DatagramPacket eofPacket = new DatagramPacket(eofByte, eofByte.length, InetAddress.getLocalHost(), newUDPPort) ;
                    udpSocket.send(eofPacket) ;

                    // Closes the UDP socket
                    udpSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            // Closes the UDP socket if a random port was not given by the Server.
            } else {
                udpSocket.close();
            }
        }
    }
}

