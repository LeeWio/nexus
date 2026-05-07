package space.nebula.nexus.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.mapper.CategoryMapper;
import space.nebula.nexus.payload.request.CategoryRequest;
import space.nebula.nexus.payload.response.CategoryResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.service.ICategoryService;

import java.util.List;

@Slf4j
@Service
public class CategoryServiceImpl implements ICategoryService {

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        return ApiResponse.success(categoryMapper.toResponseList(categoryRepository.findAll()));
    }

    @Override
    @Transactional
    @LogOperation("Create Category")
    public ApiResponse<CategoryResponse> createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw new BusinessException("Category name already exists");
        }
        if (categoryRepository.findBySlug(request.slug()).isPresent()) {
            throw new BusinessException("Category slug already exists");
        }

        Category category = new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());

        categoryRepository.save(category);
        log.info("Category created: {}", category.getName());
        return ApiResponse.success("Category created successfully", categoryMapper.toResponse(category));
    }

    @Override
    @Transactional
    @LogOperation("Update Category")
    public ApiResponse<CategoryResponse> updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Category not found"));

        if (!category.getName().equals(request.name()) && categoryRepository.findByName(request.name()).isPresent()) {
            throw new BusinessException("Category name already exists");
        }
        if (!category.getSlug().equals(request.slug()) && categoryRepository.findBySlug(request.slug()).isPresent()) {
            throw new BusinessException("Category slug already exists");
        }

        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());

        categoryRepository.save(category);
        log.info("Category updated: {}", category.getName());
        return ApiResponse.success("Category updated successfully", categoryMapper.toResponse(category));
    }

    @Override
    @Transactional
    @LogOperation("Delete Category")
    public ApiResponse<Void> deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(404, "Category not found");
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted id: {}", id);
        return ApiResponse.success("Category deleted successfully", null);
    }
}
