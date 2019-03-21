import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MainTestFile {

  public static void main(String[] args) throws IOException {

    FileWriter writer = new FileWriter("outY.txt");

    // Change file name accordingly
    String dataFileName = "output";

    PrintWriter pw = new PrintWriter(dataFileName);
    pw.close();
    FileWriter writerOut = new FileWriter(dataFileName, true);

    int numTrials = 2000;

    int[] Y = new int[numTrials];

    for (int i = 0; i < numTrials; i++) {

      writerOut.write("*** new execution ***" + "\n");
      writerOut.flush();

      // double[] data = FFT.makeRandom(64);
      // double[] dataFault = new double[data.length];
      // System.arraycopy(data, 0, dataFault, 0, data.length);

      //            System.out.println("data before bitreverse, FFT: " + FFT.test(data));
      //            System.out.println("data before bitreverse, FFTFault: " + FFTFault.test(dataFault));

      // For bitreverse
      //            FFT.bitreverse(data);
      //            FFTFault.bitreverse(dataFault);

      // For transform
      // FFT.transform(data);
      // FFTFault.transform(dataFault);

      // for (int j = 0; j < data.length; j++) {
      //   if (data[j] != dataFault[j])
      //     Y[i] = 1;
      // }

      //            double result = FFT.test(data);
      //            double resultFault = FFTFault.test(dataFault);
      //            if (result == resultFault) Y[i] = 0;
      //            else Y[i] = 1;

      //            System.out.println("data after bitreverse, FFT: " + result);
      //            System.out.println("data after bitreverse, FFTFault: " + resultFault);

      writerOut.flush();
    }

    writerOut.close();

    for (int i : Y) {
      System.out.println(i + " ");
      writer.write(i + "  ");
      writer.flush();
    }
    writer.close();
    StructuredDataCollector.structureData(dataFileName);
  }
}
