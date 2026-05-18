package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.KanbanItem;
import space.nebula.nexus.repository.KanbanItemRepository;
import space.nebula.nexus.utils.MailUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KanbanReminderTask {

    private final KanbanItemRepository itemRepository;
    private final MailUtil mailUtil;

    @Value("${app.admin-email:your-email@example.com}")
    private String adminEmail;

    /**
     * Scan for kanban items that have a reminder set and are due.
     * Runs every minute.
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
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("taskTitle", item.getTitle());
        variables.put("taskContent", item.getContent() != null ? item.getContent() : "No content provided.");
        variables.put("priority", item.getPriority().name());
        variables.put("columnName", item.getColumn().getName());
        
        // CSS class for priority badge
        String priorityClass = switch (item.getPriority()) {
            case HIGH -> "priority-high";
            case MEDIUM -> "priority-medium";
            case LOW -> "priority-low";
        };
        variables.put("priorityClass", priorityClass);

        mailUtil.sendTemplateMail(adminEmail, subject, "kanban-reminder", variables);
        log.info("Sent beautiful kanban reminder for item: {}", item.getId());
    }
}
