package com.unicauca.edu.co.financial_statements.infrastructure.out.mail;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
public class FinancialStatementMailService {

    private static final Logger log = LoggerFactory.getLogger(FinancialStatementMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectProvider<Resend> resendProvider;
    private final MailProperties mailProperties;

    public FinancialStatementMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            ObjectProvider<Resend> resendProvider,
            MailProperties mailProperties
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.resendProvider = resendProvider;
        this.mailProperties = mailProperties;
    }

    public void sendReport(
            String toEmail,
            String subject,
            String body,
            byte[] fileContent,
            String fileName,
            MediaType mediaType
    ) throws Exception {
        if ("resend".equalsIgnoreCase(mailProperties.getProvider())) {
            sendWithResend(toEmail, subject, body, fileContent, fileName, mediaType);
            return;
        }

        sendWithSmtp(toEmail, subject, body, fileContent, fileName, mediaType);
    }

    private void sendWithSmtp(
            String toEmail,
            String subject,
            String body,
            byte[] fileContent,
            String fileName,
            MediaType mediaType
    ) throws Exception {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Email service is not configured. Set spring.mail.* properties first.");
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        helper.setFrom(resolveFromEmail());
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false);
        helper.addAttachment(fileName, new ByteArrayResource(fileContent), mediaType.toString());

        mailSender.send(mimeMessage);
    }

    private void sendWithResend(
            String toEmail,
            String subject,
            String body,
            byte[] fileContent,
            String fileName,
            MediaType mediaType
    ) throws Exception {
        if (!StringUtils.hasText(resolveFromEmail())) {
            throw new IllegalStateException("mail.from is required for Resend delivery.");
        }
        Resend resend = resendProvider.getIfAvailable();
        if (resend == null) {
            throw new IllegalStateException("Resend is selected but the Resend client is not configured.");
        }

        Attachment attachment = Attachment.builder()
                .fileName(fileName)
                .content(Base64.getEncoder().encodeToString(fileContent))
                .contentType(mediaType.toString())
                .build();

        CreateEmailOptions.Builder requestBuilder = CreateEmailOptions.builder()
                .from(resolveFromEmail())
                .to(toEmail)
                .subject(subject)
                .text(body)
                .attachments(List.of(attachment));

        if (StringUtils.hasText(mailProperties.getResend().getReplyTo())) {
            requestBuilder.replyTo(mailProperties.getResend().getReplyTo().trim());
        }

        try {
            CreateEmailResponse response = resend.emails().send(requestBuilder.build());
            if (response == null || !StringUtils.hasText(response.getId())) {
                throw new IllegalStateException("Resend responded without an email id.");
            }
            log.info("Financial statement email sent via Resend. emailId={}", response.getId());
        } catch (ResendException exception) {
            throw new IllegalStateException(buildResendErrorMessage(exception), exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Resend email delivery failed: " + exception.getMessage(), exception);
        }
    }

    private String resolveFromEmail() {
        if (StringUtils.hasText(mailProperties.getFrom())) {
            return mailProperties.getFrom().trim();
        }

        return null;
    }

    private String buildResendErrorMessage(ResendException exception) {
        StringBuilder message = new StringBuilder("Resend email delivery failed");

        if (StringUtils.hasText(exception.getMessage())) {
            message.append(": ").append(exception.getMessage());
        }

        if (exception.getStatusCode() > 0) {
            message.append(" (status=").append(exception.getStatusCode()).append(")");
        }

        return message.toString();
    }
}
