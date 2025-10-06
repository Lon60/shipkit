package com.shipkit.api.repositories;

import com.shipkit.api.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@NoRepositoryBean
public interface ProjectRepository extends JpaRepository<Project, UUID> {
}
