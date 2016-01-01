import java.io.IOException;
import java.util.Random;


public class Trainer {

	public static Network Train(double[][] inputData) throws IOException {
		
		
		Network  model = new Network(new byte[] {6,5,3},new OutputFunction[] {OutputFunction.tansig,OutputFunction.purelin});

		
		
		int iterations = 0;
		double trainError = Double.MAX_VALUE;
		double validationError = Double.MAX_VALUE;
		double bestValidationError = Double.MAX_VALUE;
		int bestValidationIteration = 0;
		double[] bestWeights = null;
		

		double lastError = Double.POSITIVE_INFINITY;
		int trainingPart = (int) (inputData.length*0.09);
		ShuffleArray(inputData);
		long deltaCalcTime = 0;
		long deltaApplTime = 0;
		long validationTime = 0;
		while((trainError > 0.0001 || lastError - trainError > 0.00001) && iterations < 1000){
			lastError = trainError;
			trainError = 0;
			long start = System.nanoTime();
			model.startBatchTrainig();
			
			for(int i =0; i < trainingPart; i++){
				double[] output = new double[] {-1,-1,-1};
				output[(int)(inputData[i][6]) -1] = 1;
				if(inputData[i][6] == 3){
					i = i + 1- 1;
				}
				trainError += model.trainForBatch(inputData[i], output);
				
				
			}
			deltaCalcTime += System.nanoTime() - start;
			start = System.nanoTime();
			model.finishBatchTrainig();
			deltaApplTime += System.nanoTime()  -start;
			start = System.nanoTime();
			validationError = 0;
			for(int i =trainingPart; i < inputData.length*0.1; i++){
				double[] output = new double[] {-1,-1,-1};
				output[(int)(inputData[i][6]) -1] = 1;
				validationError += model.validate(inputData[i], output);
			}
			trainError = Math.sqrt(trainError/trainingPart);
			validationError = Math.sqrt(validationError/((inputData.length*0.1)-trainingPart));
			validationTime += System.nanoTime() - start;
			if(validationError < bestValidationError){
				bestValidationError = validationError;
				bestValidationIteration = iterations;
				bestWeights = model.getWeights();
			}
			else{
				if(iterations > bestValidationIteration + 20){
					break;
				}
			}
			iterations++;
		}
		if(bestWeights != null) model.setWeights(bestWeights);
		Logger.Log("get deltas \t" + deltaCalcTime/1000000000 + "seconds\r\n");
		Logger.Log("apply deltas \t" + deltaApplTime/1000000000 + "seconds\r\n");
		Logger.Log("validate \t" + validationTime/1000000000 + "seconds\r\n");
		Logger.Log("stopped after " + iterations + " iterations, with errors(t,v,b): " + trainError + ", " + validationError + ", " + bestValidationError + "\n\r");
		return model;
		
		
	}
	private static void ShuffleArray(double[][] inputData)
	{
		int index;
		double[] temp;
	    Random random = new Random();
	    for (int i = inputData.length - 1; i > 0; i--)
	    {
	        index = random.nextInt(i + 1);
	        temp = inputData[index];
	        inputData[index] = inputData[i];
	        inputData[i] = temp;
	    }
	}
}
