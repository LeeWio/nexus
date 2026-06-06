package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.LinkCheckLog;
import space.nebula.nexus.payload.response.PageResult;

public interface ILinkHealthService {

    /**
     * Scans all public posts and friend links for dead links.
     */
    void runFullScan();

    /**
     * Returns a paginated list of broken links.
     */
    ApiResponse<PageResult<LinkCheckLog>> getBrokenLinks(Pageable pageable);

    /**
     * Clears old health check logs.
     */
    ApiResponse<Void> clearLogs();
}
