import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class PipeLine {

	public static void main(String[] args) throws IOException {
		File folder = new File("Images");
		File[] files = folder.listFiles();
		ArrayList<File> images = new ArrayList<File>();
		int imagesFound = 0;
		for(int i =0; i < files.length; i++){
			if(files[i].isFile()){
				imagesFound++;
				images.add(files[i]);
			}
		}
		Logger.Log("Found "+ imagesFound + " images\r\n");
		for(int i = ((imagesFound-1)%5)+1; i <= imagesFound; i+=5){
			Logger.Log("running pipeline for "+i+" images\r\n");
			System.out.println("running pipeline for "+i+" images");
			File[] usedFiles = new File[i];
			for(int j = 0; j < i; j++){
				usedFiles[j] = images.get(j);//34);
			}
			
			long start = System.nanoTime();
			double[][] transformedData = Imagehandler.Transform(usedFiles);
			long elapsedTime = System.nanoTime() - start;
			Logger.Log("preprocessing took " + elapsedTime/1000000000 + "seconds\r\n");
			
			start = System.nanoTime();
			Network model = Trainer.Train(transformedData);
			elapsedTime = System.nanoTime() - start;
			Logger.Log("training took " + elapsedTime/1000000000 + "seconds\r\n");
			
			start = System.nanoTime();
			String outFolderName = "Output_Run"+ i;
			new File(outFolderName).mkdirs();
			Classify(model, usedFiles, outFolderName);
			elapsedTime = System.nanoTime() - start;
			Logger.Log("classification took an average " + (elapsedTime/1000000000)/i + " seconds per image\r\n\r\n");
		}
	}
	private static void Classify(Network model, File[] images, String folder) throws IOException{
		
		long decodeTime = 0;
		long DTOCStime = 0;
		long applTime = 0;
		long writeTime = 0;
		for(int i =0; i < images.length; i++){
			try {
				long start = System.nanoTime();
				BufferedImage originalImage = ImageIO.read(images[i]);
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
				ImageIO.write(img, "jpg", new File(folder + "/" + images[i].getName().split("[.]")[0] + ".jpg"));
				writeTime += System.nanoTime() - start;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("error reading " + images[i].getName() + ", " + e.getMessage());
			} 
		}
		Logger.Log("Decode Img\t" + decodeTime/1000000000/images.length + "seconds\r\n");
		Logger.Log("DTOCS \t\t" + DTOCStime/1000000000/images.length + "seconds\r\n");
		Logger.Log("Application \t" + applTime/1000000000/images.length + "seconds\r\n");
		Logger.Log("Write \t\t" + writeTime/1000000000/images.length + "seconds\r\n");
	}

}
