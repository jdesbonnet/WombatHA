package ie.wombat.ha.misc;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.apache.commons.math.optimization.fitting.ParametricRealFunction;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

/**
 * Have a long log file of temperature in living room, kitchen and outside. 
 * Want to see if I can measure how good the insulation is.
 * 
 * Newton's Cooling Law:
 * T(t) = Ta + (To - Ta) e ^ -kt
 * where Ta = ambient temperature, To = T(0) and k 
 * 
 * 
 * Run with:
 * java -cp ./WEB-INF/classes:./WEB-INF/lib/commons-math-2.1.jar \
 * ie.wombat.ha.InsulationExperiment \
 * /home/joe/workspace/RealTime/scripts/h.h 4 7 300 > y.y
 * 
 * Use ImageMagick to produce tiled image of charts:
 * montage ser.*.png -geometry 160x100 -tile 4x16 outimage.jpg
 * 
 *
 * Col#4 = Kitchen
 * Col#5 = Living room
 * @author joe
 *
 */
public class InsulationExperiment {

	private static final String FILE_PREFIX = "ser.";
	private static final int MIN_SERIES_LENGTH = 20;
	
	public static void main (String[] arg) throws Exception {
		
		File dataFile = new File(arg[0]);
		int inCol = Integer.parseInt(arg[1]);
		int outCol = Integer.parseInt(arg[2]);
		int binSize = Integer.parseInt(arg[3]);
		
		long time=0,startTime=0,endTime=0;
		
		// Pass one: get start and end time
		
		LineNumberReader lnr = new LineNumberReader(new FileReader(dataFile));
		String line ;
		float temp,firstTemp = -999;
		while ( (line = lnr.readLine()) != null ) {
			String[] p = line.split(" ");
			time = Long.parseLong(p[0]);
			if (startTime == 0) {
				startTime = time;
			}
			if (firstTemp == -999) {
				firstTemp = Float.parseFloat(p[inCol]);
			}
		}
		lnr.close();
		
		endTime = time;
		int interval = (int)(endTime - startTime);
		
		System.err.println ("startTime=" + startTime + " endTime=" + endTime);
		System.err.println ("interval=" + (interval/3600));
		
		
		int nbin = interval / binSize + 1;
		System.err.println ("nbin=" + nbin);
		
		float[] insideTemp = new float[nbin];
		float[] outsideTemp = new float[nbin];
		int[] rcount = new int[nbin];
		
		// Pass two:
		int bin;
		float lastTemperature=firstTemp,dt;
		lnr = new LineNumberReader(new FileReader(dataFile));
		while ( (line = lnr.readLine()) != null ) {
			String[] p = line.split(" ");
			time = Long.parseLong(p[0]);
			bin = (int)(time - startTime)/binSize;
			temp = Float.parseFloat(p[inCol]);
			dt = temp - lastTemperature;
			if (dt > 1.0 || dt < -1.0) {
				continue;
			}
			insideTemp[bin] += temp;
			outsideTemp[bin] += Float.parseFloat(p[outCol]);
			rcount[bin]++;
			lastTemperature = temp;
			//goodRecords++;
		}
		lnr.close();
	
		for (int i = 0; i < nbin; i++) {
			if (rcount[i] == 0) {
				continue;
			}
			insideTemp[i] /= rcount[i];
			outsideTemp[i] /= rcount[i];
		}
		
		// Pass 3: only interested in temperatures where it's dropping
		int startIndex = 0;
		ArrayList<float[]> series = new ArrayList<float[]>();
		ArrayList<Float>meanOutsideTempForSeries = new ArrayList<Float>();
		for (int i = 1 ; i < nbin; i++) {
			if (rcount[i] == 0) {
				saveSeries (series, meanOutsideTempForSeries, insideTemp, outsideTemp, rcount, startIndex, i);
				startIndex = i;
				continue;
			}
			
			dt = insideTemp[i] - insideTemp[i-1];
			if (dt > 0) {
				saveSeries (series, meanOutsideTempForSeries, insideTemp, outsideTemp, rcount, startIndex, i);
				startIndex = i;
				continue;	
			}
			
		}
		
		double sumk=0;
		double sumk2=0;
		int nk=0;
		
		for (int i = 0; i < series.size(); i++) {
			
			float[] ser = series.get(i);
			
			LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
			CurveFitter fitter = new CurveFitter(optimizer);
			
			for (int j = 0; j < ser.length; j++) {
				fitter.addObservedPoint((double)(j*binSize - binSize/2), (double)ser[j]);
			}
			
			double Ta = meanOutsideTempForSeries.get(i);
			double To = ser[0];
			
			/*
			double[] guess = new double[1];
			guess[0] = 0.001;
			*/
		
			/*
			double[] guess = new double[2];
			guess[0] = Ta;
			guess[1] = 0.001;
			*/
				
			double[] guess = new double[3];
			//guess[0] = Ta;
			//guess[1] = To-Ta;
			//guess[2] = 0.001;
			guess[0] = 0;
			guess[1] = 15;
			guess[2] = 1E-3;
			
			try {
				ParametricRealFunction fn = new CoolingFunction3();
				//ParametricRealFunction fn = new CoolingFunction2(To);
				//ParametricRealFunction fn = new CoolingFunction(To,Ta);
				
				double[] bestCoef = fitter.fit(fn, guess);
				System.out.print(To + " " + Ta + " ");
				for (int j = 0; j < bestCoef.length; j++) {
					System.out.print (" " + bestCoef[j]);
				}
				System.out.print (" " + (bestCoef[0] - bestCoef[1]));
				System.out.println ("");
				
				sumk += bestCoef[2];
				sumk2 += bestCoef[2]*bestCoef[2];
				nk++;
				
				File serDataFile = new File (FILE_PREFIX + i + ".dat");
				FileWriter w = new FileWriter(serDataFile);
				for (int j = 0; j < ser.length; j++) {
					w.write("" + (j*binSize - binSize/2) + " " + ser[j] + "\n");
				}
				w.close();
				
				File seqPlotFile = new File (FILE_PREFIX + i + ".gp");
				w = new FileWriter(seqPlotFile);
				w.write ("set terminal png size 320,200\n");
				w.write ("set output '" + FILE_PREFIX + i + ".png' \n");
				
				//w.write ("set title \"Series #" + i + "\"\n");
				w.write ("set ytic 2\n");
				
				int xmax = ser.length*binSize;
				w.write ("set xrange [0:" + xmax + "]\n");
				if (xmax < 11000) {
					w.write ("set xtic 3600\n"); // 1 hour
				} else {
					w.write ("set xtic 7200\n"); // 2 hour
				}
				//w.write ("set xdata time\n");
				//w.write ("set timefmt \"%s\"\n");
				//w.write ("set format x \"%H:%M\"\n");
				//w.write ("set timefmt \"%s\"\n");
				
				
				//w.write ("unset xtic\n");
				
				w.write ("set grid\n");
				w.write ("plot '" + serDataFile.getName() + "' title ''");
				
				w.write (" , ");
				w.write (String.format("%.4g",bestCoef[0]));
				w.write ("+");
				w.write (String.format("%.4g",bestCoef[1]));
				w.write ("*exp(-");
				w.write (String.format("%.4e",bestCoef[2])); 
				w.write ("*x)");
				w.write ("lt 2");
				w.write ("title 'k = " + String.format("%.3e",bestCoef[2]) + "'");
				w.write ("\n"); 
				w.close();
				
			} catch (Exception e) {
				System.err.println (e.toString());
			}
		}
		
		System.err.println ("meank=" + (sumk/nk));
		double variance = (sumk2 - sumk*sumk/nk)/(nk-1);
		double std = Math.sqrt(variance);
		System.err.println ("std = " + std );
	}
	private static void saveSeries (List<float[]> series, List<Float> meanOutsideTempForSeries, float[] insideTemp, float[] outsideTemp, int[] rcount, int start, int end) {
		int i, nrec=0;
		for (i = start; i < end; i++) {
			if (rcount[i] > 0) {
				nrec++;
			}
		}
		
		if (nrec < MIN_SERIES_LENGTH) {
			return;
		}
		
		float[] seq = new float[nrec];
		
		nrec = 0;
		float meanOutsideTemp = 0;
		for (i = start; i < end; i++) {
			if (rcount[i] > 0) {
				seq[nrec++] = insideTemp[i];
				meanOutsideTemp += outsideTemp[i];
			}
		}
		series.add(seq);
		meanOutsideTemp /= (float)nrec;
		meanOutsideTempForSeries.add(meanOutsideTemp);
	}
	
	private static class CoolingFunction implements ParametricRealFunction {

		private double Ta,To;
		public CoolingFunction (double To, double Ta) {
			this.To = To;
			this.Ta = Ta;
		}
		public double[] gradient(double x, double[] p)
				throws FunctionEvaluationException {
			double[] grad = new double[1];
			grad[0] =  1 + (To-Ta) * (-x) * Math.exp(-p[0]*x); 
			return grad;
		}

		public double value(double x, double[] p)
				throws FunctionEvaluationException {
			
			return Ta + (To-Ta)*Math.exp(-p[0]*x);
		}
		
	}
	
	private static class CoolingFunction2 implements ParametricRealFunction {

		private double To;
		public CoolingFunction2 (double To) {
			this.To = To;
		}
		
		public double[] gradient(double x, double[] p)
				throws FunctionEvaluationException {
			double[] grad = new double[3];
			grad[0] =  1 - Math.exp(-p[1]*x);
			grad[1] = (To-p[0]) * -x * Math.exp(-p[1]*x);
			return grad;
		}

		public double value(double x, double[] p)
				throws FunctionEvaluationException {
			return p[0] + (To-p[0])*Math.exp(-p[1]*x);
		}
		
	}
	
	private static class CoolingFunction3 implements ParametricRealFunction {

		public double[] gradient(double x, double[] p)
				throws FunctionEvaluationException {
			double[] grad = new double[3];
			grad[0] = 1 ;
			grad[1] = Math.exp(-p[2]*x);
			grad[2] = p[1]*(-x)*Math.exp(-p[2]*x);
			return grad;
		}

		public double value(double x, double[] p)
				throws FunctionEvaluationException {
			return p[0] + p[1]*Math.exp(-p[2]*x);
		}
		
	}
}
