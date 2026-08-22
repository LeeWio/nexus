package space.nebula.nexus.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTrendResponse {
	private String name;
	private String symbol;
	private BigDecimal current;
	private BigDecimal changePct;
	private Boolean isOpen;
	private List<TrendPoint> trendPoints;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class TrendPoint {
		private String date; // "YYYY-MM-DD" or "YYYY-MM-DD HH:mm:ss"
		private BigDecimal price;
	}
}
