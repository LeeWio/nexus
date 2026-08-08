package space.nebula.nexus.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;

/**
 * Defines the API contract shown to frontend consumers when OpenAPI is enabled.
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

	private static final String BEARER_AUTH = "bearerAuth";
	private static final String API_ERROR = "ApiError";
	private static final String API_ERROR_REF = "#/components/schemas/" + API_ERROR;

	@Bean
	OpenAPI nexusOpenApi() {
		return new OpenAPI().info(new Info().title("Nexus API").version("v1")
				.description("""
						## Frontend integration contract
						- Every JSON endpoint returns the `ApiResponse<T>` envelope: `code`, `message`, `data`, and `traceId`.
						- Supply the access token as `Authorization: Bearer <accessToken>`. Only operations marked with the lock require it.
						- Spring page requests use a zero-based `page` query parameter. `PageResult.page` in the response is one-based for display.
						- Cursor responses return `nextCursor`; pass it back as `cursor` until `hasMore` is `false`.
						- Handle `401` by renewing or clearing credentials, `403` as an authorization failure, `429` by backing off, and use `traceId` when reporting an unexpected error.
						""")
				.contact(new Contact().name("Nebula Space Team").url("https://nebula.space"))
				.license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
				.components(new Components().addSecuritySchemes(BEARER_AUTH,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
								.description("JWT access token returned by `/api/v1/auth/login` or `/api/v1/auth/otp/login`.")));
	}

	/**
	 * Applies the envelope, error, and access contract consistently to generated
	 * operations so frontend clients do not have to infer them controller by
	 * controller.
	 */
	@Bean
	OpenApiCustomizer nexusDocumentationCustomizer() {
		return openApi -> {
			ensureErrorSchema(openApi);
			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations()
					.forEach(operation -> documentOperation(path, operation)));
		};
	}

	private void documentOperation(String path, Operation operation) {
		addErrorResponse(operation, "400", "Validation failed or a business rule rejected the request.");
		addErrorResponse(operation, "404", "The referenced resource does not exist or is not visible to this caller.");
		addErrorResponse(operation, "429", "The operation is rate limited. Retry after backing off.");
		addErrorResponse(operation, "500", "An unexpected server error occurred. Include `traceId` when contacting support.");
		operation.addExtension("x-response-envelope", "ApiResponse<T>");

		if (requiresAuthentication(path, operation)) {
			if (!usesBearerAuth(operation)) {
				operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
			}
			addErrorResponse(operation, "401", "A valid JWT access token is required.");
			addErrorResponse(operation, "403", "The authenticated user lacks the required permission for this operation.");
			appendAccessNote(operation, path.startsWith("/api/v1/admin/")
					? "**Access:** Requires an authenticated staff account. The backend enforces the operation's ADMIN or EDITOR role."
					: "**Access:** Requires a valid JWT access token. User-owned resources are scoped to the current account.");
		} else {
			appendAccessNote(operation, "**Access:** Public. A JWT is not required for this operation.");
		}
	}

	private void ensureErrorSchema(OpenAPI openApi) {
		Components components = openApi.getComponents();
		if (components == null) {
			components = new Components();
			openApi.setComponents(components);
		}
		if (components.getSchemas() == null) {
			components.setSchemas(new LinkedHashMap<>());
		}
		components.getSchemas().putIfAbsent(API_ERROR, new ObjectSchema().description("Standard failed API response envelope")
				.addProperty("code", new IntegerSchema().description("Application error code; normally matches the HTTP status.")
						.example(400))
				.addProperty("message", new StringSchema().description("Human-readable failure reason.")
						.example("Validation failed: title is required"))
				.addProperty("data", new Schema<>().nullable(true).description("Always null for an error response."))
				.addProperty("traceId", new StringSchema().nullable(true)
						.description("Request correlation ID for support and server logs.").example("7e8f6a1cf4cc4d0fa2eec6d85a7c9f31")));
	}

	private boolean requiresAuthentication(String path, Operation operation) {
		return path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/user/") || usesBearerAuth(operation);
	}

	private boolean usesBearerAuth(Operation operation) {
		return operation.getSecurity() != null && operation.getSecurity().stream()
				.anyMatch(requirement -> requirement.containsKey(BEARER_AUTH));
	}

	private void addErrorResponse(Operation operation, String responseCode, String description) {
		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}
		responses.putIfAbsent(responseCode,
				new ApiResponse().description(description).content(new Content().addMediaType("application/json",
						new MediaType().schema(new Schema<>().$ref(API_ERROR_REF)))));
	}

	private void appendAccessNote(Operation operation, String note) {
		String description = operation.getDescription();
		if (description == null || description.isBlank()) {
			operation.setDescription(note);
		} else if (!description.contains("**Access:**")) {
			operation.setDescription(description + "\n\n" + note);
		}
	}
}
