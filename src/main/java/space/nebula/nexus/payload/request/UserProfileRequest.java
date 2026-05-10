package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
    @Size(max = 50)
    String nickname,
    
    @Size(max = 255)
    String avatar,
    
    @Size(max = 500)
    String bio,
    
    @Size(max = 100)
    String location,
    
    @Size(max = 100)
    String website,
    
    @Email
    String email
) {}
