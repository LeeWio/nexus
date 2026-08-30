package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.ContentWorkflowResponse;

public interface IContentWorkflowService {
	ApiResponse<ContentWorkflowResponse> getWorkflow();
}
