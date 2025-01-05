package com.example.markscalculator.utils;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.example.markscalculator.models.StudentData;

public class ExcelHandler {
    private static final String TAG = "ExcelHandler";
    private Workbook workbook;
    private Sheet sheet;
    private final Context context;
    private final Uri excelFileUri;
    private int totalRows = 0;

    public ExcelHandler(Context context, Uri fileUri) {
        this.context = context;
        this.excelFileUri = fileUri;
        loadWorkbook();
    }

    private void loadWorkbook() {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(excelFileUri);
            if (inputStream != null) {
                workbook = new XSSFWorkbook(inputStream);
                sheet = workbook.getSheetAt(0); // Get first sheet
                totalRows = sheet.getLastRowNum();
                inputStream.close();
                Log.d(TAG, "Workbook loaded successfully. Total rows: " + totalRows);
            } else {
                Log.e(TAG, "Failed to open input stream");
                throw new IOException("Could not open excel file");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading workbook: " + e.getMessage());
            throw new RuntimeException("Failed to load Excel file", e);
        }
    }

    public StudentData getStudentData(int rowIndex) {
        try {
            if (rowIndex < 1 || rowIndex > totalRows) {
                Log.e(TAG, "Invalid row index: " + rowIndex);
                return null;
            }

            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                String name = getCellValueAsString(row.getCell(0));
                String usn = getCellValueAsString(row.getCell(1));
                double exam1 = getCellValueAsDouble(row.getCell(2));
                double exam2 = getCellValueAsDouble(row.getCell(3));
                double exam3 = getCellValueAsDouble(row.getCell(4));
                double aat = getCellValueAsDouble(row.getCell(5));

                return new StudentData(name, usn, exam1, exam2, exam3, aat);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading student data at row " + rowIndex + ": " + e.getMessage());
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    return String.valueOf(cell.getNumericCellValue());
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                default:
                    return "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading cell value: " + e.getMessage());
            return "";
        }
    }

    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    return Double.parseDouble(cell.getStringCellValue());
                default:
                    return 0.0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading numeric cell value: " + e.getMessage());
            return 0.0;
        }
    }

    public void updateMarks(int rowIndex, double exam1, double exam2, double exam3, double aat) {
        try {
            if (rowIndex < 1 || rowIndex > totalRows) {
                Log.e(TAG, "Invalid row index for update: " + rowIndex);
                return;
            }

            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                Log.e(TAG, "Row not found: " + rowIndex);
                return;
            }

            // Update exam marks
            updateCell(row, 2, exam1);
            updateCell(row, 3, exam2);
            updateCell(row, 4, exam3);
            updateCell(row, 5, aat);

            // Calculate and update total
            double total = exam1 + exam2 + exam3;
            updateCell(row, 6, total);

            // Calculate and update average (out of 30)
            double average = (total / 50)*10;
            updateCell(row, 7, average);

            // Calculate and update final marks
            double finalMarks = average + aat;
            updateCell(row, 8, finalMarks);

            Log.d(TAG, "Marks updated successfully for row " + rowIndex);
        } catch (Exception e) {
            Log.e(TAG, "Error updating marks: " + e.getMessage());
        }
    }

    private void updateCell(Row row, int columnIndex, double value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(value);
    }

    public void saveWorkbook() {
        try {
            ParcelFileDescriptor pfd = context.getContentResolver()
                    .openFileDescriptor(excelFileUri, "rw");
            if (pfd != null) {
                FileOutputStream fileOutputStream =
                        new FileOutputStream(pfd.getFileDescriptor());
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                pfd.close();
                Log.d(TAG, "Workbook saved successfully");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving workbook: " + e.getMessage());
            throw new RuntimeException("Failed to save Excel file", e);
        }
    }

    public boolean hasNextStudent(int currentRow) {
        return currentRow < totalRows;
    }

    public boolean hasPreviousStudent(int currentRow) {
        return currentRow > 1;
    }

    public void close() {
        try {
            if (workbook != null) {
                workbook.close();
                Log.d(TAG, "Workbook closed successfully");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing workbook: " + e.getMessage());
        }
    }
}