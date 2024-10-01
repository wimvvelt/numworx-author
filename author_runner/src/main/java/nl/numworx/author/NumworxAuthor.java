package nl.numworx.author;

import java.io.File;

import javax.swing.JOptionPane;

public class NumworxAuthor {

	private static final String NUMWORX_DWO = "numworx.dwo";

	static void m(String m) {
		JOptionPane.showMessageDialog(null, m);
	}
	
	public static void main(String[] args) throws Exception {
//		m(System.getProperty("app.path", "app.path null"));
//		m(System.getProperty("user.dir", "user.dir null"));
// dit zou best de home directory kunnen zijn		
	    if (args.length == 0 && new File(NUMWORX_DWO).canRead()) {
	        args = new String[] { NUMWORX_DWO };
	    }
		MicroServer.main(args);
	}

}
