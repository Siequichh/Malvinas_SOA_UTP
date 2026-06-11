package com.malvinas.personal.domain.service;

import com.malvinas.personal.application.dto.role.RoleResponse;
import java.util.List;

public interface RoleService {
    List<RoleResponse> findAll();
    RoleResponse findById(Integer id);
    RoleResponse findByCode(String code);
}
