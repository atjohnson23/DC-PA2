/*
Purpose:

Functionality:

Citations:

*/

import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public class server {

    public static void main(String args[]) throws Exception{
        // Primitives used to store the confimation code from the Client and get the initial tcp port from cmd line.

        //host name for emulator
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
        String fileName = args[3];

        DatagramSocket recieveSocket = new DatagramSocket(recieveFromEmulator);
        DatagramSocket sendSocket   = new DatagramSocket() ;


        boolean morePackets = true;

        //
        ByteArrayInputStream byteInput;

        ObjectInputStream objectInput;

        packet receivePacket;

        while (morePackets)
        {
            //byte array to store contents of recieved packets
            byte[] inputBuffer = new byte[1000];
            DatagramPacket udpPacket = new DatagramPacket(inputBuffer, inputBuffer.length);
            recieveSocket.receive(udpPacket);

            //deserialize data packet recieved from client
            byteInput = new ByteArrayInputStream(inputBuffer);

            objectInput = new ObjectInputStream(byteInput);

            receivePacket = (packet) objectInput.readObject();

            objectInput.close();
            packet ackPacket =null;

            //check if packet is data packet
            if(receivePacket.getType() == 1) {
                ackPacket = new packet(0, receivePacket.getSeqNum(), 1, "0");
            }

            //if not data check if it is EOT packet
            else if(receivePacket.getType() == 3) {
                morePackets = false ;
                ackPacket = new packet(2, receivePacket.getSeqNum(), 0, null) ;
            }
            else{
                System.out.println("Invalid packet type");
            }

            // serialize ack packet to send to client
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ;
            ObjectOutputStream packetObjectStream = new ObjectOutputStream(outputStream) ;
            packetObjectStream.writeObject(ackPacket) ;
            packetObjectStream.close();
            byte[] toClientArray ;
            toClientArray = outputStream.toByteArray() ;

            System.out.println(receivePacket.getData());

            //send serialized ack packet to client
            udpPacket = new DatagramPacket(toClientArray, toClientArray.length, emulatorName, sendToEmulator) ;
            sendSocket.send(udpPacket);

            System.out.println(receivePacket.getSeqNum());
            System.out.println(receivePacket.getType());
        }
        recieveSocket.close();
        sendSocket.close();
    }
}


