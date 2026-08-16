package com.example.demo.excel;

import com.example.demo.dto.graduate.GraduateResponse;
import com.example.demo.entity.JPromotion;
import java.io.ByteArrayOutputStream;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class GraduateExcelGenerator {

  private static final String[] HEADERS = {"Last Name", "First Name", "Email", "Average"};

  @SneakyThrows
  public byte[] generate(JPromotion promotion, List<GraduateResponse> graduates) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Graduates");
      addTitleRow(sheet, promotion);
      addHeaderRow(sheet, workbook);
      addDataRows(sheet, graduates);
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private void addTitleRow(Sheet sheet, JPromotion promotion) {
    Row title = sheet.createRow(0);
    CellStyle style = sheet.getWorkbook().createCellStyle();
    Font font = sheet.getWorkbook().createFont();
    font.setBold(true);
    style.setFont(font);
    var cell = title.createCell(0);
    cell.setCellValue("Graduates - " + promotion.getRef() + " (" + promotion.getYear() + ")");
    cell.setCellStyle(style);
  }

  private void addHeaderRow(Sheet sheet, XSSFWorkbook workbook) {
    Row header = sheet.createRow(1);
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    for (int i = 0; i < HEADERS.length; i++) {
      var cell = header.createCell(i);
      cell.setCellValue(HEADERS[i]);
      cell.setCellStyle(style);
    }
  }

  private void addDataRows(Sheet sheet, List<GraduateResponse> graduates) {
    int rowIndex = 2;
    for (GraduateResponse graduate : graduates) {
      Row row = sheet.createRow(rowIndex++);
      row.createCell(0).setCellValue(graduate.lastName());
      row.createCell(1).setCellValue(graduate.firstName());
      row.createCell(2).setCellValue(graduate.email());
      row.createCell(3).setCellValue(graduate.average());
    }
    for (int i = 0; i < HEADERS.length; i++) {
      sheet.autoSizeColumn(i);
    }
  }
}
