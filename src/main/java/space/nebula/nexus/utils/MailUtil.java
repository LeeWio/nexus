package space.nebula.nexus.utils;

import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;

import java.util.Map;

/**
 * Utility class for sending emails, following the project's utility pattern.
 */
@Slf4j
@Component
public class MailUtil
{

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final String from;

	public MailUtil(JavaMailSender mailSender, TemplateEngine templateEngine,
			@Value("${spring.mail.username}") String from)
	{
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.from = from;
	}

	/**
	 * Sends an email using a Thymeleaf template.
	 *
	 * @param to           recipient email
	 * @param subject      email subject
	 * @param templateName name of the template file (without .html)
	 * @param variables    variables to be used in the template
	 */
	public void sendTemplateMail(String to, String subject, String templateName, Map<String, Object> variables)
	{
		Context context = new Context();
		context.setVariables(variables);
		String htmlContent = templateEngine.process("email/" + templateName, context);
		sendHtmlMail(to, subject, htmlContent);
	}

	/**
	 * Sends a simple text email.
	 *
	 * @param to      recipient email
	 * @param subject email subject
	 * @param content email content
	 */
	public void sendSimpleMail(String to, String subject, String content)
	{
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(content);

		try
		{
			mailSender.send(message);
			log.info("Simple email sent to {}", to);
		}
		catch (Exception e)
		{
			log.error("Failed to send simple email to {}", to, e);
			throw new BusinessException(BusinessCode.MAIL_SEND_FAILED,
					"Email delivery failed. Please verify the mail server configuration.");
		}
	}

	/**
	 * Sends an HTML email.
	 *
	 * @param to          recipient email
	 * @param subject     email subject
	 * @param htmlContent HTML email content
	 */
	public void sendHtmlMail(String to, String subject, String htmlContent)
	{
		MimeMessage message = mailSender.createMimeMessage();
		try
		{
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);

			mailSender.send(message);
			log.info("HTML email sent to {}", to);
		}
		catch (Exception e)
		{
			log.error("Failed to send HTML email to {}", to, e);
			throw new BusinessException(BusinessCode.MAIL_SEND_FAILED,
					"Email delivery failed. Please verify the mail server configuration.");
		}
	}
}
