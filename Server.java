package psp;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Server class
 * @author Brugui
 * @since 2018-02-14
 */
public class Server {
	// RESPONSE CODES
	public final static boolean DEBUG = true;
	public final static int CREDENTIALS_REQUIRED  = 100;
	public final static int INVALID_CREDENTIALS   = 101;
	public final static int VALID_CREDENTIALS     = 102;
	public final static int READY                 = 200;
	public final static int COMMAND_ERROR         = 201;
	public final static int COMMAND_RESPONSE      = 202;
	public final static int EXIT                  = 220;
	public final static int UNACCEPTED_DISCONNECT = 221;
	public final static int ACCEPTED_DISCONNECT   = 222;
	public final static int FILE_READY            = 300;  //Indicates that the server is going to send a file
	public final static int CLIENT_FILE_READY     = 300;  //Indicates that the client is ready to receive a file
	public final static int FILE_SIZE             = 301;  //Indicates that the server is going to send the size of the file
    public final static int FILE_BUFFER           = 1024; //Max buffer size

	
	public final static int PORT = 9632;
	public final static int DATAGRAM_PORT = 9633;

	
	public static void main(String[] args) {
		//My first try-with-resources :)
		try (InputStream is = new FileInputStream( ClientHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()+"/telnetKeys")) {
			System.out.println(("Opening server on port: " + PORT));
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(is, "123456".toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, "123456".toCharArray());

			//Creates a SSLServersocket
			SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			SSLServerSocket sslServerSocket = (SSLServerSocket) factory.createServerSocket(PORT);
			sslServerSocket.setEnabledCipherSuites(factory.getSupportedCipherSuites());
			while (true) {
				SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
				ClientHandler clientHandler = new ClientHandler(sslSocket);
				clientHandler.start();
			}
			/*ServerSocket serverSocket = new ServerSocket(PORT);
			while (true) {
				Socket socket = serverSocket.accept();
				ClientHandler clientHandler = new ClientHandler(socket);
				clientHandler.start();
			}*/
		} catch (IOException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | CertificateException | URISyntaxException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
