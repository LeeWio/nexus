package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.LifeListItem;
import space.nebula.nexus.entity.LifeListMemory;
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.enums.LifeListVisibility;
import space.nebula.nexus.payload.request.LifeListMemoryRequest;
import space.nebula.nexus.payload.request.LifeListStatusRequest;
import space.nebula.nexus.payload.response.LifeListPageResponse;
import space.nebula.nexus.repository.LifeListCategoryRepository;
import space.nebula.nexus.repository.LifeListItemRepository;
import space.nebula.nexus.repository.LifeListMemoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifeListServiceImplTest {

	@Mock
	private LifeListCategoryRepository categoryRepository;
	@Mock
	private LifeListItemRepository itemRepository;
	@Mock
	private LifeListMemoryRepository memoryRepository;

	private LifeListServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new LifeListServiceImpl(categoryRepository, itemRepository, memoryRepository);
	}

	@Test
	void publicListUsesPublicCountsAndHidesPrivateMemories() {
		LifeListItem item = item(1L, 7, LifeListStatus.COMPLETED, LifeListVisibility.PUBLIC);
		when(itemRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20))))
				.thenReturn(new PageImpl<>(List.of(item)));
		when(memoryRepository.findByItemIdAndVisibilityOrderByOccurredAtDescCreatedAtDesc(1L, LifeListVisibility.PUBLIC))
				.thenReturn(List.of());
		when(itemRepository.countByVisibilityAndStatusAndArchivedAtIsNull(LifeListVisibility.PUBLIC,
				LifeListStatus.COMPLETED)).thenReturn(1L);
		when(itemRepository.countByVisibilityAndArchivedAtIsNull(LifeListVisibility.PUBLIC)).thenReturn(4L);

		LifeListPageResponse page = service.getPublicItems(null, null, PageRequest.of(0, 20)).data();

		assertEquals(1, page.summary().completed());
		assertEquals(4, page.summary().total());
		assertEquals(false, page.summary().includesPrivate());
		assertEquals(0, page.list().get(0).memoryCount());
	}

	@Test
	void privateNumberLooksMissing() {
		LifeListItem item = item(2L, 8, LifeListStatus.PLANNED, LifeListVisibility.PRIVATE);
		when(itemRepository.findByNumber(8)).thenReturn(Optional.of(item));

		assertThrows(ResourceNotFoundException.class, () -> service.getPublicItem(8));
	}

	@Test
	void reversingCompletionClearsDateAndKeepsMemories() {
		LifeListItem item = item(3L, 9, LifeListStatus.COMPLETED, LifeListVisibility.PUBLIC);
		item.setCompletedAt(LocalDate.of(2024, 1, 2));
		when(itemRepository.findById(3L)).thenReturn(Optional.of(item));
		when(itemRepository.save(item)).thenReturn(item);
		when(memoryRepository.findByItemIdOrderByOccurredAtDescCreatedAtDesc(3L)).thenReturn(List.of(memory(item)));

		var response = service.changeStatus(3L, new LifeListStatusRequest(LifeListStatus.PLANNED, null)).data();

		assertEquals(LifeListStatus.PLANNED, item.getStatus());
		assertNull(item.getCompletedAt());
		assertEquals(1, response.memoryCount());
		verify(memoryRepository).findByItemIdOrderByOccurredAtDescCreatedAtDesc(3L);
	}

	@Test
	void savingAMemoryDoesNotChangeStatus() {
		LifeListItem item = item(4L, 10, LifeListStatus.PLANNED, LifeListVisibility.PUBLIC);
		when(itemRepository.findById(4L)).thenReturn(Optional.of(item));
		when(memoryRepository.save(any(LifeListMemory.class))).thenAnswer(invocation -> {
			LifeListMemory saved = invocation.getArgument(0);
			saved.setId(99L);
			return saved;
		});

		service.createMemory(4L, new LifeListMemoryRequest(LocalDate.of(2026, 10, 20), "It happened.", "Kyoto",
				LifeListVisibility.PRIVATE));

		assertEquals(LifeListStatus.PLANNED, item.getStatus());
		ArgumentCaptor<LifeListMemory> captor = ArgumentCaptor.forClass(LifeListMemory.class);
		verify(memoryRepository).save(captor.capture());
		assertEquals(LifeListVisibility.PRIVATE, captor.getValue().getVisibility());
	}

	@Test
	void refusesToDeleteACategoryThatStillHasWishes() {
		var category = new space.nebula.nexus.entity.LifeListCategory();
		category.setId(5L);
		category.setSlug("travel");
		when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
		when(itemRepository.countByCategoryId(5L)).thenReturn(2L);

		assertThrows(BusinessException.class, () -> service.deleteCategory(5L));
	}

	private LifeListItem item(Long id, int number, LifeListStatus status, LifeListVisibility visibility) {
		LifeListItem item = new LifeListItem();
		item.setId(id);
		item.setNumber(number);
		item.setTitle("Wish " + number);
		item.setStatus(status);
		item.setVisibility(visibility);
		item.setSortOrder(number);
		return item;
	}

	private LifeListMemory memory(LifeListItem item) {
		LifeListMemory memory = new LifeListMemory();
		memory.setId(1L);
		memory.setItem(item);
		memory.setContent("Kept");
		memory.setVisibility(LifeListVisibility.PUBLIC);
		return memory;
	}
}
