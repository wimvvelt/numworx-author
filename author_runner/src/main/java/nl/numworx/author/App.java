package nl.numworx.author;

import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;

import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
    	String target = App.class.getName();
		try {
	          JUnique.acquireLock(target, App::handleMessage);
	        } catch (AlreadyLockedException e1) {
	          JUnique.sendMessage(target, Arrays.toString(args));
	          System.exit(1);
	    }
    	
    	JLabel label = new JLabel( "Hello World v2!" );
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JFrame frame = new JFrame("main");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.pack();
        
        frame.setVisible(true);
    }
    
	private static String handleMessage(String message) {
		System.err.println("From application: " + message);
		return message;
	}
	
}
