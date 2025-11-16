package com.app.relayhook.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.SystemIntegrations;
import com.app.relayhook.Models.UserIntegrationsCredentials;

@Repository
public interface UserIntegrationsCredentialsRepository extends JpaRepository<UserIntegrationsCredentials, Long> {

    Optional<UserIntegrationsCredentials> findBySystemIntegrations_IdAndUser_Id(Long integrationId, Long userId);
}
