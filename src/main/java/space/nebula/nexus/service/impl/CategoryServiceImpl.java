package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.mapper.CategoryMapper;
import space.nebula.nexus.payload.request.CategoryRequest;
import space.nebula.nexus.payload.response.CategoryResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.service.ICategoryService;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService
{

	private final CategoryRepository categoryRepository;
	private final PostRepository postRepository;
	private final CategoryMapper categoryMapper;
	private final RedisUtil redisUtil;

	@Override
	@Cacheable(value = CacheConstants.CATEGORIES, key = "'all'")
	public ApiResponse<List<CategoryResponse>> retrieveAllCategories()
	{
		List<Category> allCategories = categoryRepository.findAll();
		return ApiResponse.success(categoryMapper.toResponseList(allCategories));
	}

	@Override
	@Transactional
	@LogOperation("Create Category")
	@CacheEvict(value = { CacheConstants.CATEGORIES, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<CategoryResponse> createCategory(CategoryRequest request)
	{
		// Check name and slug including deleted rows for self-healing / seamless restore
		if (request.name() != null)
		{
			var existingByName = categoryRepository.findByNameIncludeDeleted(request.name());
			if (existingByName.isPresent())
			{
				Category category = existingByName.get();
				if (Boolean.TRUE.equals(category.getIsDeleted()))
				{
					category.setIsDeleted(false);
					categoryMapper.updateEntity(category, request);
					categoryRepository.save(category);
					log.info("Restored soft-deleted category by name: {}", category.getName());
					return ApiResponse.success("Category created successfully", categoryMapper.toResponse(category));
				}
				else
				{
					throw new BusinessException(BusinessCode.DUPLICATE_KEY, "Category name already exists: " + request.name());
				}
			}
		}

		if (request.slug() != null)
		{
			var existingBySlug = categoryRepository.findBySlugIncludeDeleted(request.slug());
			if (existingBySlug.isPresent())
			{
				Category category = existingBySlug.get();
				if (Boolean.TRUE.equals(category.getIsDeleted()))
				{
					category.setIsDeleted(false);
					categoryMapper.updateEntity(category, request);
					categoryRepository.save(category);
					log.info("Restored soft-deleted category by slug: {}", category.getName());
					return ApiResponse.success("Category created successfully", categoryMapper.toResponse(category));
				}
				else
				{
					throw new BusinessException(BusinessCode.DUPLICATE_KEY, "Category slug already exists: " + request.slug());
				}
			}
		}

		Category newCategory = new Category();
		categoryMapper.updateEntity(newCategory, request);

		categoryRepository.save(newCategory);
		log.info("New category created: {}", newCategory.getName());
		return ApiResponse.success("Category created successfully", categoryMapper.toResponse(newCategory));
	}

	@Override
	@Transactional
	@LogOperation("Update Category")
	@CacheEvict(value = { CacheConstants.CATEGORIES, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<CategoryResponse> updateCategory(Long id, CategoryRequest request)
	{
		Category existingCategory = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

		validateUniqueConstraints(existingCategory, request);

		categoryMapper.updateEntity(existingCategory, request);
		categoryRepository.save(existingCategory);

		log.info("Category updated: {}", existingCategory.getName());
		return ApiResponse.success("Category updated successfully", categoryMapper.toResponse(existingCategory));
	}

	@Override
	@Transactional
	@LogOperation("Delete Category")
	@CacheEvict(value = { CacheConstants.CATEGORIES, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<Void> deleteCategory(Long id)
	{
		Assert.isTrue(categoryRepository.existsById(id), () -> new ResourceNotFoundException("Category", "id", id));

		// Check if any active posts are still associated with this category
		Assert.isFalse(postRepository.existsByCategoryId(id),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Cannot delete category as it is still referenced by active posts"));

		categoryRepository.deleteById(id);
		log.info("Category deleted id: {}", id);
		return ApiResponse.success("Category deleted successfully", null);
	}

	private void validateUniqueConstraints(Category existing, CategoryRequest request)
	{
		if (request.name() != null && (existing == null || !existing.getName().equals(request.name())))
		{
			Assert.isFalse(categoryRepository.findByNameIncludeDeleted(request.name()).isPresent(),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
							"Category name already exists: " + request.name()));
		}
		if (request.slug() != null && (existing == null || !existing.getSlug().equals(request.slug())))
		{
			Assert.isFalse(categoryRepository.findBySlugIncludeDeleted(request.slug()).isPresent(),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
							"Category slug already exists: " + request.slug()));
		}
	}
}
