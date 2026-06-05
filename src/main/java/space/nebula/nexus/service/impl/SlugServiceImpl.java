package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.service.ISlugService;
import space.nebula.nexus.utils.SlugUtil;

import java.util.function.Predicate;

@Service
public class SlugServiceImpl implements ISlugService
{

	@Override
	public String toSlug(String input)
	{
		return SlugUtil.toSlug(input);
	}

	@Override
	public String generateUniqueSlug(String requestedSlug, String fallbackTitle, Predicate<String> existsChecker)
	{
		String slug = StrUtil.isBlank(requestedSlug) ? toSlug(fallbackTitle) : toSlug(requestedSlug);

		Assert.isFalse(existsChecker.test(slug),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "The slug '" + slug + "' is already in use."));

		return slug;
	}
}
