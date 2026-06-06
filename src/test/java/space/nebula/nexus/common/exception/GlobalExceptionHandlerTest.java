package space.nebula.nexus.common.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.utils.MessageUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    void handleBusinessException_Localized() {
        BusinessException ex = new BusinessException(404, "Original Message");
        when(messageUtil.get(404)).thenReturn("Localized Not Found");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(ex);

        assertEquals(404, response.getBody().code());
        assertEquals("Localized Not Found", response.getBody().message());
    }

    @Test
    void handleBusinessException_Fallback() {
        BusinessException ex = new BusinessException(40010, "Post already published");
        when(messageUtil.get(40010)).thenReturn(""); // Simulate no translation

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(ex);

        assertEquals(40010, response.getBody().code());
        assertEquals("Post already published", response.getBody().message());
    }
}
