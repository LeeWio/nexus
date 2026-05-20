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
public class MarketIndexResponse {
	private String name;
	private String symbol;
	private BigDecimal current;
	private BigDecimal changePct;
	private List<BigDecimal> sparkline;
	private Boolean isOpen;
}
