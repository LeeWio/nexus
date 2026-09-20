package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.config.XProperties;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.MomentXSync;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.enums.MomentXSyncStatus;
import space.nebula.nexus.integration.x.XClient;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.MomentXSyncRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XSyncServiceImplTest {

	@Mock
	private MomentXSyncRepository momentXSyncRepository;
	@Mock
	private MomentRepository momentRepository;
	@Mock
	private XClient xClient;
	@Mock
	private StorageProvider storageProvider;

	private XProperties properties;
	private XSyncServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new XProperties();
		properties.setEnabled(true);
		properties.setApiKey("key");
		properties.setApiSecret("secret");
		properties.setAccessToken("token");
		properties.setAccessTokenSecret("token-secret");
		service = new XSyncServiceImpl(momentXSyncRepository, momentRepository, properties, xClient, storageProvider);
	}

	@Test
	void enqueueOnCreateRejectsNonPublicMoments() {
		Moment moment = new Moment();
		moment.setId(1L);
		moment.setVisibility(MomentVisibility.PRIVATE);

		assertThrows(BusinessException.class, () -> service.enqueueOnCreate(moment, true));
		verify(momentXSyncRepository, never()).save(any());
	}

	@Test
	void enqueueOnCreateSkipsWhenFeatureDisabled() {
		properties.setEnabled(false);
		Moment moment = new Moment();
		moment.setId(1L);
		moment.setVisibility(MomentVisibility.PUBLIC);

		assertFalse(service.enqueueOnCreate(moment, true));
		verify(momentXSyncRepository, never()).save(any());
	}

	@Test
	void enqueueOnCreatePersistsPendingRow() {
		Moment moment = new Moment();
		moment.setId(7L);
		moment.setVisibility(MomentVisibility.PUBLIC);
		when(momentXSyncRepository.findByMomentId(7L)).thenReturn(Optional.empty());

		assertTrue(service.enqueueOnCreate(moment, true));

		ArgumentCaptor<MomentXSync> captor = ArgumentCaptor.forClass(MomentXSync.class);
		verify(momentXSyncRepository).save(captor.capture());
		assertEquals(MomentXSyncStatus.PENDING, captor.getValue().getStatus());
		assertEquals(moment, captor.getValue().getMoment());
	}

	@Test
	void processPendingMarksPostedOnSuccess() {
		Moment moment = new Moment();
		moment.setId(7L);
		moment.setVisibility(MomentVisibility.PUBLIC);
		moment.setContent("Hello from Odyssey");

		MomentXSync sync = new MomentXSync();
		sync.setMoment(moment);
		sync.setStatus(MomentXSyncStatus.PENDING);
		sync.setAttempts(0);

		when(momentXSyncRepository.findByMomentId(7L)).thenReturn(Optional.of(sync));
		when(momentRepository.findById(7L)).thenReturn(Optional.of(moment));
		when(xClient.createPost(any(), any()))
				.thenReturn(new XClient.CreatedPost("123", "https://x.com/i/web/status/123"));

		service.processPending(7L);

		assertEquals(MomentXSyncStatus.POSTED, sync.getStatus());
		assertEquals("123", sync.getXPostId());
		verify(momentXSyncRepository).save(sync);
	}
}
