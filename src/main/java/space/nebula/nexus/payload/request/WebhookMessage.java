package space.nebula.nexus.payload.request;

import cn.hutool.core.lang.Dict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long webhookId;
    private String event;
    private Dict payload;
}
