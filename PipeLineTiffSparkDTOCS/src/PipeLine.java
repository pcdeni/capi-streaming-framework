import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.spark.api.java.*;
import org.apache.spark.SparkConf;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import javax.imageio.ImageIO;

public class PipeLine {

	public static void main(String[] args) throws IOException {
		SparkConf conf = new SparkConf().setAppName("pipeline")
									.set("spark.driver.maxResultSize", "8g")
									.set("spark.eventLog.dir","hdfs://storage-host.cluster:9000/user/cfolkers/.sparkStaging/spark-events")
									.set("spark.eventLog.enabled","true");
		JavaSparkContext sc = new JavaSparkContext(conf);
		FileSystem fs = DistributedFileSystem.get(new Configuration());
		 Path src=new Path("Images");
		FileStatus[] files= fs.listStatus(src);
		ArrayList<Path> images = new ArrayList<Path>();
		int imagesFound = 0;
		for(int i =0; i < files.length; i++){
			if(files[i].isFile()){
				imagesFound++;
				images.add(files[i].getPath());
			}
		}
		CustomLogger.Log("Found "+ imagesFound + " images\r\n");
		for(int i = ((imagesFound-1)%5)+1; i <= imagesFound; i+=5){
			CustomLogger.Log("running pipeline for "+i+" images\r\n");
			System.out.println("running pipeline for "+i+" images");
			ArrayList<URI> usedPaths = new ArrayList<URI>();
			for(int j = 0; j < i; j++){
				usedPaths.add(images.get(j).toUri());
			}
			
			long start = System.nanoTime();
			double[][] transformedData = Imagehandler.Transform(usedPaths,sc);
			long elapsedTime = System.nanoTime() - start;
			CustomLogger.Log("preprocessing took " + elapsedTime/1000000000 + "seconds\r\n");
			
			start = System.nanoTime();
			CustomLogger.Log("data length" + transformedData.length + "\n") ;
			Network model = Trainer.Train(transformedData);
			elapsedTime = System.nanoTime() - start;
			CustomLogger.Log("training took " + elapsedTime/1000000000 + "seconds\r\n");
			
			start = System.nanoTime();
			String outFolderName = "Output_Run"+ i;
			
			

			//out = new BufferedOutputStream(output);
			//new File(outFolderName).mkdirs();
			//Classify(model, usedPaths, outFolderName);
			elapsedTime = System.nanoTime() - start;
			CustomLogger.Log("classification took an average " + (elapsedTime/1000000000)/i + " seconds per image\r\n\r\n");
		}
	}
	private static void Classify(Network model, ArrayList<URI> images, String folder) throws IOException{
		
		long decodeTime = 0;
		long DTOCStime = 0;
		long applTime = 0;
		long writeTime = 0;
		for(int i =0; i < images.size(); i++){
			try {
				long start = System.nanoTime();
				FileSystem fs = DistributedFileSystem.get(new Configuration());
				Path imagePath = new Path(images.get(i));
		        FSDataInputStream instream = fs.open(imagePath);
				BufferedImage originalImage = ImageIO.read(instream);
				instream.close();
				if(originalImage == null) continue;
				decodeTime += System.nanoTime() - start;
				start = System.nanoTime();
				double[][] contents = Imagehandler.Transform2(originalImage);
				DTOCStime += System.nanoTime() - start;
				start = System.nanoTime();
				BufferedImage img = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
				byte[] output = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
				for(int j = 0; j < img.getHeight(); j++){
					for(int k = 0; k < img.getWidth(); k++){
						int indexContent = k * img.getHeight() + j;
						int indexOutput = j * img.getWidth() + k;
						
						double[] result = model.run(contents[indexContent]);
						double max = Double.NEGATIVE_INFINITY;
						int maxIndex = 0;
						for(int l =0; l < result.length; l++){
							if(result[l] > max){
								max = result[l];
								maxIndex = l;
							}
						}
						double ratio = Math.floor(254/(result.length-1));
						output[indexOutput] = (byte)(maxIndex*ratio);
					}
					
				}
				applTime += System.nanoTime() - start;
				start = System.nanoTime();
				FSDataOutputStream outputStream = fs.create(new Path(folder + "/" + imagePath.getName().split("[.]")[0] + ".jpg"));
				ImageIO.write(img, "jpg", outputStream);
				outputStream.close();
				writeTime += System.nanoTime() - start;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("error reading image " + i + ", " + e.getMessage());
			} 
		}
		CustomLogger.Log("Decode Img\t" + decodeTime/1000000000/images.size() + "seconds\r\n");
		CustomLogger.Log("DTOCS \t\t" + DTOCStime/1000000000/images.size() + "seconds\r\n");
		CustomLogger.Log("Application \t" + applTime/1000000000/images.size() + "seconds\r\n");
		CustomLogger.Log("Write \t\t" + writeTime/1000000000/images.size() + "seconds\r\n");
	}

}
