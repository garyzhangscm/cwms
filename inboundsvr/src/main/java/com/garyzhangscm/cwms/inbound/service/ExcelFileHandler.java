package com.garyzhangscm.cwms.inbound.service;

import com.garyzhangscm.cwms.inbound.model.FileUploadType;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelFileHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExcelFileHandler.class);

    @Autowired
    private FileService fileService;

    public List<List<String>> getData(File file) throws IOException, InvalidFormatException {
        FileInputStream fileInputStream = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(fileInputStream);

        Sheet sheet = workbook.getSheetAt(0);

        // 2D list
        List<List<String>> data = new ArrayList<>();
        for (Row row : sheet) {
            List<String>  dataRow = new ArrayList<>();
            for (Cell cell : row) {
                dataRow.add(cell.getStringCellValue());
            }
            data.add(dataRow);
        }
        return data;
    }

    /**
     * Convert the first sheet from the excel file and save it as CSV
     * @param excelFile
     * @return
     */
    public File convertExcelToCSV(File excelFile, FileUploadType fileUploadType) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(excelFile);
        Workbook workbook = new XSSFWorkbook(fileInputStream);

        Sheet sheet = workbook.getSheetAt(0);

        // save the column name as a map
        //
        Map<Integer, String> columnNameMap = new HashMap<>();

        Iterator<Row> rowIterator = sheet.iterator();
        StringBuilder data = new StringBuilder();

        // row index. we will use it to find the column name(1st row)
        int rowIndex = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            // Note: cell iterator will skip the empty cells, so we
            // we will need to loop through each column instead of
            // using the iterator
            // Iterator<Cell> cellIterator = row.cellIterator();

            int lastColumn = Math.max(row.getLastCellNum(), 0);
            for (int columnNumber = 0; columnNumber < lastColumn; columnNumber++) {
                Cell cell = row.getCell(columnNumber, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell != null) {
                    // special handling for date
                    // excel will treat date as number, we can get the actual type from the
                    // definition in FileUploadType
                    String columnName =
                            rowIndex == 0 ? cell.getStringCellValue() : columnNameMap.getOrDefault(columnNumber, "");
                    columnNameMap.put(columnNumber, columnName);
                    // skip date check for the first row
                    Class columnType = rowIndex == 0 ? String.class : fileUploadType.getColumnType(columnName);


                    logger.debug("The column type for {} is {}",
                            columnName, columnType);
                    if (columnType == ZonedDateTime.class || columnType == LocalDateTime.class  ) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                        data.append("\"")
                                .append(cell.getLocalDateTimeCellValue().format(formatter))
                                .append("\"");
                    }
                    else if (columnType == LocalDate.class  ) { 
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        data.append("\"")
                                .append(cell.getLocalDateTimeCellValue().format(formatter))
                                .append("\"");
                    }
                    else {
                        CellType type = cell.getCellType();
                        // if type is formula, then get the result type
                        if (type == CellType.FORMULA) {
                            type = cell.getCachedFormulaResultType();
                            logger.debug("Current column {} is formula, let's get the result type {}",
                                    columnName, type);
                        }
                        if (type == CellType.BOOLEAN) {
                            data.append(cell.getBooleanCellValue());
                        } else if (type == CellType.NUMERIC) {
                            data.append(formatNumericValue(cell.getNumericCellValue()));
                        } else if (type == CellType.STRING) {
                            String cellValue = cell.getStringCellValue();
                            if (!cellValue.isEmpty()) {
                                cellValue = cellValue.replaceAll("\"", "\"\"");
                                data.append("\"").append(cellValue).append("\"");
                            }
                        } else {
                            data.append(cell + "");
                        }
                    }
                }

                if(columnNumber != lastColumn - 1) {
                        data.append(",");
                }

            }
            data.append('\n');

            rowIndex++;
        }

        String csvFileName = FilenameUtils.removeExtension(excelFile.getName()) + ".csv";
        return fileService.saveCSVFile(csvFileName, data.toString());

    }


    /**
     * POI always treat numeric value as float, we may need to remove the ending 0 if
     * it is actually an int or long
     * @param numericCellValue
     * @return
     */
    private String formatNumericValue(double numericCellValue) {
        String stringValue = String.valueOf(numericCellValue);
        if (stringValue.contains(".")) {
            String[] stringTokens = stringValue.split("\\.");

            if (stringTokens.length == 2 ) {
                try {
                    int result = Integer.parseInt(stringTokens[1]);
                    if (result == 0) {
                        // return the integer part only if the
                        // fraction part is all 0
                        return stringTokens[0];
                    }
                } catch (NumberFormatException e) {

                    e.printStackTrace();
                }

            }
        }

        return stringValue;
    }
}
