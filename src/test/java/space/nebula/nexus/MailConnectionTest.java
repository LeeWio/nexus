package space.nebula.nexus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import space.nebula.nexus.utils.MailUtil;

@SpringBootTest(classes = {space.nebula.nexus.config.MockRedisConfig.class, space.nebula.nexus.config.MockRabbitMQConfig.class})
@org.springframework.test.context.ActiveProfiles("test")
public class MailConnectionTest {

	@Autowired
	private MailUtil mailUtil;

	@Test
	public void testMailConnection() {
		System.out.println(">>> Attempting to send a test email to verify configuration...");
		try {
			mailUtil.sendSimpleMail("3499508634@qq.com", "Nexus Mail Configuration Test",
					"If you receive this, your SMTP configuration for Nexus is working perfectly!");
			System.out.println(">>> Test email sent! Please check your inbox (including Spam).");
		} catch (Exception e) {
			System.err.println(">>> FAILED to send email. Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
