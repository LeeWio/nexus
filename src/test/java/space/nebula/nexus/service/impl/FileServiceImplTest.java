package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.config.StorageProperties;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.mapper.FileMapper;
import space.nebula.nexus.repository.FileRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.utils.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private StorageProvider storageProvider;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileUtil fileUtil;
    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() throws java.io.IOException {
        lenient().when(storageProperties.getMaxFileSize()).thenReturn(10485760L);
        lenient().when(storageProperties.getAllowedMimeTypes()).thenReturn(Arrays.asList("image/jpeg", "image/png"));
        lenient().when(fileUtil.convertToWebP(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void uploadFile_Deduplication_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test content".getBytes());
        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setId(1L);
        existingMetadata.setReferenceCount(1);

        when(fileRepository.findByFileHash(anyString())).thenReturn(Optional.of(existingMetadata));
        when(fileMapper.toResponse(any())).thenReturn(null);

        var response = fileService.uploadFile(file);

        assertEquals(200, response.code());
        assertEquals(2, existingMetadata.getReferenceCount());
        verify(fileRepository).save(existingMetadata);
        verify(storageProvider, never()).store(any(), any());
    }

    @Test
    void uploadFile_NewFile_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test content".getBytes());
        
        when(fileRepository.findByFileHash(anyString())).thenReturn(Optional.empty());
        when(fileUtil.detectMimeType(any(InputStream.class))).thenReturn("image/jpeg");
        when(fileUtil.isImage("image/jpeg")).thenReturn(true);
        when(fileUtil.getImageDimensions(any())).thenReturn(new FileUtil.ImageDimensions(100, 100));
        when(fileUtil.generateThumbnail(any(), anyInt(), anyInt())).thenReturn("thumb content".getBytes());
        when(storageProvider.getUrl(anyString())).thenReturn("http://example.com/file.jpg");
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(i -> {
            FileMetadata fm = i.getArgument(0);
            fm.setId(100L);
            return fm;
        });
        
        var response = fileService.uploadFile(file);

        assertEquals(200, response.code());
        verify(storageProvider, times(2)).store(any(), any()); // one for image, one for thumbnail
        verify(fileRepository).save(any(FileMetadata.class));
    }

    @Test
    void deleteFile_Permanent_Success() {
        String fileName = "test.jpg";
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(fileName);
        metadata.setReferenceCount(1);

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.of(metadata));

        var response = fileService.deleteFile(fileName);

        assertEquals(200, response.code());
        verify(storageProvider).delete(fileName);
        verify(fileRepository).delete(metadata);
    }

    @Test
    void uploadFile_SizeExceeded_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[10485761]);

        var ex = assertThrows(space.nebula.nexus.common.exception.BusinessException.class, () -> fileService.uploadFile(file));
        assertEquals(41301, ex.getCode());
        assertArrayEquals(new Object[]{"10 MB", "10 MB"}, ex.getArgs());
    }

    @Test
    void uploadFile_MimeNotAllowed_ThrowsException() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        
        when(fileRepository.findByFileHash(anyString())).thenReturn(Optional.empty());
        when(fileUtil.detectMimeType(any(InputStream.class))).thenReturn("text/plain");

        var ex = assertThrows(space.nebula.nexus.common.exception.BusinessException.class, () -> fileService.uploadFile(file));
        assertEquals(40003, ex.getCode());
        assertArrayEquals(new Object[]{"text/plain", "image/jpeg, image/png"}, ex.getArgs());
    }
}
