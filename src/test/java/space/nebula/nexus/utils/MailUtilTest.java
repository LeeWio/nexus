package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MailUtilTest {

	@InjectMocks
	private MailUtil mailUtil;

	@Mock
	private JavaMailSender mailSender;

	@Test
	public void testSendSimpleMail() {
		ReflectionTestUtils.setField(mailUtil, "from", "admin@nexus.com");

		mailUtil.sendSimpleMail("test@example.com", "Subject", "Content");
		verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
	}

	@Test
	public void testSendHtmlMail() {
		ReflectionTestUtils.setField(mailUtil, "from", "admin@nexus.com");
		when(mailSender.createMimeMessage()).thenReturn(org.mockito.Mockito.mock(MimeMessage.class));

		mailUtil.sendHtmlMail("test@example.com", "Subject", "<h1>Content</h1>");
		verify(mailSender, times(1)).send(any(MimeMessage.class));
	}
}
