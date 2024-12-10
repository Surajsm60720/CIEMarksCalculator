package com.example.markscalculator.utils;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.example.markscalculator.models.StudentData;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CsvHandler {
    private final Context context;
    private final Uri fileUri;
    private final List<String[]> csvData = new ArrayList<>();
    private static final String CSV_SEPARATOR = ",";
    private static final int EXPECTED_COLUMNS = 5; // Name, USN, Exam1, Exam2, Exam3

    public CsvHandler(Context context, Uri fileUri) throws IOException {
        this.context = context;
        this.fileUri = fileUri;
        loadCsvFile();
    }

    private void loadCsvFile() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getContentResolver().openInputStream(fileUri)))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] row = line.split(CSV_SEPARATOR, -1); // -1 to keep empty values
                
                // Validate row data
                if (row.length != EXPECTED_COLUMNS) {
                    throw new IOException("Invalid CSV format at line " + lineNumber + 
                            ". Expected " + EXPECTED_COLUMNS + " columns, found " + row.length);
                }
                
                // Trim whitespace from each field
                for (int i = 0; i < row.length; i++) {
                    row[i] = row[i].trim();
                }
                
                csvData.add(row);
            }
        }
    }

    @NonNull
    public StudentData getStudentData(int row) throws IllegalArgumentException {
        if (row < 1 || row >= csvData.size()) {
            throw new IllegalArgumentException("Invalid row index: " + row);
        }
        
        String[] data = csvData.get(row);
        return new StudentData(
            validateField(data[0], "Name", row),  // name
            validateField(data[1], "USN", row),   // usn
            parseDouble(data[2], "Exam1", row),   // exam1
            parseDouble(data[3], "Exam2", row),   // exam2
            parseDouble(data[4], "Exam3", row)    // exam3
        );
    }

    private String validateField(String value, String fieldName, int row) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Empty %s at row %d", fieldName, row));
        }
        return value.trim();
    }

    private double parseDouble(String value, String fieldName, int row) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (parsed < 0 || parsed > 100) {
                throw new IllegalArgumentException(
                    String.format("Invalid %s mark at row %d: must be between 0 and 100", fieldName, row));
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Invalid %s mark format at row %d: %s", fieldName, row, value));
        }
    }

    public void updateMarks(int row, double exam1, double exam2, double exam3) {
        if (row < 1 || row >= csvData.size()) {
            throw new IllegalArgumentException("Invalid row index: " + row);
        }

        String[] data = csvData.get(row);
        // Validate the original row has correct number of columns
        if (data.length != EXPECTED_COLUMNS) {
            throw new IllegalStateException("Corrupted data row: " + row);
        }

        // Format with 2 decimal places for consistency
        data[2] = String.format("%.2f", exam1);
        data[3] = String.format("%.2f", exam2);
        data[4] = String.format("%.2f", exam3);
    }

    public void saveFile() throws IOException {
        // First save to a temporary string to verify it's valid
        StringBuilder tempBuffer = new StringBuilder();
        for (String[] row : csvData) {
            if (row.length != EXPECTED_COLUMNS) {
                throw new IOException("Invalid data detected before saving");
            }
            tempBuffer.append(String.join(CSV_SEPARATOR, row)).append("\n");
        }

        // Only if the above succeeds, write to file
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                    context.getContentResolver().openOutputStream(fileUri, "wt")))) {
            writer.write(tempBuffer.toString());
            writer.flush();
        }
    }

    public boolean hasPreviousStudent(int currentRow) {
        return currentRow > 1;
    }

    public boolean hasNextStudent(int currentRow) {
        return currentRow < csvData.size() - 1;
    }

    public void close() {
        // Clean up any resources if needed
    }
} 