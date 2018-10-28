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

public class client {

    public static void main(String args[]) {
        // Primitives to store the initial emulator send to and receive from ports.
        int sendToPort = Integer.parseInt(args[1]);
        int receiveFromPort = Integer.parseInt(args[2]);

        // Creates a string to store the file name the user wants to send to the server.
        // This string is then converted to a file object.
        String clientFileName = args[3];
        File fileToSend = new File(clientFileName);

        // Initializes the UDP socket to null ;
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
                boolean sentEOT = false;

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
                        else{
                            toServer = new packet(3, seqNum, 0, null);
                        }

                        System.out.println("packet seq num" + toServer.getSeqNum());
                        System.out.println(" packet data " + toServer.getData());
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


                        }
                        else if(!sentEOT){
                            udpPacket[seqNum] = new DatagramPacket(toServerArray, toServerArray.length, emulatorName, sendToPort);
                        }


                        //System.out.println(moreData);
                        if (i == udpFileToSend.length && moreData) {
                            //System.out.println("string");
                            moreData = false;
                            oldMax = (seqNum + 1) % 8;
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
                            //emulatorSocketReceive.setSoTimeout(0);


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
                            boolean repeatedACK = false;
                            System.out.println("Ack Seq Num = " + receivePacket.getSeqNum());
                            if (receivePacket.getType() == 0) {

                                System.out.println("move window forward");
                                newBase = (receivePacket.getSeqNum() + 1) % 8;
                                System.out.println(("newbase = " + newBase));


                            } else if (receivePacket.getType() == 2){
                                System.out.println("Received EOTACK");
                                System.out.println("move window forward");
                                newBase = (receivePacket.getSeqNum() + 1) % 8;
                                System.out.println(("newbase = " + newBase));

                            }
                            else{
                                System.out.println("invalid packet type");
                            }
                        } catch (SocketTimeoutException e) {
                            // resend all packets in window
                            System.out.println("socket Timeout");
                            System.out.println("sequence Number " + seqNum);
                            System.out.println("old base " + oldBase);
                            System.out.println("old Max " + oldMax);
                            receivedAck = false;
                            for (int pacNum = oldBase; pacNum != (oldMax + 1) % 8; pacNum = (pacNum + 1) % 8) {
                                System.out.println(" send packet number " + pacNum);
                                emulatorSocketSend.send(udpPacket[pacNum]);
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

                    if (moreData) {
                        seqNum = (seqNum + 1) % 8;
                    }
                    oldBase = newBase;

                    // Exits while loop once the end of the udpFileToSend [] is reached.
                } while (oldBase != oldMax + 1);
                System.out.println("End of Loop");

                
            } catch (IOException e) {
                e.printStackTrace();
            }

            emulatorSocketSend.close();
            emulatorSocketReceive.close();
            // Closes the UDP socket if a random port was not given by the Server.
        } else {
            emulatorSocketSend.close();
            emulatorSocketReceive.close();
        }
    }
}




