package net.gudenau.proxynx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A simple and stupid stream copier.
 *
 * This is where any bottleneck surely is at the moment.
 * */
class StreamCopier{
    /**
     * The {@link java.io.InputStream InputStream} of the copier.
     * */
    private final InputStream input;
    /**
     * The {@link java.io.OutputStream OutputStream} of the copier.
     * */
    private final OutputStream output;
    
    /**
     * Creates a new StreamCopier.
     *
     * @param input The source of the data
     * @param output The destination of the data
     * */
    StreamCopier(InputStream input, OutputStream output){
        this.input = input;
        this.output = output;
    }
    
    /**
     * Actually do the transfer.
     * */
    void run(){
        try{
            int read;
            byte[] buffer = new byte[1024 * 10];
            while((read = input.read(buffer)) > 0){
                output.write(buffer, 0, read);
                output.flush();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
