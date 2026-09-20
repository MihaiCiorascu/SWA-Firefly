package com.wildfiresync.server.repository;

import com.wildfiresync.server.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
}
