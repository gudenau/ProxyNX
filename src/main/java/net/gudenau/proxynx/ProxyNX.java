package net.gudenau.proxynx;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The entry point of the program
 * */
public class ProxyNX{
    public static void main(String[] arguments){
        // Get a list of all the IP addresses on the system
        List<String> addresses = new LinkedList<>();
        try{
            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            interfaceEnumeration.asIterator().forEachRemaining(
                networkInterface -> networkInterface.getInetAddresses().asIterator().forEachRemaining(
                    inetAddress -> addresses.add(inetAddress.getHostAddress())
                )
            );
        }catch(SocketException e){
            addresses.add("Failed to get addresses");
        }
        // Sort because why not?
        addresses.sort(String::compareTo);
    
        // Create the simple UI
        SwingUtilities.invokeLater(()->{
            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Machine IPs"));
            panel.add(new JLabel("(Often the correct on will be 192.168.X.X)"));
            addresses.forEach(address->panel.add(new JLabel(address)));
            
            JFrame frame = new JFrame("ProxyNX");
            frame.add(panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
    
            // Center it
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setLocation(
                (screen.width - frame.getWidth()) >> 1,
                (screen.height - frame.getHeight()) >> 1
            );
            
            frame.setVisible(true);
        });
        
        // Create the server
        try(ServerSocket serverSocket = new ServerSocket(3128)){
            while(!serverSocket.isClosed()){
                Socket clientSocket = serverSocket.accept();
                try{
                    Thread client = new Thread(new ProxyClient(clientSocket)::run);
                    client.start();
                // Just swallow because crap happens
                }catch(IOException ignored){}
            }
        }catch(IOException e){
            e.printStackTrace();
            System.exit(0);
        }
    }
}
