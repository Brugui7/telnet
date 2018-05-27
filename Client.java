package psp;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

/**
 * @author Brugui
 * @since 2018-09-18
 */
public class Client {

	private static void debug(String message){
		if (Server.DEBUG) System.out.println(message);
	}

    /** Receives a file from the server
     *
     * @param filename File-Name
     * @param size Total size of the file
     * @param dos outputStream opened with the server
     * @throws IOException IOException
     */
	private static void receiveFile(String filename, int size, DataOutputStream dos) throws IOException {
	    int totalSize = size;
        DatagramSocket ds = new DatagramSocket(Server.DATAGRAM_PORT);
        dos.writeInt(Server.CLIENT_FILE_READY);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filename));
        for (int i = 0; i < totalSize; i++) {
            byte[] buffer = new byte[Server.FILE_BUFFER];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            ds.receive(dp);
            buffer = dp.getData();
            bos.write(buffer);
            totalSize -= 1024;
        }
        bos.close();
    }

	public static void main(String[] args) throws UnknownHostException, IOException {
	    int lastStatus; //Last status received from the server
	    Scanner scanner  = new Scanner(System.in);
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket("localhost",Server.PORT);
        socket.setEnabledCipherSuites(factory.getSupportedCipherSuites());

        debug("Conectando");
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream  dis = new DataInputStream(socket.getInputStream());
        //                                              ###########################
        //                                              ########## LOGIN ##########
        //                                              ###########################
        if (dis.readInt() == Server.CREDENTIALS_REQUIRED){
            debug("Se requieren credenciales");
            boolean failed = false;
            do {
                System.out.println(failed ? "Usuario o contraseña incorrectos" : "Se requieren usuario y contraseña");
                System.out.print("Usuario:\n> ");
                dos.writeUTF(scanner.next());
                scanner.nextLine();
                System.out.print("Contraseña:\n> ");
                dos.writeUTF(scanner.next());
                scanner.nextLine();
                lastStatus = dis.readInt();
                failed = lastStatus == Server.INVALID_CREDENTIALS;
            } while (lastStatus != Server.VALID_CREDENTIALS);
            System.out.println("Credenciales válidas, entrando...");
            //                                              ############################
            //                                              ########### CORE ###########
            //                                              ############################
            while (true){
                lastStatus = dis.readInt();
                if (lastStatus == Server.READY){
                    System.out.print("> ");
                    dos.writeUTF(scanner.nextLine());
                    lastStatus = dis.readInt();
                    if (lastStatus == Server.FILE_SIZE){
                        int fileSize = dis.readInt();
                        lastStatus = dis.readInt();
                        if (lastStatus == Server.FILE_READY){
                            String filename = dis.readUTF();
                            receiveFile(filename, fileSize, dos);
                            System.out.println("Fichero recibido");
                        }
                    } else if(lastStatus == Server.EXIT){
                        lastStatus = dis.readInt();
                        if (lastStatus == Server.ACCEPTED_DISCONNECT){
                            break;
                        }
                    }
                    System.out.println(dis.readUTF());

                }
            }
            System.out.println("Saliendo...");
        }

	}
}
