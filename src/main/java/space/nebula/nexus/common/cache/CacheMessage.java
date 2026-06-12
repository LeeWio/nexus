package space.nebula.nexus.common.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheMessage implements Serializable {
    private String cacheName;
    private Object key;
    private String sourceInstanceId; // To avoid self-invalidation
}
