package space.nebula.nexus.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchResponse {
	private String name;
	private String code;
	private String symbol;
	private String market; // "US" or "CN"
}
