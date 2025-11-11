package com.app.relayhook.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.Users;
import com.app.relayhook.Models.Workflow;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long>{

    Optional<Users> findByEmail(String email);

    Optional<Users> findByEmailAndPassword(String string, String string2);

}
