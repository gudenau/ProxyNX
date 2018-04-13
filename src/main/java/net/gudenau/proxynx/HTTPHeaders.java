package net.gudenau.proxynx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * A very simple and somewhat flawed HTTP header implementation.
 * */
class HTTPHeaders{
    /**
     * The found request proprieties.
     * */
    private final Map<String, String> proprieties = new HashMap<>();
    /**
     * The HTTP request method, such as GET.
     * */
    private String httpRequest;
    /**
     * The "target" of the request, the actual URL.
     * */
    private String httpTarget;
    /**
     * The HTTP version, such as HTTP/1.1
     * */
    private String httpVersion;
    
    /**
     * Reads the HTTP request from the {@link java.io.InputStream InputStream}.
     *
     * @param inputStream The stream to read
     *
     * @throws java.io.IOException If there was an error during IO
     * */
    void read(InputStream inputStream) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        if(line == null){
            throw new IOException("Unexpected EOS");
        }
        
        String[] split = line.split(" ");
        httpRequest = split[0];
        httpTarget = split[1];
        httpVersion = split[2];
        
        while((line = reader.readLine()) != null && !line.isEmpty()){
            split = line.split(": ", 2);
            proprieties.put(split[0], split[1]);
        }
        if(line == null){
            throw new IOException("Unexpected EOS");
        }
    }
    
    /**
     * Writes the header to the {@link java.io.OutputStream OutputStream}.
     *
     * @param outputStream The stream to write to
     *
     * @throws If there was an error during IO
     * */
    void write(OutputStream outputStream) throws IOException{
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        writer.write(httpRequest);
        writer.write(' ');
        writer.write(httpTarget);
        writer.write(' ');
        writer.write(httpVersion);
        writer.write("\r\n");
        for(Map.Entry<String, String> entry : proprieties.entrySet()){
            writer.write(entry.getKey());
            writer.write(": ");
            writer.write(entry.getValue());
            writer.write("\r\n");
        }
        writer.write("\r\n");
        writer.flush();
    }
    
    /**
     * Gets the request method.
     *
     * @return The HTTP request method
     * */
    String getHttpRequest(){
        return httpRequest;
    }
    
    /**
     * Gets the "target" of the request. Should be a full URL.
     *
     * @return The "target" of the request
     * */
    String getHttpTarget(){
        return httpTarget;
    }
    
    /**
     * Gets the HTTP version string.
     *
     * @return The HTTP version string
     * */
    String getHttpVersion(){
        return httpVersion;
    }
    
    /**
     * Gets a propriety from the header.
     *
     * @param name The name of the propriety
     *
     * @return The propriety or null if not found
     * */
    String getPropriety(String name){
        return proprieties.get(name);
    }
}
