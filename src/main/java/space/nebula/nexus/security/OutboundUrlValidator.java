package space.nebula.nexus.security;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import space.nebula.nexus.common.exception.BusinessException;

/**
 * Validates URLs before the server makes an outbound HTTP request.
 *
 * <p>Loopback, link-local, site-local, multicast and unspecified addresses are
 * rejected to prevent server-side request forgery into trusted networks.</p>
 */
@Component
public class OutboundUrlValidator {

	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

	/**
	 * Validates an HTTP URL and every address currently returned by DNS.
	 *
	 * @param url outbound URL
	 * @return the parsed and normalized URI
	 * @throws BusinessException if the URL can access a non-public destination
	 */
	public URI validate(String url) {
		try {
			URI uri = URI.create(url).normalize();
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!ALLOWED_SCHEMES.contains(scheme) || uri.getUserInfo() != null || uri.getHost() == null) {
				throw invalidUrl();
			}

			String host = IDN.toASCII(uri.getHost());
			InetAddress[] addresses = InetAddress.getAllByName(host);
			if (addresses.length == 0 || Arrays.stream(addresses).anyMatch(this::isNonPublic)) {
				throw invalidUrl();
			}
			return uri;
		} catch (IllegalArgumentException | UnknownHostException e) {
			throw invalidUrl();
		}
	}

	private boolean isNonPublic(InetAddress address) {
		if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
				|| address.isSiteLocalAddress() || address.isMulticastAddress()) {
			return true;
		}
		byte[] bytes = address.getAddress();
		if (bytes.length == 4) {
			int first = Byte.toUnsignedInt(bytes[0]);
			int second = Byte.toUnsignedInt(bytes[1]);
			return first == 0 || first >= 224 || (first == 100 && second >= 64 && second <= 127)
					|| (first == 192 && second == 0) || (first == 198 && (second == 18 || second == 19));
		}
		return (bytes[0] & 0xfe) == 0xfc;
	}

	private BusinessException invalidUrl() {
		return new BusinessException("Outbound URL must resolve to a public HTTP(S) address");
	}
}
