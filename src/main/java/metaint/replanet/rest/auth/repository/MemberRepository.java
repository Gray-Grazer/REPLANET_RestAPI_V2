package metaint.replanet.rest.auth.repository;

import metaint.replanet.rest.auth.dto.MemberDto;
import metaint.replanet.rest.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Member findByProviderId(String kakaoTokenId);


    Member findEmailByPhone(String phone);

    Member findPhoneByEmail(String email);

}
