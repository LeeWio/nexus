package space.nebula.nexus.service;

import cn.hutool.core.lang.Dict;

public interface IMetricsService {
	/**
	 * Returns a consolidated snapshot of system performance metrics.
	 */
	Dict getSystemPerformanceSnapshot();
}
