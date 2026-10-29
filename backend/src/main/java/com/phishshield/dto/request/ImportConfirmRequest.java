package com.phishshield.dto.request;

import com.phishshield.dto.response.StatementPreviewRowDto;

import java.util.ArrayList;
import java.util.List;

public class ImportConfirmRequest {

    private List<StatementPreviewRowDto> rows = new ArrayList<>();
    private boolean confirmAmbiguous;
    private String fileName;

    public ImportConfirmRequest() {}

    public List<StatementPreviewRowDto> getRows() { return rows; }
    public void setRows(List<StatementPreviewRowDto> rows) { this.rows = rows; }
    public boolean isConfirmAmbiguous() { return confirmAmbiguous; }
    public void setConfirmAmbiguous(boolean confirmAmbiguous) { this.confirmAmbiguous = confirmAmbiguous; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
