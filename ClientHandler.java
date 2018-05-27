package psp;

import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocket;

/**
 * Thread to manage multiple client connections
 * @author Brugui
 * @since 2018-02-14
 */
public class ClientHandler extends Thread {
	
	private final static HashMap<String, String> credentials = new HashMap<>();
	private SSLSocket socket = null;
	private PrintWriter writer = null;
	private String user, password;


	
	/*public ClientHandler(Socket socket) {
		credentials.put("admin", "admin");
		credentials.put("test", "1234");
		this.socket = socket;
	}*/

    public ClientHandler(SSLSocket socket) {
        credentials.put("admin", "admin");
        credentials.put("test", "1234");
        this.socket = socket;
    }

    /**
     * Sends a mail with the connection info
     * @param user User
     * @param socket Socket
     * @throws IOException IOException
     */
	private static void sendMail(String user, Socket socket) throws IOException{
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

		Session session = Session.getInstance(props,new javax.mail.Authenticator() {	
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("alumnoselcampico@gmail.com", "1dam2dam");
			}
		});
		
		try {
			Message message = new MimeMessage(session);
			message.setRecipients(Message.RecipientType.TO,	InternetAddress.parse("thesigmarilion@hotmail.com"));
			message.setSubject("Se ha conectado el usuario "+user);
			message.setText("Se ha conectado a las "+dateFormat.format(date)+" el usuario "+user+" con la ip "+socket.getInetAddress());
			Transport.send(message);
		} catch (MessagingException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}


	private void sendFile(File file, int totalSize) throws IOException {
        DatagramSocket ds = new DatagramSocket();
        byte[] data = new byte[Server.FILE_BUFFER];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

        //Sends the file in pieces
        for (int i = 0; i < totalSize; i++) {
            bis.read(data, 0, data.length); //Reads part of file into byte array
            DatagramPacket dp = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), Server.DATAGRAM_PORT);
            ds.send(dp);
            totalSize -= data.length; //Reduces total size
        }
    }
	
	@Override
	public void run() {
		try {
            StringTokenizer commandData;
            String          command;
		    String          lastCommand;
		    Process         process;
            InputStream     processInputStream, processErrorInputStream;
            BufferedReader  processBufferedReader;
            StringBuilder   response;
            //Where this class is running
            String          path = ClientHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            path = path.substring(0, path.lastIndexOf("/"));
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			DataInputStream dis  = new DataInputStream(socket.getInputStream());
			//                                              ###########################
			//                                              ########## LOGIN ##########
            //                                              ###########################
            dos.writeInt(Server.CREDENTIALS_REQUIRED);
            boolean logged = false;
            do{
                user = dis.readUTF();
                password = dis.readUTF();
                if (credentials.containsKey(user)){
                    logged = credentials.get(user).equals(password);
                }
                if (!logged){
                    dos.writeInt(Server.INVALID_CREDENTIALS);
                }
            } while(!logged);
            dos.writeInt(Server.VALID_CREDENTIALS);
            sendMail(user, socket); //Sends the mail with the user logged
            //                                              ############################
            //                                              ########### CORE ###########
            //                                              ############################
            while (true){
                dos.writeInt(Server.READY);
                lastCommand = dis.readUTF();

                commandData = new StringTokenizer(lastCommand);
                command = commandData.nextToken();//The first word is the command itself
                //Checks for internal commands
                if (command.equals("receive")){
                    if (commandData.hasMoreTokens()){
                        String filename = commandData.nextToken();
                        File file = new File(filename);
                        if (file.exists()){
                            int totalSize = (int) file.length();
                            dos.writeInt(Server.FILE_SIZE);
                            dos.writeInt(totalSize);
                            dos.writeInt(Server.FILE_READY);
                            dos.writeUTF(filename);
                            if (dis.readInt() == Server.CLIENT_FILE_READY){
                                sendFile(file, totalSize);
                                dos.writeUTF("Archivo enviado");
                            }
                        } else{
                            dos.writeInt(Server.COMMAND_ERROR);
                            dos.writeUTF("No se ha encontrado el fichero: "+filename);
                        }
                    }
                } else if (command.equals("cd")){
                    if(lastCommand.equals("cd ..")) {
                        if (new File("../").exists() && !path.equals("/")) {
                            if (path.lastIndexOf("/") == 0) {
                                path = "/";
                            } else {
                                path = path.substring(0, path.lastIndexOf("/"));
                            }
                            dos.writeInt(Server.COMMAND_RESPONSE);
                            dos.writeUTF(path);
                        } else {
                            dos.writeInt(Server.COMMAND_ERROR);
                        }
                    } else {
                        if (commandData.hasMoreTokens()) {
                            String newPath = commandData.nextToken();
                            if (!newPath.equals("/")){
                                if(new File(newPath).exists() ){
                                    path = newPath;
                                    dos.writeInt(Server.COMMAND_RESPONSE);
                                    dos.writeUTF(path);
                                } else if (new File(path + "/" + newPath).exists() ){
                                    path = path +"/"+newPath;
                                    dos.writeInt(Server.COMMAND_RESPONSE);
                                    dos.writeUTF(path);
                                } else {
                                    dos.writeInt(Server.COMMAND_ERROR);
                                    dos.writeUTF("Ruta especificada inválida");
                                }
                            } else{
                                dos.writeInt(Server.COMMAND_ERROR);
                                dos.writeUTF("Ruta especificada inválida");
                            }
                        } else {
                            dos.writeInt(Server.COMMAND_ERROR);
                            dos.writeUTF("Argumentos inválidos");
                        }

                    }
                } else if(lastCommand.equals("exit")){
                    dos.writeInt(Server.EXIT);
                    dos.writeInt(Server.ACCEPTED_DISCONNECT);
                    break;
                } else {
                    try {
                        process = Runtime.getRuntime().exec(lastCommand, null, new File(path));
                        processInputStream = process.getInputStream();
                        processErrorInputStream = process.getErrorStream();
                        //Checks if the commands was or not success
                        if (process.waitFor() != 0){
                            dos.writeInt(Server.COMMAND_ERROR);
                            processBufferedReader = new BufferedReader(new InputStreamReader(processErrorInputStream));
                        } else {
                            dos.writeInt(Server.COMMAND_RESPONSE);
                            processBufferedReader = new BufferedReader(new InputStreamReader(processInputStream));
                        }
                        String line;
                        response = new StringBuilder();
                        while ((line = processBufferedReader.readLine()) != null){
                            response.append(line);
                            response.append("\n");
                        }
                        dos.writeUTF(response.toString());
                    } catch (IOException e) {
                        dos.writeInt(Server.COMMAND_ERROR);
                        dos.writeUTF("El comando especificado no existe");
                    }
                }
            }
		} catch (IOException | InterruptedException | URISyntaxException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
    }
}
