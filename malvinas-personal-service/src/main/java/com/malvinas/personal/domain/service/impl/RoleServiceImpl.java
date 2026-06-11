package com.malvinas.personal.domain.service.impl;

import com.malvinas.personal.application.dto.role.RoleResponse;
import com.malvinas.personal.domain.repository.RoleRepository;
import com.malvinas.personal.domain.service.RoleService;
import com.malvinas.personal.infrastructure.exception.ResourceNotFoundException;
import com.malvinas.personal.infrastructure.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepo;
    private final RoleMapper mapper;

    @Override
    public List<RoleResponse> findAll() {
        return roleRepo.findByIsActiveTrue().stream().map(mapper::toDto).toList();
    }

    @Override
    public RoleResponse findById(Integer id) {
        return roleRepo.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id));
    }

    @Override
    public RoleResponse findByCode(String code) {
        return roleRepo.findByCode(code).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Rol con codigo: " + code));
    }
}
