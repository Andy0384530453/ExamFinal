package com.example.demo.pdf;

import com.example.demo.entity.TranscriptItem;
import com.example.demo.entity.User;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class TranscriptPdfGenerator {

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
  private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
  private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

  @SneakyThrows
  public File generate(UUID transcriptId, User student, List<TranscriptItem> items) {
    File pdf = File.createTempFile("transcript-" + transcriptId, ".pdf");
    Document document = new Document(PageSize.A4);
    PdfWriter.getInstance(document, new FileOutputStream(pdf));
    document.open();
    addHeader(document, student);
    addTable(document, items);
    document.close();
    return pdf;
  }

  private void addHeader(Document document, User student) {
    Paragraph title = new Paragraph("GRADE TRANSCRIPT", TITLE_FONT);
    title.setAlignment(Element.ALIGN_CENTER);
    document.add(title);
    document.add(new Paragraph(" "));
    document.add(
        new Paragraph(
            "Student: " + student.getFirstName() + " " + student.getLastName(), SUBTITLE_FONT));
    document.add(new Paragraph("Email: " + student.getEmail(), SUBTITLE_FONT));
    document.add(new Paragraph(" "));
  }

  private void addTable(Document document, List<TranscriptItem> items) {
    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);
    table.addCell(headerCell("Course"));
    table.addCell(headerCell("Exam date"));
    table.addCell(headerCell("Coefficient"));
    table.addCell(headerCell("Grade"));
    table.addCell(headerCell("Credits"));
    for (TranscriptItem item : items) {
      table.addCell(cell(item.getCourseTitle()));
      table.addCell(cell(formatExamDate(item)));
      table.addCell(cell(formatCoefficient(item)));
      table.addCell(cell(formatGrade(item)));
      table.addCell(cell(String.valueOf(item.getCredits())));
    }
    document.add(table);
  }

  private PdfPCell headerCell(String text) {
    PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    return cell;
  }

  private PdfPCell cell(String text) {
    return new PdfPCell(new Phrase(text));
  }

  private String formatExamDate(TranscriptItem item) {
    if (item.getExamDate() == null) {
      return "";
    }
    return DateTimeFormatter.ISO_INSTANT.format(item.getExamDate());
  }

  private String formatCoefficient(TranscriptItem item) {
    return item.getCoefficient() == null ? "" : String.valueOf(item.getCoefficient());
  }

  private String formatGrade(TranscriptItem item) {
    return item.getGrade() == null ? "" : String.valueOf(item.getGrade());
  }
}
