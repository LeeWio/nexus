package space.nebula.nexus.service;

import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.payload.response.MomentXSyncResponse;

import java.util.Collection;
import java.util.Map;

public interface IXSyncService {

	/**
	 * Validates shareToX against visibility and feature flag, then inserts a
	 * PENDING sync row when posting should be attempted after commit.
	 *
	 * @return true when a PENDING sync row was created
	 */
	boolean enqueueOnCreate(Moment moment, Boolean shareToX);

	/** Perform the outbound X API work for a PENDING sync row. */
	void processPending(Long momentId);

	MomentXSyncResponse findByMomentId(Long momentId);

	Map<Long, MomentXSyncResponse> findByMomentIds(Collection<Long> momentIds);
}
