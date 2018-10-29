/*
Authors: Chandler Musgrove (jcm982) & Adam Johnson (atj113)

Purpose: The purpose of this code is to implement a server socket program which utilizes go-back-in to handle packet
loss.

Functionality: This code implements go-back-in using a cumulative ack. The server is tasked with sending an ack for
each packet it receives from the client as long as the packet is labeled with the sequence number it is expecting.
Duplicate packets (packets sharing the same sequence number) or out of sequence packets are discarded. Once the server
receives an EOT packet from the client, it returns an EOT ack.

Citations:
Programming Assignment #2 PDF
https://docs.oracle.com/javase/7/docs/api/java/io/ByteArrayOutputStream.html
https://stackoverflow.com/questions/17940423/send-object-over-udp-in-java
https://stackoverflow.com/questions/3997459/send-and-receive-serialize-object-on-udp
*/

import java.io.*;
import java.net.*;

public class server {

    public static void main(String args[]) throws Exception {
        // Primitives used to store the confimation code from the Client and get the initial tcp port from cmd line.

        //host name for emulator
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

        // UDP port number used by the server to receive data from the emulator
        int recieveFromEmulator = Integer.parseInt(args[1]);

        // UDP port number used by the emulator to receive ACKs from the server
        int sendToEmulator = Integer.parseInt(args[2]);

        // name of the file into which the received data is written

        DatagramSocket receiveSocket = new DatagramSocket(recieveFromEmulator);
        DatagramSocket sendSocket = new DatagramSocket();


        boolean morePackets = true;

        //byte array and object stream initialization
        ByteArrayInputStream byteInput;
        ObjectInputStream objectInput;

        //packet object definition
        packet receivePacket;

        //int and boolean definitions
        int expectedSeqNum = 0;
        boolean firstPacketReceived = false;
        boolean gotPacket;

        // File stream definitions
        FileOutputStream udpOutputSteam = new FileOutputStream(args[3], false);
        FileOutputStream arrivalLog = new FileOutputStream("arrival.log", false);

        while (morePackets) {
            gotPacket = false;
            //byte array to store contents of received packets
            byte[] inputBuffer = new byte[1000];
            DatagramPacket udpPacket = new DatagramPacket(inputBuffer, inputBuffer.length);
            receiveSocket.receive(udpPacket);

            //deserialize data packet recieved from client
            byteInput = new ByteArrayInputStream(inputBuffer);

            objectInput = new ObjectInputStream(byteInput);

            receivePacket = (packet) objectInput.readObject();

            objectInput.close();
            packet ackPacket = null;

            //check if packet is data packet
            arrivalLog.write(Integer.toString(receivePacket.getSeqNum()).getBytes());
            arrivalLog.write("\n".getBytes());
            if (receivePacket.getSeqNum() == expectedSeqNum) {

                if (receivePacket.getType() == 1) {
                    ackPacket = new packet(0, receivePacket.getSeqNum(), 1, "0");
                    expectedSeqNum = (expectedSeqNum + 1) % 8;
                    firstPacketReceived = true;
                    udpOutputSteam.write(receivePacket.getData().getBytes());
                    gotPacket = true;
                }

                //if not data check if it is EOT packet

                else if (receivePacket.getType() == 3) {
                    morePackets = false;
                    ackPacket = new packet(2, receivePacket.getSeqNum(), 0, null);
                    gotPacket = true;
                } else {
                    System.out.println("Invalid packet type");
                }
            }
            if (firstPacketReceived && gotPacket) {

                // serialize ack packet to send to client
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ObjectOutputStream packetObjectStream = new ObjectOutputStream(outputStream);
                packetObjectStream.writeObject(ackPacket);
                packetObjectStream.close();
                byte[] toClientArray;
                toClientArray = outputStream.toByteArray();


                //send serialized ack packet to client
                udpPacket = new DatagramPacket(toClientArray, toClientArray.length, emulatorName, sendToEmulator);
                sendSocket.send(udpPacket);
            }
        }

        // Close the communication sockets
        receiveSocket.close();
        sendSocket.close();
    }
}


