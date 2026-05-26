package space.nebula.nexus.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.model.SecurityUser;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService
{

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException
	{
		User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + usernameOrEmail));

		return new SecurityUser(user);
	}
}
