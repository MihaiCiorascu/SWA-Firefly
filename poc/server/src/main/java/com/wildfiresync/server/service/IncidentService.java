package com.wildfiresync.server.service;

import com.wildfiresync.server.dto.IncidentDto;
import com.wildfiresync.server.entity.Incident;
import com.wildfiresync.server.mapper.IncidentMapper;
import com.wildfiresync.server.repository.IncidentRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class IncidentService {

	private final IncidentRepository incidentRepository;

	private final IncidentMapper incidentMapper;

	public IncidentService(IncidentRepository incidentRepository, IncidentMapper incidentMapper) {
		this.incidentRepository = incidentRepository;
		this.incidentMapper = incidentMapper;
	}

	@Transactional
	public IncidentDto save(@Valid IncidentDto dto) {
		Incident incident = incidentRepository.save(incidentMapper.toEntity(dto));
		return incidentMapper.toDto(incident);
	}

}
