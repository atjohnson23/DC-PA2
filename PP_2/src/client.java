/*
Authors: Chandler Musgrove (jcm982) & Adam Johnson (atj113)

Purpose: The purpose of this code is to implement a client socket program which utilizes go-back-in to handle packet
loss.

Functionality: This code implements go-back-in through a cumulative ack. This is done by establishing a window of
which is composed of packets with unique sequence numbers. This window is first filled, meaning each packet is generated
with a unique sequence number from the command line file given to the program and then sent to server. Once the window
is filled, the client waits for an ack packet from the server. The ack does not need to be received in proper order from
0 to 7, rather the window will slide forward with regards to the sequence number received from the server. This means
the window may slide forward by many packets, allowing the client to send multiple packets again to the server in order
to properly fill the window again. If an ack is not received from the server within 2 seconds of the oldest packet being
sent, the socket will timeout, which will the cause the client to resend every packet within its window to the server
and then wait for an ack from the server again, reinitializing the timer to 2 seconds. This process will stop once the
entire contents of the file has been sent. The server then sends an EOT packet to server which tells the server it is
done using the socket and is ready to terminate the connection. The client waits for an EOT ack from the server, closes
the sending and receiving sockets and ends.

Citations:
Programming Assignment #2 PDF
https://docs.oracle.com/javase/7/docs/api/java/io/ByteArrayOutputStream.html
https://stackoverflow.com/questions/17940423/send-object-over-udp-in-java
https://stackoverflow.com/questions/3997459/send-and-receive-serialize-object-on-udp
*/

import java.io.*;
import java.net.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class client {

    public static void main(String args[]) {
        // Primitives to store the initial emulator send to and receive from ports.
        int sendToPort = Integer.parseInt(args[1]);
        int receiveFromPort = Integer.parseInt(args[2]);

        // Creates a string to store the file name the user wants to send to the server.
        // This string is then converted to a file object.
        String clientFileName = args[3];
        File fileToSend = new File(clientFileName);

        // Initializes the UDP socket to null
        // One socket is used for receiving packets while the other is used to send packets.
        DatagramSocket emulatorSocketSend = null;
        DatagramSocket emulatorSocketReceive = null;
        try {
            // Opens the datagram socket (UDP socket) and prints the Random port the datagram packets will be sent.
            emulatorSocketSend = new DatagramSocket();
            emulatorSocketReceive = new DatagramSocket(receiveFromPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        InetAddress emulatorName = null;

        if (args[0].equals("localhost")) {
            try {

                emulatorName = InetAddress.getLocalHost();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Otherwise the socket address is set to that as specified by the User.
        } else if (!args[0].equals("localhost")) {
            try {

                emulatorName = InetAddress.getByName(args[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        DatagramPacket[] udpPacket = new DatagramPacket[8];
        // Enters the If statement if the UDP connection socket has been set.
        if (emulatorSocketSend != null) {
            // Creates a new byte array which will be used to store the contents of the file to be sent.
            // Initializes the file input stream which will be used to read the contents of the file into byte [].
            byte[] udpFileToSend = new byte[(int) fileToSend.length()];
            FileInputStream udpFileStream;
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

            // File stream definitions
            FileOutputStream seqNumLog = null;
            FileOutputStream ackLog = null;
            try {
                seqNumLog = new FileOutputStream("seqnum.log",false);
                ackLog = new FileOutputStream("ack.log",false) ;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            packet toServer;

            try {
                // Initializes integer which will be used to step through the udpFileToSend [].
                int i = 0;
                int seqNum = 0;
                int oldBase = 0;
                int newBase = 0;
                int oldMax = 6;
                int delta;
                boolean moreData = true;
                boolean receivedAck = true;
                //used to determine if EOT packet has been sent
                boolean sentEOT = false;
                //used to determine if EOT ACK has been received. This is the escape case for the primary loop
                boolean receivedEOTAck = false;

                // Do while loop which iterates until i is greater than the udpFileToSend arrays length.
                do {
                    if (receivedAck) {
                        // Initializes a new byte array to store chunks of the file in 4 byte increments to be sent
                        // to the server.
                        byte[] chunkArray = new byte[30];

                        // For loop iterates through the chunkArray until the end of the array is reached.
                        for (int j = 0; j < chunkArray.length; j += 1) {
                            // If statement to only store data in the chunkArray if the udpFileToSend arrays length
                            // has not yet been reached. Prevents us from sending extra garbage bytes to the Server.
                            if (i < udpFileToSend.length) {
                                // Sets the byte of the chunck array equal to that of the udpFileToSend array.
                                // Increments i.
                                chunkArray[j] = udpFileToSend[i];
                                i += 1;
                            }
                        }


                        String packetData = new String(chunkArray, 0, chunkArray.length);

                        if(moreData) {
                            toServer = new packet(1, seqNum, chunkArray.length, packetData);
                        }
                        //if there is no more data to send create EOT packet
                        else{
                            toServer = new packet(3, seqNum, 0, null);
                        }

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ObjectOutputStream packetObjectStream = new ObjectOutputStream(outputStream);
                        packetObjectStream.writeObject(toServer);
                        packetObjectStream.close();
                        byte[] toServerArray;
                        toServerArray = outputStream.toByteArray();


                        // Creates the datagram packet to send to the server, giving it the necessary parameters to set
                        // its length to that of the chunkArray, store the chunkArray and sets the address to that of
                        // the host and the port to the one sent back by the server. Sends the packet.
                        if (moreData) {
                            udpPacket[seqNum] = new DatagramPacket(toServerArray, toServerArray.length, emulatorName, sendToPort);
                            emulatorSocketSend.send(udpPacket[seqNum]);

                            // Write sequence number to log file
                            seqNumLog.write(Integer.toString(toServer.getSeqNum()).getBytes()) ;
                            seqNumLog.write("\n".getBytes()) ;

                        }
                        //if there is no more data and an EOT packet has not been sent send EOT
                        else if(!sentEOT){
                            udpPacket[seqNum] = new DatagramPacket(toServerArray, toServerArray.length, emulatorName, sendToPort);
                            emulatorSocketSend.send(udpPacket[seqNum]);
                            sentEOT = true;
                            oldMax = seqNum;

                            // Write sequence number to log file
                            seqNumLog.write(Integer.toString(toServer.getSeqNum()).getBytes()) ;
                            seqNumLog.write("\n".getBytes()) ;
                        }


                        //System.out.println(moreData);
                        if (i == udpFileToSend.length && moreData) {
                            moreData = false;
                        }
                    }
                    //receive condition
                    if (seqNum == oldMax || (!moreData && sentEOT)) {

                        // Re-initializes the packet. Receives the packet from the Server (This should contain the ack).
                        // Stores this in a byte array which is then printed to the screen for the User.
                        byte[] inputBuffer = new byte[1000];
                        DatagramPacket udpPacketAck = new DatagramPacket(inputBuffer, inputBuffer.length);
                        emulatorSocketReceive.setSoTimeout(2000);
                        try {
                            emulatorSocketReceive.receive(udpPacketAck);

                            //Deserialize data packet received from client
                            packet receivePacket = null;

                            ByteArrayInputStream byteInput = new ByteArrayInputStream(inputBuffer);

                            ObjectInputStream objectInput = new ObjectInputStream(byteInput);
                            objectInput.close();
                            try {
                                receivePacket = (packet) objectInput.readObject();
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }

                            //check if packet is an ack
                            if (receivePacket.getType() == 0) {
                                newBase = (receivePacket.getSeqNum() + 1) % 8;
                                receivedAck = true;

                                // Write sequence number of ack packet
                                ackLog.write(Integer.toString(receivePacket.getSeqNum()).getBytes()) ;
                                ackLog.write("\n".getBytes()) ;


                            } else if (receivePacket.getType() == 2){
                                newBase = (receivePacket.getSeqNum() + 1) % 8;
                                receivedEOTAck = true;

                                // Write sequence number of ack packet
                                ackLog.write(Integer.toString(receivePacket.getSeqNum()).getBytes()) ;
                                ackLog.write("\n".getBytes()) ;

                            }
                            else{
                                System.out.println("invalid packet type");
                            }
                        } catch (SocketTimeoutException e) {
                            // resend all packets in window
                            receivedAck = false;
                            for (int pacNum = oldBase; pacNum != (oldMax + 1) % 8; pacNum = (pacNum + 1) % 8) {
                                emulatorSocketSend.send(udpPacket[pacNum]);

                                // Write sequence number to log file
                                seqNumLog.write(Integer.toString(pacNum).getBytes()) ;
                                seqNumLog.write("\n".getBytes()) ;
                            }

                            if (seqNum == 0) {
                                seqNum = 7;
                            } else {
                                seqNum--;
                            }
                        }

                        if (newBase > oldBase) {
                            delta = newBase - oldBase;

                        } else {
                            delta = newBase - oldBase + 8;
                        }

                        if (moreData) {
                            oldMax = (oldMax + delta) % 8;
                        }
                    }

                    if (!sentEOT) {
                        seqNum = (seqNum + 1) % 8;
                    }
                    oldBase = newBase;

                    // Exits while loop once the end of the udpFileToSend [] is reached.

                }while (!receivedEOTAck);

            } catch (IOException e) {
                e.printStackTrace();
            }

            // Closes the UDP socket if a random port was not given by the Server.
            emulatorSocketSend.close();
            emulatorSocketReceive.close();

        } else {
            // Closes the UDP socket if a random port was not given by the Server.
            emulatorSocketSend.close();
            emulatorSocketReceive.close();
        }
    }
}




