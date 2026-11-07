package team.projectpulse.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<PeerEvaluationUser, Integer> {

    Optional<PeerEvaluationUser> findByUsername(String username);

    Optional<PeerEvaluationUser> findByEmail(String email);

    List<PeerEvaluationUser> findByEmailIn(Collection<String> emails);

    /**
     * The account's enabled flag alone, or empty if no such account exists. Read on every bearer-token request
     * (FR-SEC-active-account), so it selects one column instead of loading the user and its eager associations.
     */
    @Query("select u.enabled from PeerEvaluationUser u where u.id = :id")
    Optional<Boolean> findEnabledById(@Param("id") Integer id);

}
