package com.musclefocus.backend.repository;

import com.musclefocus.backend.model.Buddy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuddyRepository extends JpaRepository<Buddy, Long> {
    List<Buddy> findAllByOrderByCreatedAtDesc();
    List<Buddy> findByStatusOrderByCreatedAtDesc(String status);
}
