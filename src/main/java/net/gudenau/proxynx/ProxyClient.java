package net.gudenau.proxynx;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;

/**
 * Where most of the work is actually done.
 *
 * Handles the nx requests as well as the proxy connections.
 * */
class ProxyClient{
    /**
     * The {@link java.net.Socket Socket} that is connected to the Switch.
     * */
    private final Socket socket;
    /**
     * The {@link java.io.InputStream InputStream} that is connected to the Switch.
     * */
    private final InputStream inputStream;
    /**
     * The {@link java.io.OutputStream OutputStream} that is connected to the Switch.
     * */
    private final OutputStream outputStream;
    
    /**
     * Creates a new ProxyClient from the provided socket.
     *
     * @param socket The socket that is connected to the Switch
     *
     * @throws java.io.IOException If there was an error getting
     *          the streams from the socket
     * */
    ProxyClient(Socket socket) throws IOException{
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }
    
    /**
     * Handle the connection, should be done on it's own thread.
     * */
    void run(){
        // Make sure the resources get freed up.
        try(socket; inputStream; outputStream){
            // Read in the HTTP header
            HTTPHeaders headers = new HTTPHeaders();
            headers.read(inputStream);
            
            // Extract the host and the port number
            String host = headers.getPropriety("Host");
            
            // Default HTTP port is 80
            int port = 80;
            // Change if specified
            if(host.contains(":")){
                String[] split = host.split(":");
                host = split[0];
                port = Integer.parseInt(split[1]);
            }
    
            // Is it a file we intercept?
            if("ctest.cdn.nintendo.net".equals(host) || host.endsWith(".nx")){
                serveFile(headers);
            }else{
                forward(host, port, headers);
            }
        }catch(Throwable e){
            e.printStackTrace();
        }
    }
    
    /**
     * Serves an intercepted URL.
     *
     * @param headers The HTTP request
     * */
    private void serveFile(HTTPHeaders headers) throws IOException{
        // Figure out what the path is on the server
        String target = headers.getHttpTarget();
        String path = new URL(target).getPath();
        
        // Create the file from that
        File file = new File(path.equals("/") ? "index.html" : path.substring(1));
    
        // It is important to not close this so that the underlying stream is still
        //  usable
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        
        // Throw out a 404 if it does not exist
        if(!file.exists()){
            writer.write("HTTP/1.1 404 Not Found\r\n");
            writer.write("Content-Length: 0\r\n");
            writer.write("Cache-Control: max-age=0, no-cache, no-store\r\n");
            writer.write("Pragma: no-cache\r\n");
            writer.write("Connection: close\r\n");
            writer.write("Content-Type: text/html; charset=utf-8\r\n");
            writer.write("\r\n");
            // We are done here
            return;
        }else{
            writer.write("HTTP/1.1 200 OK\r\n");
            // Send out the correct document size
            writer.write("Content-Length: " + file.length() + "\r\n");
            writer.write("Cache-Control: max-age=0, no-cache, no-store\r\n");
            writer.write("Pragma: no-cache\r\n");
            writer.write("Connection: close\r\n");
            writer.write("Content-Type: text/html; charset=utf-8\r\n");
            writer.write("\r\n");
            writer.flush();
        }
        
        // Read the document and send it to the Switch
        try(InputStream inputStream = new FileInputStream(file)){
            inputStream.transferTo(outputStream);
        }
        outputStream.flush();
    }
    
    /**
     * Forward the request to the intended server
     *
     * @param host The host that was requested
     * @param port The port of the host
     * @param headers The HTTP request
     * */
    private void forward(String host, int port, HTTPHeaders headers) throws IOException{
        // Connect to the remote system
        try(Socket serverSocket = new Socket(host, port);
            InputStream serverInput = serverSocket.getInputStream();
            OutputStream serverOutput = serverSocket.getOutputStream()){
    
            // Is it a CONNECT request?
            if("CONNECT".equals(headers.getHttpRequest())){
                // Handle that, pretend the connection was okay
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                writer.write(headers.getHttpVersion());
                writer.write(" 200 OK\r\n\r\n");
                writer.flush();
            }else{
                // Write the original headers
                headers.write(serverOutput);
            }
    
            // Transfer the rest of the data in a (hopefully) transparent manor
            Thread thread1 = new Thread(new StreamCopier(inputStream, serverOutput)::run);
            Thread thread2 = new Thread(new StreamCopier(serverInput, outputStream)::run);
    
            thread1.start();
            thread2.start();
    
            // Wait for the transfers to finish before closing the streams and socket
            try{
                thread1.join();
                thread2.join();
            }catch(InterruptedException ignored){}
        }
    }
}
