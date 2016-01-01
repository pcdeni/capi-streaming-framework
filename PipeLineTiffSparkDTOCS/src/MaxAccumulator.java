import org.apache.spark.AccumulatorParam;

public class MaxAccumulator implements AccumulatorParam<double[]> {
	public double[] zero(double[] initialValue) {
		for(int i = 0 ; i < initialValue.length;i++){
			initialValue[i] = 0.0;
		}
		return initialValue;
	}
	public double[] addAccumulator(double[] current, double[] update) {
		for(int i = 0 ; i < current.length;i++){
			current[i] = Math.max(current[i], update[i]);
		}
		return current;
	}
	@Override
	public double[] addInPlace(double[] current, double[] update) {
		for(int i = 0 ; i < current.length;i++){
			current[i] = Math.max(current[i], update[i]);
		}
		return current;
	}
}
