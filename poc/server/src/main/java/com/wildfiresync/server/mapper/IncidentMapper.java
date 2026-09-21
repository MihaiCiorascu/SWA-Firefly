package com.wildfiresync.server.mapper;

import com.wildfiresync.server.dto.IncidentDto;
import com.wildfiresync.server.entity.Incident;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

	public Incident toEntity(IncidentDto dto) {
		return new Incident(dto.severity());
	}

	public IncidentDto toDto(Incident incident) {
		return new IncidentDto(incident.getSeverity());
	}

}
