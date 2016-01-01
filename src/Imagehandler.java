import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class Imagehandler {
	final static double[] alphaValues = {0.6496, 0.9040, 0.9255, 0.9457, 0.9648, 0.9829} ;
	
	public static double[][] Transform(File[] images) throws IOException {
		
		
		ArrayList<double[]> trainingData = new ArrayList<double[]>();
		long decodeTime = 0;
		long loadAnnTime = 0;
		long DTOCStime = 0;
		long filterTime = 0;
		long start = System.nanoTime();
		double[] max = new double[alphaValues.length];
		double[] min = new double[alphaValues.length];
		for(int i = 0; i< alphaValues.length; i++){
			min[i] = Double.POSITIVE_INFINITY;
		}
		long normalisationTime = System.nanoTime() - start;
		for(int i =0; i < images.length; i++){
			//System.out.println("image " + i);
			if(images[i].isFile()){
				start = System.nanoTime();
				BufferedImage image = ImageIO.read(images[i]);
				decodeTime += System.nanoTime() - start;
				start = System.nanoTime();
				if(image == null) continue;
				String filename = images[i].getName().split("[.]")[0];
				String overlayPath = images[i].getParent() + "/Overlay/" + filename + "_Y.bin";
				byte[] targetData = RetrieveAnnotation(overlayPath);
				loadAnnTime += System.nanoTime() - start;
				start = System.nanoTime();
				double[][] transformedData = new double[6][image.getWidth()*image.getHeight()];
				for(int j = 0; j < alphaValues.length; j++){
					//System.out.println("DTOCS " + j);
					transformedData[j] = DTOCS(((DataBufferByte)image.getRaster().getDataBuffer()).getData(), image.getWidth(), image.getHeight(),alphaValues[j]);
					if(transformedData[j].length != targetData.length ){
						System.out.println("target size doesn't match image size");
					}
				}
				DTOCStime += System.nanoTime() - start;
				start = System.nanoTime();
				for(int k =0; k< targetData.length;k++){
					for(int l = 0; l < alphaValues.length; l++){
						max[l] = Math.max(max[l], transformedData[l][k]);
						min[l] = Math.min(min[l],transformedData[l][k]);
					}
					if(targetData[k] != 0){
						double[] set = new double[7];
						for(int l = 0; l < 6; l++){
							set[l] = transformedData[l][k];
						}
						set[6] = targetData[k];
						trainingData.add(set);
					}
				}
				filterTime += System.nanoTime() - start;
			}
		}
		start = System.nanoTime();
		double bias[] = new double[alphaValues.length]; 
		double ratio[] = new double[alphaValues.length];
		for(int i = 0; i< alphaValues.length; i++){
			bias[i] = min[i] + 0.5 *Math.abs(max[i]-min[i]);
			ratio[i] = 2/Math.abs(max[i]-min[i]);
		}
		
		for(int i = 0; i < trainingData.size(); i++){
			for(int j = 0; j< alphaValues.length; j++){
				trainingData.get(i)[j] = (trainingData.get(i)[j]-bias[j])*ratio[j];
			}
		}
		normalisationTime += System.nanoTime() - start;
		start = System.nanoTime();
		double[][] returnData = new double[trainingData.size()][7];
		for(int i = 0; i < trainingData.size(); i++){
			returnData[i] = trainingData.get(i);
		}
		filterTime = System.nanoTime() - start;
		Logger.Log("Decode Img\t" + decodeTime/1000000000 + "seconds\r\n");
		Logger.Log("Load Annotation\t" + loadAnnTime/1000000000 + "seconds\r\n");
		Logger.Log("DTOCS \t\t" + DTOCStime/1000000000 + "seconds\r\n");
		Logger.Log("Normalise \t" + normalisationTime/1000000000 + "seconds\r\n");
		Logger.Log("filter \t\t" + filterTime/1000000000 + "seconds\r\n");
		
		return returnData;
		
	}
	public static double[][] Transform2(BufferedImage image) throws IOException {
		
		double[][] trainingData = new double[image.getWidth()*image.getHeight()][6];
		for(int j = 0; j < alphaValues.length; j++){
			double[] transformedData = DTOCS(((DataBufferByte)image.getRaster().getDataBuffer()).getData(), image.getWidth(), image.getHeight(),alphaValues[j]);
			for(int k =0; k< transformedData.length;k++){
				trainingData[k][j] = transformedData[k];
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
	private static byte[] RetrieveAnnotation(String filename){
		File matFile = new File(filename);
		byte[] buffer = new byte[(int)matFile.length()];
		try{
			InputStream stream = new FileInputStream(matFile);
			stream.read(buffer, 0, (int)matFile.length());
			stream.close();
		}
		catch(Exception e){
			System.out.println("Error reading annotation file " + e.getMessage());
		}
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
