package space.nebula.nexus.mapper.config;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Global MapStruct configuration to ensure consistency and eliminate unmapped warnings.
 * Standardizes how system fields (auditing, soft delete) are handled across all mappers.
 */
@MapperConfig(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR, // Force error on unmapped properties for strictness
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED
)
public interface CentralMapperConfig {
    
    // This interface serves as a template. Specific ignores are applied in individual mappers
    // or through shared methods if they shared exact signatures.
}
