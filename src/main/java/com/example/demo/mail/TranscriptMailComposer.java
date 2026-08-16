package com.example.demo.mail;

import com.example.demo.entity.JUser;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TranscriptMailComposer {

  private static final String SUBJECT = "Your grade transcript";
  private static final String HTML_TEMPLATE =
      "<p>Hi,</p><p>Here is your grade transcript:</p><p><a href=\"%s\">Download your"
          + " transcript</a></p>";

  public Email buildTranscriptEmail(JUser student, String pdfUrl) throws Exception {
    return new Email(
        new InternetAddress(student.getEmail()),
        List.of(),
        List.of(),
        SUBJECT,
        String.format(HTML_TEMPLATE, pdfUrl),
        List.of());
  }
}
