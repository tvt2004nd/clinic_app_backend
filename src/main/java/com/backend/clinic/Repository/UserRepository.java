package com.backend.clinic.Repository;

import com.backend.clinic.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    @Query("""
            select u from User u
            where (:keyword is null or :keyword = ''
                   or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%')))
            order by u.createdAt desc
            """)
    List<User> searchUsers(@Param("keyword") String keyword);
}
