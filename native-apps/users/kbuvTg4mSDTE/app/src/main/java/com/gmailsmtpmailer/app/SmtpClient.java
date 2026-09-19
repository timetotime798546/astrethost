package com.gmailsmtpmailer.app;

import android.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SmtpClient {

    public interface SmtpListener {
        void onLog(String message);
        void onSuccess();
        void onFailure(String error);
    }

    public static void sendEmail(final String host, final int port, final boolean useSsl,
                                 final String username, final String password,
                                 final String to, final String subject, final String body,
                                 final SmtpListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                BufferedReader reader = null;
                PrintWriter writer = null;
                try {
                    listener.onLog("[CONNECTING] Initiating connection to " + host + ":" + port + "...");
                    if (useSsl) {
                        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                        SSLSocket sslSocket = (SSLSocket) factory.createSocket(host, port);
                        sslSocket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
                        socket = sslSocket;
                    } else {
                        socket = new Socket(host, port);
                    }
                    
                    socket.setSoTimeout(15000); // 15 seconds network timeout

                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                    // 1. Read greeting
                    String line = readLine(reader, listener);
                    if (!line.startsWith("220")) {
                        throw new Exception("Unexpected greeting response: " + line);
                    }

                    // 2. Send EHLO
                    sendCommand(writer, "EHLO localhost", listener);
                    while (true) {
                        line = readLine(reader, listener);
                        if (line.startsWith("250 ")) {
                            break;
                        } else if (!line.startsWith("250-")) {
                            throw new Exception("Error in EHLO response: " + line);
                        }
                    }

                    // 3. Send AUTH LOGIN
                    sendCommand(writer, "AUTH LOGIN", listener);
                    line = readLine(reader, listener);
                    if (!line.startsWith("334")) {
                        throw new Exception("Auth Login not accepted or rejected: " + line);
                    }

                    // 4. Send username in Base64
                    String b64User = Base64.encodeToString(username.getBytes("UTF-8"), Base64.NO_WRAP);
                    sendCommand(writer, b64User, listener, "[Username Base64]");
                    line = readLine(reader, listener);
                    if (!line.startsWith("334")) {
                        throw new Exception("Username rejected: " + line);
                    }

                    // 5. Send password in Base64
                    String b64Pass = Base64.encodeToString(password.getBytes("UTF-8"), Base64.NO_WRAP);
                    sendCommand(writer, b64Pass, listener, "[Password Base64]");
                    line = readLine(reader, listener);
                    if (!line.startsWith("235")) {
                        throw new Exception("Authentication failed: " + line + "\nNote: Ensure you configure a 16-character Google App Password in your account settings.");
                    }
                    listener.onLog("[AUTH] Connection authorized!");

                    // 6. Send MAIL FROM
                    sendCommand(writer, "MAIL FROM:<" + username + ">", listener);
                    line = readLine(reader, listener);
                    if (!line.startsWith("250")) {
                        throw new Exception("MAIL FROM rejected: " + line);
                    }

                    // 7. Send RCPT TO
                    sendCommand(writer, "RCPT TO:<" + to + ">", listener);
                    line = readLine(reader, listener);
                    if (!line.startsWith("250")) {
                        throw new Exception("Recipient target rejected: " + line);
                    }

                    // 8. Send DATA
                    sendCommand(writer, "DATA", listener);
                    line = readLine(reader, listener);
                    if (!line.startsWith("354")) {
                        throw new Exception("DATA rejected: " + line);
                    }

                    // 9. Send headers and message body
                    listener.onLog("[SENDING] Uploading mail text envelope...");
                    writer.print("From: " + username + "\r\n");
                    writer.print("To: " + to + "\r\n");
                    writer.print("Subject: " + subject + "\r\n");
                    writer.print("Content-Type: text/plain; charset=UTF-8\r\n");
                    writer.print("\r\n");
                    writer.print(body + "\r\n");
                    writer.print(".\r\n");
                    writer.flush();

                    line = readLine(reader, listener);
                    if (!line.startsWith("250")) {
                        throw new Exception("End-of-data code mismatch: " + line);
                    }

                    listener.onLog("[DISPATCH] Received successful hand-off confirm!");

                    // 10. QUIT
                    sendCommand(writer, "QUIT", listener);
                    readLine(reader, listener);

                    listener.onSuccess();

                } catch (Exception e) {
                    listener.onFailure(e.getMessage() != null ? e.getMessage() : e.toString());
                } finally {
                    try { if (reader != null) reader.close(); } catch (Exception e) {}
                    try { if (writer != null) writer.close(); } catch (Exception e) {}
                    try { if (socket != null) socket.close(); } catch (Exception e) {}
                }
            }

            private void sendCommand(PrintWriter writer, String command, SmtpListener lst) {
                sendCommand(writer, command, lst, command);
            }

            private void sendCommand(PrintWriter writer, String command, SmtpListener lst, String logRepresentation) {
                writer.print(command + "\r\n");
                writer.flush();
                lst.onLog("> " + logRepresentation);
            }

            private String readLine(BufferedReader reader, SmtpListener lst) throws Exception {
                String line = reader.readLine();
                if (line == null) {
                    throw new Exception("Connection interrupted by server.");
                }
                lst.onLog("< " + line);
                return line;
            }
        }).start();
    }
}