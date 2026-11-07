package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a bearer token whose account has since been deactivated or deleted (FR-SEC-active-account).
 *
 * <p>A JWT stays valid for its full lifetime, so without this check a student deactivated mid-session would keep
 * her access until the token expired. This wraps the existing {@link JwtAuthenticationConverter}, reads the enabled flag
 * of the user named by the {@code userId} claim, and throws {@link DisabledException} when that user is missing or disabled.
 * {@code DisabledException} is an {@code AuthenticationException}, so the bearer-token entry point answers 401 and
 * the SPA logs out. It runs on every request that carries a bearer header, including {@code permitAll} routes.
 *
 * <p>The cost is one single-column primary-key read per authenticated request (design/stu.md, Key decisions).
 */
@Component
public class ActiveAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter delegate;
    private final UserRepository userRepository;


    public ActiveAccountJwtAuthenticationConverter(JwtAuthenticationConverter jwtAuthenticationConverter, UserRepository userRepository) {
        this.delegate = jwtAuthenticationConverter;
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer userId = readUserId(jwt);
        boolean active = userId != null && this.userRepository.findEnabledById(userId).orElse(false);
        if (!active) {
            throw new DisabledException("This account is deactivated or no longer exists.");
        }
        return this.delegate.convert(jwt);
    }

    /**
     * Reads the {@code userId} claim the same way {@code UserUtils.getUserId} does. {@code UserUtils} reads it off
     * the security context, which is not populated yet while the token is being converted.
     */
    private static Integer readUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Integer i) return i;
        if (claim instanceof Long l) return l.intValue();
        if (claim instanceof String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

}
