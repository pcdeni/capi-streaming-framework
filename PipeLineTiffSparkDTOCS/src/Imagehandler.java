import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.spark.Accumulator;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;

public class Imagehandler {
	final static double[] alphaValues = {0.6496, 0.9040, 0.9255, 0.9457, 0.9648, 0.9829} ;
	
	public static double[][] Transform(List<URI> images, JavaSparkContext sc) throws IOException {
		long start = System.nanoTime();
		double[] max = new double[alphaValues.length];
		double[] min = new double[alphaValues.length];
		for(int i = 0; i< alphaValues.length; i++){
			min[i] = Double.POSITIVE_INFINITY;
		}
		long normalisationTime = System.nanoTime() - start;
		start = System.nanoTime();
		final Accumulator<double[]> minAccum = sc.accumulator(new double[alphaValues.length], new MinAccumulator());
		final Accumulator<double[]> maxAccum = sc.accumulator(new double[alphaValues.length], new MaxAccumulator());
		
		JavaRDD<URI> distData = sc.parallelize(images,images.size());
		CustomLogger.Log("partitions = " + distData.partitions().size()+ "\n");
		JavaRDD<double[]> mappedData =  distData.flatMap( new FlatMapFunction<URI,double[]>() {
			@Override
			public Iterable<double[]> call(URI img) throws Exception {
				// TODO Auto-generated method stub
				return transformFunction(img, minAccum,maxAccum);
			}
		});
		
		
		mappedData.cache();
		mappedData.count();
//		mappedData.foreachPartition(new VoidFunction<Iterator<double[]>>() {
//			@Override
//			public void call(Iterator<double[]> entries) throws Exception {
//				double[] max = new double[6];
//				double[] min = new double[6];
//				for(int i = 0; i<6;i++){
//					min[i] = Double.MAX_VALUE;
//				}
//				while(entries.hasNext()){
//					double[] entry = entries.next();
//					for(int i = 0; i < entry.length; i++){
//						for(int j = 0; j<6;j++){
//							min[j] = Math.min(entry[j], min[j]);
//							max[j] = Math.max(entry[j], max[j]);
//						}
//					}
//				}
//				minAccum.add(min);
//				maxAccum.add(max);
//				
//			}
//		});
		//CustomLogger.Log("res =" + res.size() + "\r\n");
		long LoadandDTOCStime = System.nanoTime() - start;
		
//		start = System.nanoTime();
//		JavaRDD<double[]> filteredData = mappedData.filter(new Function<double[], Boolean>() {
//			
//			@Override
//			public Boolean call(double[] entry) throws Exception {
//				return entry[6] > 0;
//			}
//		});
//		long filterTime = System.nanoTime() - start;
		
		start = System.nanoTime();
		max = maxAccum.value();
		min = minAccum.value();
		final double bias[] = new double[alphaValues.length]; 
		final double ratio[] = new double[alphaValues.length];
		for(int i = 0; i< alphaValues.length; i++){
			bias[i] = min[i] + 0.5 *Math.abs(max[i]-min[i]);
			ratio[i] = 2/Math.abs(max[i]-min[i]);
		}
		
		JavaRDD<double[]> normalisedData = mappedData.map(new Function<double[],double[]>(){
			@Override
			public double[] call(double[] entry) throws Exception {
				for(int j = 0; j< alphaValues.length; j++){
					entry[j] = (entry[j]-bias[j])*ratio[j];
				}
				return entry;
			}
		});
		List<double[]> res = normalisedData.collect(); 
		normalisationTime += System.nanoTime() - start;
		
		start = System.nanoTime();
		double[][] returnData = new double[res.size()][7];
		for(int i = 0; i < res.size(); i++){
			returnData[i] = res.get(i);
		}
		long filterTime = System.nanoTime() - start;
		
		CustomLogger.Log("Load and DTOCS \t\t" + LoadandDTOCStime/1000000 + "miliseconds\r\n");
		CustomLogger.Log("Normalise \t" + normalisationTime/1000000 + "miliseconds\r\n");
		CustomLogger.Log("filter \t\t" + filterTime/1000000 + "miliseconds\r\n");
		
		return returnData;
		
	}
	private static ArrayList<double[]> transformFunction(URI imageUri,Accumulator<double[]> minAccum,Accumulator<double[]> maxAccum) throws IOException, InterruptedException{
		ArrayList<double[]> data = new ArrayList<double[]>();
		FileSystem fs = DistributedFileSystem.get(new Configuration());
		Path imagePath = new Path(imageUri);
        FSDataInputStream instream = fs.open(imagePath);
		BufferedImage image = ImageIO.read(instream);
		instream.close();
		if(image == null) return data;
		String filename = imagePath.getName().split("[.]")[0];
		String overlayPath = "Images/Overlay/" + filename + "_Y.bin";
		byte[] targetData = RetrieveAnnotation(overlayPath);
		double[][] transformedData = new double[6][image.getWidth()*image.getHeight()];
		DTOCSThread[] threads = new DTOCSThread[alphaValues.length]; 
		for(int j = 0; j < alphaValues.length; j++){
			threads[j] = new DTOCSThread(image,alphaValues[j]); 
		//	threads[j].start();
			//double[] transformedData = DTOCS(((DataBufferByte)image.getRaster().getDataBuffer()).getData(), image.getWidth(), image.getHeight(),alphaValues[j]);
			threads[j].run();
		}
		//for(int j = 0; j < alphaValues.length; j++){
		//	threads[j].join();
		//}
		for(int j = 0; j < alphaValues.length; j++){
			transformedData[j] = threads[j].result;
			if(transformedData[j].length != targetData.length ){
				System.out.println("target size doesn't match image size");
			}
		}
		for(int k =0; k< targetData.length;k++){
			
			double[] set = new double[7];
			for(int l = 0; l < 6; l++){
				set[l] = transformedData[l][k];
			}
			set[6] = targetData[k];
			minAccum.add(set);
			maxAccum.add(set);
			if(set[6] > 0){
				data.add(set);
			}
		}
		return data;
	}
	
	public static double[][] Transform2(BufferedImage image) throws IOException {
		
		double[][] trainingData = new double[image.getWidth()*image.getHeight()][6];
		/*for(int j = 0; j < alphaValues.length; j++){
			double[] transformedData = DTOCS(((DataBufferByte)image.getRaster().getDataBuffer()).getData(), image.getWidth(), image.getHeight(),alphaValues[j]);
			//!double[] transformedData = DTOCS(((DataBufferByte)image.getRaster().getDataBuffer()).getData(), image.getWidth(), image.getHeight(),alphaValues);
			for(int k =0; k< transformedData.length;k++){
				trainingData[k][j] = transformedData[k];
			}
		}*/

		DTOCSThread[] threads = new DTOCSThread[alphaValues.length]; 
		for(int j = 0; j < alphaValues.length; j++){
			threads[j] = new DTOCSThread(image,alphaValues[j]); 
			threads[j].run();
		
		for(int k =0; k< transformedData.length;k++){
				trainingData[k][j] = threads[k].result;
			}
		}

		double[] max = new double[alphaValues.length];
		double[] min = new double[alphaValues.length];
		for(int i = 0; i< alphaValues.length; i++){
			min[i] = Double.POSITIVE_INFINITY;
		}
		for(int i = 0; i < trainingData.length; i++){
			for(int j = 0; j < alphaValues.length; j++){
				max[j] = Math.max(max[j], trainingData[i][j]);
				min[j] = Math.min(min[j],trainingData[i][j]);
			}
		}
		double bias[] = new double[alphaValues.length]; 
		double ratio[] = new double[alphaValues.length];
		for(int i = 0; i< alphaValues.length; i++){
			bias[i] = min[i] + 0.5 *Math.abs(max[i]-min[i]);
			ratio[i] = 2/Math.abs(max[i]-min[i]);
		}
		for(int i = 0; i < trainingData.length; i++){
			for(int j = 0; j< alphaValues.length; j++){
				trainingData[i][j] = (trainingData[i][j]-bias[j])*ratio[j];
			}
		}
		return trainingData;
		
	}
	private static byte[] RetrieveAnnotation(String filename) throws IOException{
		Path matFile = new Path(filename);
		FileSystem fs = DistributedFileSystem.get(new Configuration());   
		FileStatus status = fs.getFileStatus(matFile);
		byte[] buffer = new byte[(int)status.getLen()];
		FSDataInputStream stream = fs.open(matFile);
		stream.readFully(buffer);//(buffer, 0, (int)status.getLen());
		stream.close();
		return buffer;
	}
	
	private static void DTOCS_old(BufferedImage image, double alpha){
		byte[] data = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		double[] grayData = new double[(image.getWidth()+2) * (image.getHeight()+2)];
		double[] grayDataTemp = new double[grayData.length];
		for(int i =1; i < imageHeight+1; i++){
			for(int j = 1; j <  imageWidth+1; j++){
				grayData[i*(imageWidth+2) + j] = 0.2989*(data[(i-1)*imageWidth*3 + (j-1)*3 ] &0xFF) + 0.5870*(data[(i-1)*imageWidth*3 + (j-1)*3 + 1]&0xFF) + 0.1140*(data[(i-1)*imageWidth*3 + (j-1)*3+2]&0xFF);
				grayDataTemp[i*(imageWidth+2) + j] = Double.POSITIVE_INFINITY;
			}
		}
		
		for(int u =0; u < 4; u++){
			for(int i =1; i < imageHeight+1; i++){
				for(int j = 1; j < imageWidth+1; j++){
					double a = grayData[(i-1)*(imageWidth+2) + (j-1)];
					double b = grayData[(i-1)*(imageWidth+2) + j];
					double e = grayData[(i-1)*(imageWidth+2) + (j+1)];
					double d = grayData[i*(imageWidth+2) + (j-1)];
					double c = grayData[i*(imageWidth+2) + j];
					
					double a2 = grayDataTemp[(i-1)*(imageWidth+2) + (j-1)];
					double b2 = grayDataTemp[(i-1)*(imageWidth+2) + j];
					double e2 = grayDataTemp[(i-1)*(imageWidth+2) + (j+1)];
					double d2 = grayDataTemp[i*(imageWidth+2) + (j-1)];
					double c2 = grayDataTemp[i*(imageWidth+2) + j];
					
					double da = Math.abs(c-a);
					double db = Math.abs(c-b);
					double de = Math.abs(c-e);
					double dd = Math.abs(c-d);
					
					grayDataTemp[i*(imageWidth+2) + j] =   alpha*Math.min(c2, Math.min(Math.min(da+a2+1,db+b2+1),Math.min(de+e2+1,dd+d2+1)));
				}
			}
			for(int i = imageHeight; i > 0 ; i--){
				for(int j = imageWidth; j > 0; j--){
					double a = grayData[(i)*(imageWidth+2) + (j+1)];
					double b = grayData[(i+1)*(imageWidth+2) + (j-1)];
					double e = grayData[(i+1)*(imageWidth+2) + (j)];
					double d = grayData[(i+1)*(imageWidth+2) + (j+1)];
					double c = grayData[i*(imageWidth+2) + j];
					
					double a2 = grayDataTemp[(i)*(imageWidth+2) + (j+1)];
					double b2 = grayDataTemp[(i+1)*(imageWidth+2) + (j-1)];
					double e2 = grayDataTemp[(i+1)*(imageWidth+2) + (j)];
					double d2 = grayDataTemp[(i+1)*(imageWidth+2) + (j+1)];
					double c2 = grayDataTemp[i*(imageWidth+2) + j];
					
					double da = Math.abs(c-a);
					double db = Math.abs(c-b);
					double de = Math.abs(c-e);
					double dd = Math.abs(c-d);
					
					grayDataTemp[i*(imageWidth+2) + j] =   alpha*Math.min(c2, Math.min(Math.min(da+a2+1,db+b2+1),Math.min(de+e2+1,dd+d2+1)));
				}
			}
			for(int i =1; i < imageHeight+1; i++){
				for(int j = 1; j < imageWidth+1; j++){
					double a = grayData[(i)*(imageWidth+2) + (j+1)];
					double b = grayData[(i-1)*(imageWidth+2) + (j-1)];
					double e = grayData[(i-1)*(imageWidth+2) + (j)];
					double d = grayData[(i-1)*(imageWidth+2) + (j+1)];
					double c = grayData[i*(imageWidth+2) + j];
					
					double a2 = grayDataTemp[(i)*(imageWidth+2) + (j+1)];
					double b2 = grayDataTemp[(i-1)*(imageWidth+2) + (j-1)];
					double e2 = grayDataTemp[(i-1)*(imageWidth+2) + (j)];
					double d2 = grayDataTemp[(i-1)*(imageWidth+2) + (j+1)];
					double c2 = grayDataTemp[i*(imageWidth+2) + j];
					
					double da = Math.abs(c-a);
					double db = Math.abs(c-b);
					double de = Math.abs(c-e);
					double dd = Math.abs(c-d);
					
					grayDataTemp[i*(imageWidth+2) + j] =   alpha*Math.min(c2, Math.min(Math.min(da+a2+1,db+b2+1),Math.min(de+e2+1,dd+d2+1)));
				}
			}
			for(int i =1; i < imageHeight+1; i++){
				for(int j = 1; j < imageWidth+1; j++){
					double a = grayData[(i)*(imageWidth+2) + (j-1)];
					double b = grayData[(i+1)*(imageWidth+2) + (j-1)];
					double e = grayData[(i+1)*(imageWidth+2) + (j)];
					double d = grayData[(i+1)*(imageWidth+2) + (j+1)];
					double c = grayData[i*(imageWidth+2) + j];
					
					double a2 = grayDataTemp[(i)*(imageWidth+2) + (j-1)];
					double b2 = grayDataTemp[(i+1)*(imageWidth+2) + (j-1)];
					double e2 = grayDataTemp[(i+1)*(imageWidth+2) + (j)];
					double d2 = grayDataTemp[(i+1)*(imageWidth+2) + (j+1)];
					double c2 = grayDataTemp[i*(imageWidth+2) + j];
					
					double da = Math.abs(c-a);
					double db = Math.abs(c-b);
					double de = Math.abs(c-e);
					double dd = Math.abs(c-d);
					
					grayDataTemp[i*(imageWidth+2) + j] =   alpha*Math.min(c2, Math.min(Math.min(da+a2+1,db+b2+1),Math.min(de+e2+1,dd+d2+1)));
				}
			}
			//double[] temp = grayDataTemp;
			//grayDataTemp = grayData;
			for(int i =0; i < grayData.length;i++) grayData[i] = grayDataTemp[i];
			
		}
		double max = 0.0;
		double min = Double.POSITIVE_INFINITY;
		for(int i =1; i < imageHeight+1; i++){
			for(int j = 1; j < imageWidth+1; j++){
				max = Math.max(max, grayData[i*(imageWidth+2) + j]);
				min = Math.min(min,grayData[i*(imageWidth+2) + j]);
			}
		}
		double ratio = 255/(max-min);
		for(int i =1; i < imageHeight+1; i++){
			for(int j = 1; j < imageWidth+1; j++){
				data[(i-1)*imageWidth*3 + (j-1)*3] = (byte)((grayData[i*(imageWidth+2) + j] -min) * ratio);
				data[(i-1)*imageWidth*3 + (j-1)*3 + 1] = (byte)((grayData[i*(imageWidth+2) + j]-min)* ratio);
				data[(i-1)*imageWidth*3 + (j-1)*3 + 2] = (byte)((grayData[i*(imageWidth+2) + j]-min)* ratio);
			}
		}
		
	}
	private static double[] DTOCS(byte[]  data, int imageWidth, int imageHeight, double alpha){

		double[] grayData = new double[(imageWidth+2) * (imageHeight+2)];
		double[] grayDataTemp = new double[grayData.length];
		for(int i =1; i < imageHeight+1; i++){
			for(int j = 1; j <  imageWidth+1; j++){
				grayData[i*(imageWidth+2) + j] = Math.round(0.2989*(data[(i-1)*imageWidth*3 + (j-1)*3 ] &0xFF) + 0.5870*(data[(i-1)*imageWidth*3 + (j-1)*3 + 1]&0xFF) + 0.1140*(data[(i-1)*imageWidth*3 + (j-1)*3+2]&0xFF));
				grayDataTemp[i*(imageWidth+2) + j] = Double.POSITIVE_INFINITY;
			}
		}
		
		for(int u =0; u < 4; u++){
			for(int i =1; i < imageHeight+1; i++){
				for(int j = 1; j < imageWidth+1; j++){
					double a = grayData[(i-1)*(imageWidth+2) + (j-1)];
					double b = grayData[(i)*(imageWidth+2) + (j-1)];
					double e = grayData[(i+1)*(imageWidth+2) + (j-1)];
					double d = grayData[(i-1)*(imageWidth+2) + (j)];
					double c = grayData[i*(imageWidth+2) + j];
					
					double a2 = grayDataTemp[(i-1)*(imageWidth+2) + (j-1)];
					double b2 = grayDataTemp[(i)*(imageWidth+2) + (j-1)];
					double e2 = grayDataTemp[(i+1)*(imageWidth+2) + (j-1)];
					double d2 = grayDataTemp[(i-1)*(imageWidth+2) + (j)];
					double c2 = grayDataTemp[i*(imageWidth+2) + j];
					
					double da = Math.abs(c-a);
					double db = Math.abs(c-b);
					double de = Math.abs(c-e);
					double dd = Math.abs(c-d);
					
					grayDataTemp[i*(imageWidth+2) + j] =   alpha*Math.min(c2, Math.min(Math.min(da+a2+1,db+b2+1),Math.min(de+e2+1,dd+d2+1)));
				}
			}
			for(int i = imageHeight; i > 0 ; i--){
				for(int j = imageWidth; j > 0; j--){
					double a = grayData[(i+1)*(imageWidth+2) + (j+1)];
					double b = grayData[(i)*(imageWidth+2) + (j+1)];
					double e = grayData[(i-1)*(imageWidth+2) + (j+1)];
					double d = grayData[(i+1)*(imageWidth+2) + (j)];
					double c = grayData[i*(imageWidth+2) + j];
					
					double a2 = grayDataTemp[(i+1)*(imageWidth+2) + (j+1)];
					double b2 = grayDataTemp[(i)*(imageWidth+2) + (j+1)];
					double e2 = grayDataTemp[(i-1)*(imageWidth+2) + (j+1)];
					double d2 = grayDataTemp[(i+1)*(imageWidth+2) + (j)];
					double c2 = grayDataTemp[i*(imageWidth+2) + j];
					
					double da = Math.abs(c-a);
					double db = Math.abs(c-b);
					double de = Math.abs(c-e);
					double dd = Math.abs(c-d);
					
					grayDataTemp[i*(imageWidth+2) + j] =   alpha*Math.min(c2, Math.min(Math.min(da+a2+1,db+b2+1),Math.min(de+e2+1,dd+d2+1)));
				}
			}
			for(int i = imageHeight; i > 0 ; i--){
				for(int j = 1; j < imageWidth+1; j++){
					double a = grayData[(i-1)*(imageWidth+2) + (j-1)];
					double b = grayData[(i)*(imageWidth+2) + (j-1)];
					double e = grayData[(i+1)*(imageWidth+2) + (j-1)];
					double d = grayData[(i+1)*(imageWidth+2) + (j)];
					double c = grayData[i*(imageWidth+2) + j];
					
					double a2 = grayDataTemp[(i-1)*(imageWidth+2) + (j-1)];
					double b2 = grayDataTemp[(i)*(imageWidth+2) + (j-1)];
					double e2 = grayDataTemp[(i+1)*(imageWidth+2) + (j-1)];
					double d2 = grayDataTemp[(i+1)*(imageWidth+2) + (j)];
					double c2 = grayDataTemp[i*(imageWidth+2) + j];
					
					double da = Math.abs(c-a);
					double db = Math.abs(c-b);
					double de = Math.abs(c-e);
					double dd = Math.abs(c-d);
					
					grayDataTemp[i*(imageWidth+2) + j] =   alpha*Math.min(c2, Math.min(Math.min(da+a2+1,db+b2+1),Math.min(de+e2+1,dd+d2+1)));
				}
			}
			for(int i =1; i < imageHeight+1; i++){
				for(int j = imageWidth; j > 0; j--){
					double a = grayData[(i-1)*(imageWidth+2) + (j+1)];
					double b = grayData[(i)*(imageWidth+2) + (j+1)];
					double e = grayData[(i+1)*(imageWidth+2) + (j+1)];
					double d = grayData[(i-1)*(imageWidth+2) + (j)];
					double c = grayData[i*(imageWidth+2) + j];
					
					double a2 = grayDataTemp[(i-1)*(imageWidth+2) + (j+1)];
					double b2 = grayDataTemp[(i)*(imageWidth+2) + (j+1)];
					double e2 = grayDataTemp[(i+1)*(imageWidth+2) + (j+1)];
					double d2 = grayDataTemp[(i-1)*(imageWidth+2) + (j)];
					double c2 = grayDataTemp[i*(imageWidth+2) + j];
					
					double da = Math.abs(c-a);
					double db = Math.abs(c-b);
					double de = Math.abs(c-e);
					double dd = Math.abs(c-d);
					
					grayDataTemp[i*(imageWidth+2) + j] =   alpha*Math.min(c2, Math.min(Math.min(da+a2+1,db+b2+1),Math.min(de+e2+1,dd+d2+1)));
				}
			}
			//double[] temp = grayDataTemp;
			//grayDataTemp = grayData;
			//for(int i =0; i < grayData.length;i++) grayData[i] = grayDataTemp[i];
			
		}
		
		
		double[] transformedData = new double[imageWidth*imageHeight];
		for(int i =1; i < imageHeight+1; i++){
			for(int j = 1; j < imageWidth+1; j++){
				//transformedData[(i-1)*imageWidth + j-1] = grayDataTemp[i*(imageWidth+2) + j];
				transformedData[(j-1)*imageHeight + i-1] = grayDataTemp[i*(imageWidth+2) + j];

			}
		}
		return transformedData;
		
	}
}
