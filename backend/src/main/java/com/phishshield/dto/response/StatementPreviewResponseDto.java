package com.phishshield.dto.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatementPreviewResponseDto {
    private String fileName;
    private String fileType;
    private int totalRowsFound;
    private int validRowsCount;
    private int duplicateCount;
    private int invalidCount;
    private boolean isAmbiguous;
    private String ambiguityReason;
    private String confidence;
    private Map<String, String> detectedColumns = new HashMap<>();
    private List<StatementPreviewRowDto> previewRows = new ArrayList<>();
    private List<Map<String, Object>> invalidRows = new ArrayList<>();
    private String notice;

    public StatementPreviewResponseDto() {}

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public int getTotalRowsFound() { return totalRowsFound; }
    public void setTotalRowsFound(int totalRowsFound) { this.totalRowsFound = totalRowsFound; }
    public int getValidRowsCount() { return validRowsCount; }
    public void setValidRowsCount(int validRowsCount) { this.validRowsCount = validRowsCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
    public int getInvalidCount() { return invalidCount; }
    public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }
    public boolean isAmbiguous() { return isAmbiguous; }
    public void setAmbiguous(boolean ambiguous) { isAmbiguous = ambiguous; }
    public String getAmbiguityReason() { return ambiguityReason; }
    public void setAmbiguityReason(String ambiguityReason) { this.ambiguityReason = ambiguityReason; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public Map<String, String> getDetectedColumns() { return detectedColumns; }
    public void setDetectedColumns(Map<String, String> detectedColumns) { this.detectedColumns = detectedColumns; }
    public List<StatementPreviewRowDto> getPreviewRows() { return previewRows; }
    public void setPreviewRows(List<StatementPreviewRowDto> previewRows) { this.previewRows = previewRows; }
    public List<Map<String, Object>> getInvalidRows() { return invalidRows; }
    public void setInvalidRows(List<Map<String, Object>> invalidRows) { this.invalidRows = invalidRows; }
    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }
}
