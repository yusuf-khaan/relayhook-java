package com.app.relayhook.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.SystemIntegrations;

@Repository
public interface SystemIntegrationsRepository extends JpaRepository<SystemIntegrations, Long> {

    Page<SystemIntegrations> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);

    boolean existsByProvider(String provider);

    SystemIntegrations findByProvider(String provider);
}
