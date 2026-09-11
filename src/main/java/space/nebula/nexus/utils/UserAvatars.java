package space.nebula.nexus.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resolves a public avatar URL from a stored upload, GitHub identity, or email.
 */
public final class UserAvatars {

	private static final Pattern GITHUB_USERNAME = Pattern
			.compile("^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$");
	private static final HexFormat HEX = HexFormat.of();

	private UserAvatars() {
	}

	/**
	 * Picks the best display avatar without overwriting a custom upload.
	 *
	 * @param storedAvatar
	 *            uploaded or previously stored avatar URL
	 * @param email
	 *            account email used for Gravatar
	 * @param githubUsername
	 *            linked GitHub login
	 * @return avatar URL, or {@code null} when no source is available
	 */
	public static String resolve(String storedAvatar, String email, String githubUsername) {
		if (hasText(storedAvatar)) {
			return storedAvatar.trim();
		}
		String githubAvatar = githubAvatar(githubUsername);
		if (githubAvatar != null) {
			return githubAvatar;
		}
		return gravatar(email);
	}

	/**
	 * Builds a Gravatar URL for the given email, falling back to an identicon.
	 *
	 * @param email
	 *            account email
	 * @return Gravatar URL, or {@code null} when the email is missing
	 */
	public static String gravatar(String email) {
		if (!hasText(email) || !email.contains("@")) {
			return null;
		}
		String normalized = email.trim().toLowerCase(Locale.ROOT);
		return "https://www.gravatar.com/avatar/" + sha256Hex(normalized) + "?s=256&d=identicon";
	}

	private static String githubAvatar(String githubUsername) {
		if (!hasText(githubUsername)) {
			return null;
		}
		String username = githubUsername.trim();
		if (!GITHUB_USERNAME.matcher(username).matches()) {
			return null;
		}
		return "https://github.com/" + username + ".png";
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is required to build Gravatar URLs", exception);
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
