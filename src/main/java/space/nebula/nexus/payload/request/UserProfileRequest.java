package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update user profile information")
public record UserProfileRequest(
    @Schema(description = "User display name", example = "TechEnthusiast")
    @Size(max = 50)
    String nickname,
    
    @Schema(description = "URL to the user avatar image")
    @Size(max = 255)
    String avatar,
    
    @Schema(description = "Short personal biography")
    @Size(max = 500)
    String bio,
    
    @Schema(description = "Geographic location", example = "San Francisco, CA")
    @Size(max = 100)
    String location,
    
    @Schema(description = "Link to personal website or blog")
    @Size(max = 100)
    String website,
    
    @Schema(description = "Public contact email", example = "hello@johndoe.com")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    String email
) {}
