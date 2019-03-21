package SciMark;

import java.io.FileWriter;
import java.io.IOException;

public class MainTestFile {

  public static void main(String[] args) throws IOException {

    FileWriter writer = new FileWriter("/Users/Nebula/IdeaProjects/SootTest2/outY.txt");

    int[] Y = new int[2000];

    FileWriter writerOut = new FileWriter("/Users/Nebula/IdeaProjects/SootTest2/output", true);

    for (int i = 0; i < 2000; i++) {

      writerOut.write("===============start of a new class===============" + "\n");
      writerOut.flush();

      double[] data = FFT.makeRandom(64);
      double[] dataFault = new double[data.length];
      System.arraycopy(data, 0, dataFault, 0, data.length);

      //            System.out.println("data before bitreverse, FFT: " + FFT.test(data));
      //            System.out.println("data before bitreverse, FFTFault: " + FFTFault.test(dataFault));

      // For bitreverse
      //            FFT.bitreverse(data);
      //            FFTFault.bitreverse(dataFault);

      // For transform
      FFT.transform(data);
      FFTFault.transform(dataFault);

      for (int j = 0; j < data.length; j++) {
        if (data[j] != dataFault[j])
          Y[i] = 1;
      }

      //            double result = FFT.test(data);
      //            double resultFault = FFTFault.test(dataFault);
      //            if (result == resultFault) Y[i] = 0;
      //            else Y[i] = 1;

      //            System.out.println("data after bitreverse, FFT: " + result);
      //            System.out.println("data after bitreverse, FFTFault: " + resultFault);

      writerOut.write("===============end of a new class===============" + "\n");
      writerOut.flush();
    }

    writerOut.close();

    for (int i : Y) {
      System.out.println(i + " ");
      writer.write(i + "  ");
      writer.flush();
    }
    writer.close();
  }
}
