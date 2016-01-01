import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;


public class CustomLogger {
	public static BufferedOutputStream out;
	private static String fileName = "hdfs://storage-host.cluster:9000/user/cfolkers/log.txt";
	private static boolean init = false;
	public static void Log(String text) throws IOException{
		if(!init){
			init();
		}
		out.write(text.getBytes("UTF-8"));
		out.flush();
	}
	public static void ChangeFile(String newFileName) throws IOException{
		out.close();
		fileName = newFileName;
		init = false;
	}
	
	private static void init() throws IOException{
		FileSystem fs = DistributedFileSystem.get(new Configuration());
		FSDataOutputStream output = fs.create(new Path(fileName));

		out = new BufferedOutputStream(output);
		init = true;
	}
}
