package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.following.FollowingNews;
import com.eodigaljido.backend.domain.following.FollowingNewsRead;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowingNewsReadRepository extends JpaRepository<FollowingNewsRead, Long> {

    boolean existsByUserAndNews(User user, FollowingNews news);
}
