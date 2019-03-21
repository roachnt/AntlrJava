import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class StructuredDataCollector {
  public static void structureData(String filePath) {
    // 7 Columns of data
    // Class, method, line, scope, variable, version, value
    BufferedReader reader;
    try {
      // Change name of data file accordingly
      reader = new BufferedReader(new FileReader(filePath));
      String line = reader.readLine();
      HashMap<String, ArrayList<Double>> variableVersionValueArrayMap = new HashMap<>();
      HashMap<String, Double> variableVersionValueMap = new HashMap<>();
      while (line != null) {
        if (line.equals("*** new execution ***")) {
          for (String variableVersion : variableVersionValueMap.keySet()) {
            variableVersionValueArrayMap.putIfAbsent(variableVersion, new ArrayList<>());
            variableVersionValueArrayMap.get(variableVersion).add(variableVersionValueMap.get(variableVersion));
          }
          variableVersionValueMap.clear();
          line = reader.readLine();
          continue;
        }
        String[] row = line.split(",");

        // String className = row[0];
        // String methodName = row[1];
        // String lineNumber = row[2];
        // String scope = row[3];
        String variable = row[4];
        String version = row[5];
        Double value = Double.parseDouble(row[6]);

        variableVersionValueMap.put(variable + "_" + version, value);
        // read next line
        line = reader.readLine();
      }
      reader.close();
      System.out.println(variableVersionValueArrayMap);

      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("newoutput.txt")));

      for (String s : variableVersionValueArrayMap.keySet()) {
        ArrayList<Double> list = variableVersionValueArrayMap.get(s);

        writer.write(String.format("%30s", s));
        for (int i = 0; i < list.size(); i++) {
          writer.write(String.format("%15g", list.get(i)));
        }
        writer.write("\n");
        writer.flush();

      }
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}