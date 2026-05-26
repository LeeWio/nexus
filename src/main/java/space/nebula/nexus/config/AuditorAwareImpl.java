package space.nebula.nexus.config;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String>
{

	@Override
	public Optional<String> getCurrentAuditor()
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (ObjectUtil.isNull(authentication) || !authentication.isAuthenticated()
				|| ObjectUtil.equal("anonymousUser", authentication.getPrincipal()))
		{
			return Optional.of("SYSTEM");
		}

		return Optional.of(authentication.getName());
	}
}
