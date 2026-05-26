package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.repository.KanbanItemRepository;

import cn.hutool.core.lang.Dict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KanbanReminderTask {

	private final KanbanItemRepository itemRepository;
	private final RabbitTemplate rabbitTemplate;

	@Value("${app.admin-email:your-email@example.com}")
	private String adminEmail;

	/**
	 * Scan for kanban items that have a reminder set and are due. Runs every
	 * minute.
	 */
	@Scheduled(cron = "0 * * * * *")
	@Transactional
	public void checkReminders() {
		LocalDateTime now = LocalDateTime.now();
		List<KanbanItem> dueItems = itemRepository.findByReminderAtBefore(now);

		for (KanbanItem item : dueItems) {
			sendReminderEmail(item);

			// Clear reminder to avoid duplicate notifications
			item.setReminderAt(null);
			itemRepository.save(item);
		}
	}

	private void sendReminderEmail(KanbanItem item) {
		String subject = "Kanban Task Reminder: " + item.getTitle();

		// CSS class for priority badge
		String priorityClass = switch (item.getPriority()) {
			case HIGH -> "priority-high";
			case MEDIUM -> "priority-medium";
			case LOW -> "priority-low";
		};

		Map<String, Object> variables = Dict.create()
				.set("taskTitle", item.getTitle())
				.set("taskContent", item.getContent() != null ? item.getContent() : "No content provided.")
				.set("priority", item.getPriority().name())
				.set("columnName", item.getColumn().getName())
				.set("priorityClass", priorityClass);

		TemplateMailMessage message = TemplateMailMessage.builder()
				.to(adminEmail)
				.subject(subject)
				.templateName("kanban-reminder")
				.variables(variables)
				.type(TemplateMailMessage.MailType.TEMPLATE)
				.build();

		rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, message);
		log.info("Dispatched async beautiful kanban reminder for item: {}", item.getId());
	}
}
