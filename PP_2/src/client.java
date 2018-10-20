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
import java.io.Serializable;


public class client {

    public static void main(String args[]) {
        // Primitives to store the initial emulator send to and receive from ports.
        int sendToPort = Integer.parseInt(args[1]);
        int receiveFromPort = Integer.parseInt(args[2]);

        // Creates a string to store the file name the user wants to send to the server.
        // This string is then converted to a file object.
        String clientFileName = args[3] ;
        File fileToSend = new File(clientFileName) ;

        // Initializes the UDP socket to null ;
        DatagramSocket emulatorSocket = null;
        try {
            // Opens the datagram socket (UDP socket) and prints the Random port the datagram packets will be sent.
            emulatorSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        InetAddress emulatorName = null ;


        if (args[0].equals("localhost")) {
            try {

                emulatorName = InetAddress.getLocalHost() ;
            } catch (IOException e) {
                e.printStackTrace();
            }
        // Otherwise the socket address is set to that as specified by the User.
        } else if (!args[0].equals("localhost")) {
            try {

                emulatorName = InetAddress.getByName(args[0]) ;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Enters the If statement if the UDP connection socket has been set.
        if (emulatorSocket != null) {
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
                    byte[] chunkArray = new byte[30];

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

                    packet toServer = new packet(1, 0, chunkArray.length, chunkArray.toString()) ;

                    ByteArrayOutputStream toByteArray = new ByteArrayOutputStream() ;
                    ObjectOutputStream packetObjectStream = new ObjectOutputStream(toByteArray) ;
                    packetObjectStream.writeObject(toServer) ;
                    byte[] toServerArray;
                    toServerArray = toByteArray.toByteArray() ;


                    // Creates the datagram packet to send to the server, giving it the necessary parameters to set
                    // its length to that of the chunkArray, store the chunkArray and sets the address to that of
                    // the host and the port to the one sent back by the server. Sends the packet.
                    DatagramPacket udpPacket = new DatagramPacket(toServerArray, toServerArray.length, emulatorName, sendToPort) ;
                    emulatorSocket.send(udpPacket) ;

                    // Re-initializes the packet. Receives the packet from the Server (This should contain the ack).
                    // Stores this in a byte array which is then printed to the screen for the User.
                    udpPacket = new DatagramPacket(chunkArray, chunkArray.length) ;
                    emulatorSocket.receive(udpPacket);
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
                DatagramPacket eofPacket = new DatagramPacket(eofByte, eofByte.length, InetAddress.getLocalHost(), sendToPort) ;
                emulatorSocket.send(eofPacket) ;

                // Closes the UDP socket
                emulatorSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Closes the UDP socket if a random port was not given by the Server.
        } else {
            emulatorSocket.close();
        }
    }
}




