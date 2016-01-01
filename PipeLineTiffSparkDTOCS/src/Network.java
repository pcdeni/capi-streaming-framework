import java.util.Random;


public class Network {
	double[] weights;
	double[] weightDelta;
	byte[] weightDirection;
	double[] biases;
	byte[] layerSizes;
	double outputs[];
	OutputFunction[] functions;
	double alpha = 0.01;
	double initdelta = 0.07;
	double deltaInc = 1.2;
	double deltaDec = 0.5;
	double deltaMax = 50.0;
	double[] weightDeltasAccum;
	
	public Network(byte[] layerSizes, OutputFunction[] functions ){
		int layerCount = Math.min(layerSizes.length,functions.length+1);
		int weightCount = 0;
		int outputCount = layerSizes[0];
		for(int i = 1; i< layerCount;i++){
			weightCount += (layerSizes[i-1]+1)*layerSizes[i];
			outputCount += layerSizes[i]+1;
		}
		weights = new double[weightCount];
		weightDelta = new double[weightCount];
		weightDirection = new byte[weightCount];
		outputs = new double[outputCount];
		this.layerSizes = layerSizes;
		this.functions = functions;
		initParams();
	}
	public double[] getWeights(){
		double[] copyOfWeights = new double[weights.length];
		for(int i = 0; i < weights.length; i++){
			copyOfWeights[i] = weights[i];
		}
		return copyOfWeights;
	}
	
	public void setWeights(double[] newWeights){
		for(int i = 0; i < newWeights.length; i++){
			weights[i] = newWeights[i];
		}
	}
	
	public double[] run(double[] inputs){
		for(int i =0; i < layerSizes[0]; i++){
			outputs[i] = inputs[i];
		}
		int previousOffset = 0;
		int offset = layerSizes[0]+1;
		int weightsOffset = 0;
		for(int i = 1; i < layerSizes.length; i++){
			for(int j = 0; j < layerSizes[i]; j++, offset++){
				outputs[offset] = 0;
				for(int k = 0; k <= layerSizes[i-1]; k++){
					outputs[offset] += outputs[previousOffset + k] * weights[weightsOffset + j*(layerSizes[i-1]+1) + k];
				}
				outputs[offset] = transfer(outputs[offset],i-1);
			}
			previousOffset += layerSizes[i-1]+1;
			weightsOffset += layerSizes[i] * (layerSizes[i-1]+1);
			offset++;
		}
		double[] result = new double[layerSizes[layerSizes.length-1]];
		for(int i =0; i < result.length; i++){
			result[i] = outputs[previousOffset + i];
		}
		return result;
	}
	
	public double validate(double[] inputs, double[] expectedOutput){
		double[] currentOutput = run(inputs);
		double[] deltas = new double[currentOutput.length];
		double sum = 0;
		for(int i = 0; i < currentOutput.length; i++){
			deltas[i] = currentOutput[i] - expectedOutput[i];
			sum += Math.pow(deltas[i],2);
		}
		return (sum/currentOutput.length);
	}
	
	public double train(double[] inputs, double[] expectedOutput){
		double[] currentOutput = run(inputs);
		double[] deltas = new double[currentOutput.length];
		double sum = 0;
		for(int i = 0; i < currentOutput.length; i++){
			deltas[i] = currentOutput[i] - expectedOutput[i];
			sum += Math.pow(deltas[i],2);
		}
		adapt(deltas);
		return (sum/currentOutput.length);
	}
	
	private void adapt(double[] deltas){
		double[] layerDeltas = new double[layerSizes[2]];
		double[] newWeights = new double[weights.length];
		for(int i = 0; i < layerSizes[2]; i++){
			layerDeltas[i] =  -deltas[i] * deltaTransfer(outputs[outputs.length - layerSizes[2] + i],1) ;
			for(int j = 0; j< layerSizes[1]+1;j++){
				double delta =  layerDeltas[i] * outputs[outputs.length - layerSizes[2] - (layerSizes[1]+1) + j];
				int index = weights.length - layerSizes[2]*(layerSizes[1]+1) + i * (layerSizes[1]+1) + j;
				if(Math.signum(delta) == 0|| weightDirection[index] == 0){
					weightDirection[index] = (byte)Math.signum(delta);
				}
				else if(weightDirection[index] == (byte)Math.signum(delta)){
			    	weightDelta[index] *= deltaInc;
			    }
			    else{
			    	weightDelta[index] *= deltaDec;
			    	weightDirection[index] = (byte)Math.signum(delta);
			    }
				weightDelta[index] = Math.min(weightDelta[index], deltaMax);
				newWeights[index] = weights[index]  + weightDelta[index] *Math.signum(delta);
			}
		}
		for(int i = 0; i < layerSizes[1]; i++){
			double layerDeltaSum =0;
			for(int k =0; k < layerSizes[2];k++){
				int index = weights.length - layerSizes[2]*(layerSizes[1]+1) + k * (layerSizes[1]+1) + i;
				layerDeltaSum += layerDeltas[k] * weights[index];
				weights[index] = newWeights[index];
			}
			for(int j = 0; j<= layerSizes[0];j++){
				double delta = layerDeltaSum * deltaTransfer(outputs[outputs.length - layerSizes[2] - (layerSizes[1]+1) + j],0) * outputs[j];
				int index = weights.length - layerSizes[2]*(layerSizes[1]+1) - layerSizes[1]*(layerSizes[0]+1)  + i * (layerSizes[0]+1) + j;
				if(Math.signum(delta) == 0 || weightDirection[index] == 0){
					weightDirection[index] = (byte)Math.signum(delta);
				}
				else if(weightDirection[index] == (byte)Math.signum(delta)){
			    	weightDelta[index] *= deltaInc;
			    }
			    else{
			    	weightDelta[index] *= deltaDec;
			    	weightDirection[index] = (byte)Math.signum(delta);
			    }
				weightDelta[index] = Math.min(weightDelta[index], deltaMax);
				weights[index] += weightDelta[index] *Math.signum(delta);
			}
		}
		for(int k =0; k < layerSizes[2];k++){
			int index = weights.length - layerSizes[2]*(layerSizes[1]+1) + k * (layerSizes[1]+1) + layerSizes[1];
			weights[index] = newWeights[index];
		}
	}
	public void startBatchTrainig(){
		weightDeltasAccum= new double[weights.length];
	}
	
	public void finishBatchTrainig(){
		for(int i =0; i < weightDeltasAccum.length; i++){
			double delta = weightDeltasAccum[i];
			if(Math.signum(delta) == 0|| weightDirection[i] == 0){
				weightDirection[i] = (byte)Math.signum(delta);
			}
			else if(weightDirection[i] == (byte)Math.signum(delta)){
		    	weightDelta[i] *= deltaInc;
		    }
		    else{
		    	weightDelta[i] *= deltaDec;
		    	weightDirection[i] = (byte)Math.signum(delta);
		    }
			weightDelta[i] = Math.min(weightDelta[i], deltaMax);
			weights[i] += weightDelta[i] * Math.signum(weightDeltasAccum[i]);
		}
	}
	public double trainForBatch(double[] inputs, double[] expectedOutput){
		double[] currentOutput = run(inputs);
		double[] deltas = new double[currentOutput.length];
		double sum = 0;
		for(int i = 0; i < currentOutput.length; i++){
			deltas[i] = currentOutput[i] - expectedOutput[i];
			sum += Math.pow(deltas[i],2);
		}
		addWeightDeltas(deltas);
		return Math.sqrt(sum/currentOutput.length);
	}
	private void addWeightDeltas(double[] deltas){
		double[] layerDeltas = new double[layerSizes[2]];
		for(int i = 0; i < layerSizes[2]; i++){
			layerDeltas[i] =  -deltas[i] * deltaTransfer(outputs[outputs.length - layerSizes[2] + i],1) ;
			for(int j = 0; j< layerSizes[1]+1;j++){
				double delta =  layerDeltas[i] * outputs[outputs.length - layerSizes[2] - (layerSizes[1]+1) + j];
				int index = weights.length - layerSizes[2]*(layerSizes[1]+1) + i * (layerSizes[1]+1) + j;
//				if(Math.signum(delta) == 0|| weightDirection[index] == 0){
//					weightDirection[index] = (byte)Math.signum(delta);
//				}
//				else if(weightDirection[index] == (byte)Math.signum(delta)){
//			    	weightDelta[index] *= deltaInc;
//			    }
//			    else{
//			    	weightDelta[index] *= deltaDec;
//			    	weightDirection[index] = (byte)Math.signum(delta);
//			    }
//				weightDelta[index] = Math.min(weightDelta[index], deltaMax);
				weightDeltasAccum[index] += delta;
			}
		}
		for(int i = 0; i < layerSizes[1]; i++){
			double layerDeltaSum =0;
			for(int k =0; k < layerSizes[2];k++){
				int index = weights.length - layerSizes[2]*(layerSizes[1]+1) + k * (layerSizes[1]+1) + i;
				layerDeltaSum += layerDeltas[k] * weights[index];
			}
			for(int j = 0; j<= layerSizes[0];j++){
				double delta = layerDeltaSum * deltaTransfer(outputs[outputs.length - layerSizes[2] - (layerSizes[1]+1) + j],0) * outputs[j];
				int index = weights.length - layerSizes[2]*(layerSizes[1]+1) - layerSizes[1]*(layerSizes[0]+1)  + i * (layerSizes[0]+1) + j;
//				if(Math.signum(delta) == 0 || weightDirection[index] == 0){
//					weightDirection[index] = (byte)Math.signum(delta);
//				}
//				else if(weightDirection[index] == (byte)Math.signum(delta)){
//			    	weightDelta[index] *= deltaInc;
//			    }
//			    else{
//			    	weightDelta[index] *= deltaDec;
//			    	weightDirection[index] = (byte)Math.signum(delta);
//			    }
//				weightDelta[index] = Math.min(weightDelta[index], deltaMax);
				weightDeltasAccum[index] += delta;
			}
		}
	}
	private double transfer(double x,int functionIndex){
		switch(functions[functionIndex]){
		case tansig :
			return Math.tanh(x);
		case purelin :
			return x;
		default:
			return x;
		}
	}
	
	private double deltaTransfer(double x,int functionIndex){
		switch(functions[functionIndex]){
		case tansig :
			return 1 - Math.pow(Math.tanh(x),2);
		case purelin :
			return 1;
		default:
			return x;
		}
	}
	
	public void initParams(){
		Random r = new Random();
		for(int i =0; i < weights.length; i++){
			weights[i] = r.nextDouble()-0.5;
			weightDelta[i] = initdelta;
		}
		int index = 0;
		for(int i = 0; i < layerSizes.length-1; i++){
			index += layerSizes[i]+1;
			outputs[index-1] = 1;
		}
	}
}


