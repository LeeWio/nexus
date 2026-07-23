package space.nebula.nexus.payload.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Represents a JSON message sent by Canal to RabbitMQ.
 */
@Data
public class CanalMessage {
	private String database;
	private String table;
	private String type;
	private List<Map<String, String>> data;
	private List<Map<String, String>> old;
}
