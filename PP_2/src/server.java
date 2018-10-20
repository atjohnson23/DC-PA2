/*
Purpose:

Functionality:

Citations:

*/

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public class server {

    public static void main(String args[]) throws Exception{
        // Primitives used to store the confimation code from the Client and get the initial tcp port from cmd line.

        //host name for emulator
        String emulatorName = args[0];

        // UDP port number used by the server to receive data from the emulator
        int recieveFromEmulator = Integer.parseInt(args[1]);

        // UDP port number used by the emulator to receive ACKs from the server
        int sendToEmulator = Integer.parseInt(args[2]);

        // name of the file into which the received data is written
        String fileName = args[3];

        DatagramSocket recieveSocket = new DatagramSocket();


        boolean morePackets = true;

        //byte array to store contents of recieved packets
        byte[] inputBuffer = new byte[42];

        //
        ByteArrayInputStream byteInput;

        ObjectInputStream objectInput;

        DatagramPacket dgPacket = new DatagramPacket(inputBuffer, inputBuffer.length);

        packet receivePacket;

        while (morePackets)
        {
            recieveSocket.receive(dgPacket);

            byteInput = new ByteArrayInputStream(inputBuffer);

            objectInput = new ObjectInputStream(byteInput);

            receivePacket = (packet) objectInput.readObject();

            System.out.println(receivePacket.getData());







        }
    }
}


