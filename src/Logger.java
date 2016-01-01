import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;


public class Logger {
	public static PrintWriter out;
	private static String fileName = "log.txt";
	private static boolean init = false;
	public static void Log(String text) throws IOException{
		if(!init){
			init();
		}
		out.print(text);
		out.flush();
	}
	public static void ChangeFile(String newFileName){
		fileName = newFileName;
		init = false;
	}
	
	private static void init() throws FileNotFoundException{
		out = new PrintWriter(fileName);
		init = true;
	}
}
