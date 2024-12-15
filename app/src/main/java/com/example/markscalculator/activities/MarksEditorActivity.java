package com.example.markscalculator.activities;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.markscalculator.R;
import com.example.markscalculator.models.StudentData;
import com.example.markscalculator.utils.ExcelHandler;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MarksEditorActivity extends AppCompatActivity {
    private TextView nameTextView;
    private TextView usnTextView;
    private EditText exam1EditText;
    private EditText exam2EditText;
    private EditText exam3EditText;
    private TextView totalTextView;
    private TextView averageTextView;
    private Button calculateButton;
    private Button previousButton;
    private Button submitButton;
    private ProgressBar progressBar;

    private ExcelHandler excelHandler;
    private int currentRow = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean hasUnsavedChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marks_editor);

        initializeViews();
        setupExcelHandler();
        setupListeners();
    }

    private void initializeViews() {
        nameTextView = findViewById(R.id.nameTextView);
        usnTextView = findViewById(R.id.usnTextView);
        exam1EditText = findViewById(R.id.exam1EditText);
        exam2EditText = findViewById(R.id.exam2EditText);
        exam3EditText = findViewById(R.id.exam3EditText);
        totalTextView = findViewById(R.id.totalTextView);
        averageTextView = findViewById(R.id.averageTextView);
        calculateButton = findViewById(R.id.calculateButton);
        previousButton = findViewById(R.id.previousButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupExcelHandler() {
        Uri fileUri = getIntent().getData();
        if (fileUri == null) {
            showError("No file selected");
            finish();
            return;
        }

        executor.execute(() -> {
            try {
                excelHandler = new ExcelHandler(this, fileUri);
                mainHandler.post(this::loadCurrentStudent);
            } catch (Exception e) {
                mainHandler.post(() -> showError("Error loading Excel file: " + e.getMessage()));
            }
        });
    }

    private void setupListeners() {
        TextWatcher markChangeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                hasUnsavedChanges = true;
                calculateButton.setEnabled(true);
            }
        };

        exam1EditText.addTextChangedListener(markChangeWatcher);
        exam2EditText.addTextChangedListener(markChangeWatcher);
        exam3EditText.addTextChangedListener(markChangeWatcher);

        calculateButton.setOnClickListener(v -> calculateMarks());
        previousButton.setOnClickListener(v -> navigateToStudent(currentRow - 1));
        submitButton.setOnClickListener(v -> saveChangesAndContinue());
    }

    private void loadCurrentStudent() {
        showLoading(true);
        executor.execute(() -> {
            try {
                StudentData student = excelHandler.getStudentData(currentRow);
                mainHandler.post(() -> {
                    if (student != null) {
                        displayStudentData(student);
                        updateNavigationButtons();
                    } else {
                        showError("Error loading student data");
                    }
                    showLoading(false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showError("Error: " + e.getMessage());
                    showLoading(false);
                });
            }
        });
    }

    private void displayStudentData(StudentData student) {
        nameTextView.setText(getString(R.string.name_format, student.getName()));
        usnTextView.setText(getString(R.string.usn_format, student.getUsn()));
        exam1EditText.setText(String.valueOf(student.getExam1()));
        exam2EditText.setText(String.valueOf(student.getExam2()));
        exam3EditText.setText(String.valueOf(student.getExam3()));
        updateCalculations(student.getTotal(), student.getAverage());
    }

    private void calculateMarks() {
        try {
            double exam1 = parseMarkInput(exam1EditText);
            double exam2 = parseMarkInput(exam2EditText);
            double exam3 = parseMarkInput(exam3EditText);

            double total = exam1 + exam2 + exam3;
            double average = total / 5;

            updateCalculations(total, average);
            hasUnsavedChanges = true;
        } catch (NumberFormatException e) {
            showError("Please enter valid marks");
        }
    }

    private double parseMarkInput(EditText editText) throws NumberFormatException {
        String input = editText.getText().toString().trim();
        if (input.isEmpty())
            return 0.0;
        double value = Double.parseDouble(input);
        if (value < 0 || value > 50) {
            throw new NumberFormatException("Marks must be between 0 and 50");
        }
        return value;
    }

    private void updateCalculations(double total, double average) {
        totalTextView.setText(getString(R.string.total_format, total));
        averageTextView.setText(getString(R.string.average_format, average));
    }

    private void navigateToStudent(int newRow) {
        if (hasUnsavedChanges) {
            showSaveChangesDialog(() -> {
                currentRow = newRow;
                loadCurrentStudent();
            });
        } else {
            currentRow = newRow;
            loadCurrentStudent();
        }
    }

    private void updateNavigationButtons() {
        previousButton.setEnabled(excelHandler.hasPreviousStudent(currentRow));
        submitButton.setEnabled(excelHandler.hasNextStudent(currentRow));
    }

    private void saveChangesAndContinue() {
        showLoading(true);
        executor.execute(() -> {
            try {
                double exam1 = parseMarkInput(exam1EditText);
                double exam2 = parseMarkInput(exam2EditText);
                double exam3 = parseMarkInput(exam3EditText);

                excelHandler.updateMarks(currentRow, exam1, exam2, exam3);
                excelHandler.saveWorkbook();

                mainHandler.post(() -> {
                    showLoading(false);
                    hasUnsavedChanges = false;
                    showSaveSuccessDialog();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showError("Error saving changes: " + e.getMessage());
                    showLoading(false);
                });
            }
        });
    }

    private void showSaveSuccessDialog() {
        if (excelHandler.hasNextStudent(currentRow)) {
            new AlertDialog.Builder(this)
                    .setTitle("Success")
                    .setMessage("Changes saved successfully!")
                    .setPositiveButton("Next Student", (dialog, which) -> {
                        navigateToStudent(currentRow + 1);
                    })
                    .setNegativeButton("Stay Here", null)
                    .setCancelable(false)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Success")
                    .setMessage("Changes saved successfully! This is the last student.")
                    .setPositiveButton("OK", null)
                    .setCancelable(false)
                    .show();
        }
    }

    private void showSaveChangesDialog(Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Do you want to save your changes?")
                .setPositiveButton("Save", (dialog, which) -> {
                    saveChangesAndContinue();
                    onConfirm.run();
                })
                .setNegativeButton("Discard", (dialog, which) -> {
                    hasUnsavedChanges = false;
                    onConfirm.run();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content),
                message, Snackbar.LENGTH_LONG).show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        exam1EditText.setEnabled(!show);
        exam2EditText.setEnabled(!show);
        exam3EditText.setEnabled(!show);
        calculateButton.setEnabled(!show);
        previousButton.setEnabled(!show);
        submitButton.setEnabled(!show);
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            showSaveChangesDialog(() -> super.onBackPressed());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (excelHandler != null) {
            excelHandler.close();
        }
    }
}