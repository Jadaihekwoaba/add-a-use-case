package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.instructor.InstructorRepository;
import team.projectpulse.student.StudentRepository;
import team.projectpulse.system.StatusCode;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account: a deactivated account is refused on its next request, even with an unexpired token,
 * and cannot log in again.
 */
@DisplayName("Integration tests for FR-SEC-active-account")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    InstructorRepository instructorRepository;

    @Autowired
    JwtEncoder jwtEncoder;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    String studentJohnToken;

    String instructorBillToken;


    @BeforeEach
    void setUp() throws Exception {
        this.studentJohnToken = login("j.smith@abc.edu");
        this.instructorBillToken = login("b.gates@abc.edu");
    }

    @Test
    @DisplayName("An active student's token is accepted")
    void testActiveStudentTokenIsAccepted() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true));
    }

    @Test
    @DisplayName("A deactivated student's unexpired token is refused with 401 on her next request")
    void testDeactivatedStudentTokenIsRefused() throws Exception {
        this.studentRepository.findById(4).orElseThrow().setEnabled(false);

        this.mockMvc.perform(get(this.baseUrl + "/students/4").header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("A deactivated student's token is refused even on a permitAll route")
    void testDeactivatedStudentTokenIsRefusedOnAPublicRoute() throws Exception {
        this.studentRepository.findById(4).orElseThrow().setEnabled(false);

        this.mockMvc.perform(get(this.baseUrl + "/users/exists/j.smith@abc.edu").header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("A token whose account no longer exists is refused with 401")
    void testTokenOfMissingAccountIsRefused() throws Exception {
        // A validly signed token for a user id that names no account, as if the account had been deleted
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .subject("ghost@abc.edu").claim("userId", 9999).claim("authorities", "ROLE_student")
                .build();
        String ghostToken = "Bearer " + this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").header(HttpHeaders.AUTHORIZATION, ghostToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("A deactivated student cannot log in again")
    void testDeactivatedStudentCannotLogIn() throws Exception {
        this.studentRepository.findById(4).orElseThrow().setEnabled(false);

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("The check also covers instructors (UC-INS-deactivate-instructor inherits it)")
    void testDeactivatedInstructorTokenIsRefused() throws Exception {
        this.instructorRepository.findById(2).orElseThrow().setEnabled(false);

        this.mockMvc.perform(get(this.baseUrl + "/students/5").header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    private String login(String username) throws Exception {
        String body = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(username, "123456")))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + new JSONObject(body).getJSONObject("data").getString("token");
    }

}
