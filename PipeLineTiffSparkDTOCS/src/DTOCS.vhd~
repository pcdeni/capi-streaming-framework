
library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;
  use IEEE.MATH_REAL.ALL;


-- private static double[] DTOCS(byte[]  data (24bit -> 8bit), int imageWidth (1436), int imageHeight (814), double alpha){
	double[] grayData = new double[(imageWidth+2) * (imageHeight+2)];
	double[] grayDataTemp = new double[grayData.length];
	
	--for iterator_1 in 1 to imageHeight+1 loop
	--	for iterator_2 in 1 to imageWidth+1 loop
	--		grayData[iterator_1 * (imageWidth + 2) + iterator_2] := 2989 * (data[(iterator_1 - 1) * imageWidth * 3 + (iterator_2 - 1) * 3] &0xFF)
	--								      + 5870 * (data[(iterator_1 - 1) * imageWidth * 3 + (iterator_2 - 1) * 3 + 1] &0xFF)
	--								      + 1140 * (data[(iterator_1 - 1) * imageWidth * 3 + (iterator_2 - 1) * 3 + 2] &0xFF);
	--		grayDataTemp[iterator_1 * (imageWidth + 2) + iterator_2] := --Double.POSITIVE_INFINITY;
	--	end loop;
	--end loop;
	for iterator in 0 to (imageHeight+1)*(imageWidth+1)-1 loop
		grayData[(floor(iterator/imageWidth)+1) * (imageWidth + 2) + (iterator mod imageWidth)] := 
			2989 * (data[((floor(iterator/imageWidth)+1) - 1) * imageWidth * 3 + ((iterator mod imageWidth) - 1) * 3] &0xFF)
		      + 5870 * (data[((floor(iterator/imageWidth)+1) - 1) * imageWidth * 3 + ((iterator mod imageWidth) - 1) * 3 + 1] &0xFF)
		      + 1140 * (data[((floor(iterator/imageWidth)+1) - 1) * imageWidth * 3 + ((iterator mod imageWidth) - 1) * 3 + 2] &0xFF);
		grayDataTemp[(floor(iterator/imageWidth)+1) * (imageWidth + 2) + (iterator mod imageWidth)] := --Double.POSITIVE_INFINITY;
	end loop

	for repeat_number in 0 to 4 loop
		--for iterator_2 in 1 to imageHeight+1 loop
		--	for iterator_3 in 1 to imageWidth+1 loop
		--		double a  := grayData[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double b  := grayData[(iterator_2    ) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double e  := grayData[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double d  := grayData[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3    )];
		--		double c  := grayData[(iterator_2    ) * (imageWidth + 2) + (iterator_3    )];
		--	
		--		double a2 := grayDataTemp[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double b2 := grayDataTemp[(iterator_2    ) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double e2 := grayDataTemp[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double d2 := grayDataTemp[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3    )];
		--		double c2 := grayDataTemp[(iterator_2    ) * (imageWidth + 2) + (iterator_3    )];
		--	
		--		double da := abs(c - a);
		--		double db := abs(c - b);
		--		double de := abs(c - e);
		--		double dd := abs(c - d);
		--	
		--		grayDataTemp[iterator_2 * (imageWidth + 2) + iterator_3] := alpha * min(c2, min(min(da + a2 + 1, db + b2 + 1), min(de + e2 + 1, dd + d2 + 1)));
		--	end loop;
		--end loop;
		for iterator in 0 to (imageHeight+1)*(imageWidth+1)-1 loop
			double a  := grayData[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double b  := grayData[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double e  := grayData[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double d  := grayData[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
			double c  := grayData[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
		
			double a2 := grayDataTemp[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double b2 := grayDataTemp[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double e2 := grayDataTemp[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double d2 := grayDataTemp[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
			double c2 := grayDataTemp[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
		
			double da := abs(c - a);
			double db := abs(c - b);
			double de := abs(c - e);
			double dd := abs(c - d);
		
			grayDataTemp[(floor(iterator/imageWidth)+1) * (imageWidth + 2) + (iterator mod imageWidth)] := 
				alpha * min(c2, min(min(da + a2 + 1, db + b2 + 1), min(de + e2 + 1, dd + d2 + 1)));
		end loop;

		--for iterator_2 in imageHeight downto 0 loop
		--	for iterator_3 in imageWidth downto 0 loop
		--		double a  := grayData[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double b  := grayData[(iterator_2    ) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double e  := grayData[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double d  := grayData[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3    )];
		--		double c  := grayData[(iterator_2    ) * (imageWidth + 2) + (iterator_3    )];
		--	
		--		double a2 := grayDataTemp[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double b2 := grayDataTemp[(iterator_2    ) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double e2 := grayDataTemp[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double d2 := grayDataTemp[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3    )];
		--		double c2 := grayDataTemp[(iterator_2    ) * (imageWidth + 2) + (iterator_3    )];
		--	
		--		double da := abs(c - a);
		--		double db := abs(c - b);
		--		double de := abs(c - e);
		--		double dd := abs(c - d);
		--	
		--		grayDataTemp[iterator_2 * (imageWidth + 2) + iterator_3] := alpha * min(c2, min(min(da + a2 + 1, db + b2 + 1), min(de + e2 + 1, dd +d2 + 1)));
		--	end loop;
		--end loop;
		for iterator in 0 to (imageHeight+1)*(imageWidth+1)-1 loop
			double a  := grayData[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double b  := grayData[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double e  := grayData[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double d  := grayData[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
			double c  := grayData[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
		
			double a2 := grayDataTemp[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double b2 := grayDataTemp[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double e2 := grayDataTemp[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double d2 := grayDataTemp[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
			double c2 := grayDataTemp[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
		
			double da := abs(c - a);
			double db := abs(c - b);
			double de := abs(c - e);
			double dd := abs(c - d);
		
			grayDataTemp[(floor(iterator/imageWidth)+1) * (imageWidth + 2) + (iterator mod imageWidth)] :=
				alpha * min(c2, min(min(da + a2 + 1, db + b2 + 1), min(de + e2 + 1, dd +d2 + 1)));
		end loop;

		--for iterator_2 in imageHeight downto 0 loop
		--	for iterator_3 in 1 to imageWidth+1 loop
		--		double a  := grayData[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double b  := grayData[(iterator_2    ) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double e  := grayData[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double d  := grayData[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3    )];
		--		double c  := grayData[(iterator_2    ) * (imageWidth + 2) + (iterator_3    )];
		--	
		--		double a2 := grayDataTemp[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double b2 := grayDataTemp[(iterator_2    ) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double e2 := grayDataTemp[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3 - 1)];
		--		double d2 := grayDataTemp[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3    )];
		--		double c2 := grayDataTemp[(iterator_2    ) * (imageWidth + 2) + (iterator_3    )];
		--	
		--		double da := abs(c - a);
		--		double db := abs(c - b);
		--		double de := abs(c - e);
		--		double dd := abs(c - d);
		--	
		--		grayDataTemp[iterator_2 * (imageWidth + 2) + iterator_3] := alpha * min(c2, min(min(da + a2 + 1, db + b2 + 1), min(de + e2 + 1, dd + d2 + 1)));
		--	end loop;
		--end loop;
		for iterator in 0 to (imageHeight+1)*(imageWidth+1)-1 loop
			double a  := grayData[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double b  := grayData[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double e  := grayData[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double d  := grayData[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
			double c  := grayData[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
		
			double a2 := grayDataTemp[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double b2 := grayDataTemp[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double e2 := grayDataTemp[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth) - 1)];
			double d2 := grayDataTemp[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
			double c2 := grayDataTemp[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
		
			double da := abs(c - a);
			double db := abs(c - b);
			double de := abs(c - e);
			double dd := abs(c - d);
		
			grayDataTemp[(floor(iterator/imageWidth)+1) * (imageWidth + 2) + (iterator mod imageWidth)] :=
				alpha * min(c2, min(min(da + a2 + 1, db + b2 + 1), min(de + e2 + 1, dd + d2 + 1)));
		end loop;

		--for iterator_2 in 1 to imageHeight+1 loop
		--	for iterator_3 in imageWidth downto 0 loop
		--		double a  := grayData[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double b  := grayData[(iterator_2    ) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double e  := grayData[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double d  := grayData[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3    )];
		--		double c  := grayData[(iterator_2    ) * (imageWidth + 2) + (iterator_3    )];
		--	
		--		double a2 := grayDataTemp[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double b2 := grayDataTemp[(iterator_2    ) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double e2 := grayDataTemp[(iterator_2 + 1) * (imageWidth + 2) + (iterator_3 + 1)];
		--		double d2 := grayDataTemp[(iterator_2 - 1) * (imageWidth + 2) + (iterator_3    )];
		--		double c2 := grayDataTemp[(iterator_2    ) * (imageWidth + 2) + (iterator_3    )];
		--	
		--		double da := abs(c - a);
		--		double db := abs(c - b);
		--		double de := abs(c - e);
		--		double dd := abs(c - d);
		--	
		--		grayDataTemp[iterator_2 * (imageWidth + 2) + iterator_3] := alpha * min(c2, min(min(da + a2 + 1, db + b2 + 1), min( de + e2 + 1, dd + d2 + 1)));
		--	end loop;
		--end loop;
		for iterator in 0 to (imageHeight+1)*(imageWidth+1)-1 loop
			double a  := grayData[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double b  := grayData[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double e  := grayData[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double d  := grayData[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
			double c  := grayData[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
		
			double a2 := grayDataTemp[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double b2 := grayDataTemp[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double e2 := grayDataTemp[((floor(iterator/imageWidth)+1) + 1) * (imageWidth + 2) + ((iterator mod imageWidth) + 1)];
			double d2 := grayDataTemp[((floor(iterator/imageWidth)+1) - 1) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
			double c2 := grayDataTemp[((floor(iterator/imageWidth)+1)    ) * (imageWidth + 2) + ((iterator mod imageWidth)    )];
		
			double da := abs(c - a);
			double db := abs(c - b);
			double de := abs(c - e);
			double dd := abs(c - d);
		
			grayDataTemp[(floor(iterator/imageWidth)+1) * (imageWidth + 2) + (iterator mod imageWidth)] :=
				alpha * min(c2, min(min(da + a2 + 1, db + b2 + 1), min( de + e2 + 1, dd + d2 + 1)));
		end loop;

	end loop;
	double[] transformedData = new double[imageWidth*imageHeight];
	--for iterator_1 in 1 to imageHeight+1 loop
	--	for iterator_2 in 1 to imageWidth+1 loop
	--		transformedData[(iterator_2 - 1) * imageHeight + iterator_1 - 1] := grayDataTemp[iterator_1 * (imageWidth + 2) + iterator_2];
	--	end loop;
	--end loop;
	for iterator in 0 to (imageHeight+1)*(imageWidth+1)-1 loop
		transformedData[((iterator mod imageWidth) - 1) * imageHeight + (floor(iterator/imageWidth)+1) - 1] :=
			grayDataTemp[(floor(iterator/imageWidth)+1) * (imageWidth + 2) + (iterator mod imageWidth)];
	end loop;

	--return transformedData;
}
