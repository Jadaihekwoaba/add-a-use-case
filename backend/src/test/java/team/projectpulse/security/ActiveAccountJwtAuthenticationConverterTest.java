package team.projectpulse.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import team.projectpulse.user.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * FR-SEC-active-account: the converter reads only the enabled flag, never the whole user, on every request.
 */
@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtAuthenticationConverterTest {

    @Mock
    UserRepository userRepository;

    @Test
    @DisplayName("An enabled account is converted, with a single-column read and no entity load")
    void testEnabledAccountIsConverted() {
        given(this.userRepository.findEnabledById(4)).willReturn(Optional.of(true));

        AbstractAuthenticationToken token = converter().convert(jwtWithUserId(4));

        assertThat(token).isNotNull();
        assertThat(token.isAuthenticated()).isTrue();
        verify(this.userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("A disabled account is refused")
    void testDisabledAccountIsRefused() {
        given(this.userRepository.findEnabledById(4)).willReturn(Optional.of(false));

        assertThat(catchThrowable(() -> converter().convert(jwtWithUserId(4)))).isInstanceOf(DisabledException.class);
        verify(this.userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("A missing account is refused")
    void testMissingAccountIsRefused() {
        given(this.userRepository.findEnabledById(999)).willReturn(Optional.empty());

        assertThat(catchThrowable(() -> converter().convert(jwtWithUserId(999)))).isInstanceOf(DisabledException.class);
    }

    @Test
    @DisplayName("A token without a readable userId claim is refused without touching the database")
    void testUnreadableUserIdIsRefused() {
        assertThat(catchThrowable(() -> converter().convert(jwtWithUserId("not-a-number")))).isInstanceOf(DisabledException.class);
        verify(this.userRepository, never()).findEnabledById(any());
    }

    private ActiveAccountJwtAuthenticationConverter converter() {
        return new ActiveAccountJwtAuthenticationConverter(new JwtAuthenticationConverter(), this.userRepository);
    }

    private static Jwt jwtWithUserId(Object userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("someone")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("userId", userId)
                .build();
    }

}
