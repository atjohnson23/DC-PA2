/*
Purpose:

Functionality:

Citations:

*/

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public class Server {

    public static void main(String args[]) throws IOException {
        // Primitives used to store the confimation code from the Client and get the initial tcp port from cmd line.
        int tcpPort = Integer.parseInt(args[0]);
        int confCode;

        // Primitive used to store the randomly selected port for UDP communication
        int randomUDPPort = ThreadLocalRandom.current().nextInt(1024, 65535 + 1);

        // Opens the initial TCP socket.
        try (ServerSocket tcpSocket = new ServerSocket(tcpPort)) {
            Socket tcpConnectionSocket = tcpSocket.accept();

            // Initializes the data input stream which the client has stored the confirmation code in.
            // Stores this code in the primitive confCode.
            DataInputStream tcpInputStream = new DataInputStream(tcpConnectionSocket.getInputStream());
            confCode = tcpInputStream.readInt();

            // Validates confirmation code, and, if correct, stores the random port for UDP communication in the
            // output stream. Closes the TCP socket connection.
            if (confCode == 259) {
                DataOutputStream out = new DataOutputStream(tcpConnectionSocket.getOutputStream());
                out.writeInt(randomUDPPort);
                tcpConnectionSocket.close();
            }

            // Opens a new UDP socket. Creates a byte array to store data packets sent from the client.
            // A 4 byte chunck was specified to be used for this project.
            DatagramSocket udpConnectionSocket = new DatagramSocket(randomUDPPort);
            byte[] datagramBuffer = new byte[4];
            DatagramPacket ackPacket = new DatagramPacket(datagramBuffer, datagramBuffer.length);

            // Prints to the User that negotiation was accepted and displays the port to be used.
            System.out.println("\nNegotiation detected. Selected the following random port " + randomUDPPort + "\n");

            // Integer used to detect the end of the file sent from the client.
            int eofInt = 1;

            // While loop interates until the end of file integer is detected.
            while(eofInt != 0)
            {
                /* Receives the file packet chunks from the client and stores them in a byte array. The receive method
                will block until the packet is received from the client. The port of this packet is then store in
                pPort along with the address stored in pAdress. */
                udpConnectionSocket.receive(ackPacket);
                byte[] udpFileData = ackPacket.getData() ;
                InetAddress pAddress = ackPacket.getAddress() ;
                int pPort = ackPacket.getPort() ;

                // Stores the packet data in a string to capitalize its contents. Creates a new byte array with this
                // data.
                String makeUpper = new String(udpFileData) ;
                makeUpper = makeUpper.toUpperCase() ;
                byte[] ackData = makeUpper.getBytes() ;

                // Stores the ackData byte array in the datagram packet to be sent back to the client.
                // Sends the packet to the Client.
                ackPacket = new DatagramPacket(ackData, ackData.length, pAddress, pPort) ;
                udpConnectionSocket.send(ackPacket);

                // Creates a new file output stream to write the contents of the original datagram packet to a file.
                // Writes the data received from the first packet to a file on the Server titles output.txt.
                FileOutputStream udpOutputSteam = new FileOutputStream("output.txt",true);
                udpOutputSteam.write(udpFileData) ;

                // Gets any integer stored in the udpFileData packet. This integer will be zero once the end of the
                // file is reached.
                ByteBuffer eofBuffer = ByteBuffer.wrap(udpFileData) ;
                eofInt = eofBuffer.getInt() ;
            }

            // Closes the UDP Socket
            udpConnectionSocket.close() ;
            }
        }
    }


