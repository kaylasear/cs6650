package assignment1.part2;

import assignment1.part2.model.SystemStats;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a CSV writer that creates and outputs a CSV file.
 */
public class CsvWriter {
    private final String filePath = "./src/main/java/assignment1/part2/";

    /**
     * Create and write to a CSV file containing information
     * on system stats
     * @param list of system stats objects
     * @param fileName string to name the file
     */
    public void writeToCsvFile(ArrayList<SystemStats> list, String fileName) {
        File file = new File(filePath + fileName);

        try {
            FileWriter outputFile = new FileWriter(file);

            outputFile.append("StartTime");
            outputFile.append(",");
            outputFile.append("RequestType");
            outputFile.append(",");
            outputFile.append("Latency");
            outputFile.append(",");
            outputFile.append("ResponseCode");
            outputFile.append("\n");

            for (SystemStats stat : list) {
                List<String> rowData = Arrays.asList(String.valueOf(stat.getStartTime()), stat.getRequestType(), String.valueOf(stat.getLatency()), String.valueOf(stat.getResponseCode()));
                outputFile.append(String.join(",", rowData));
                outputFile.append("\n");
            }

            outputFile.flush();
            outputFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
