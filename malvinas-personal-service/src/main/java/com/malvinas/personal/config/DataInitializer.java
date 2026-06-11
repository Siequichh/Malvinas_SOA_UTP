package com.malvinas.personal.config;

import com.malvinas.personal.domain.entity.Employee;
import com.malvinas.personal.domain.entity.Role;
import com.malvinas.personal.domain.repository.EmployeeRepository;
import com.malvinas.personal.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepo;
    private final EmployeeRepository employeeRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleRepo.count() == 0) {
            log.info("Inicializando roles del sistema...");
            List<Role> roles = List.of(
                Role.builder().code("ADM").name("Administrador").description("Acceso total al sistema").isActive(true).build(),
                Role.builder().code("SUP").name("Supervisor").description("Gestiona despachos y flota").isActive(true).build(),
                Role.builder().code("MOV").name("Movilizador").description("Control de carga").isActive(true).build(),
                Role.builder().code("DRV").name("Conductor").description("Ejecuta despachos").isActive(true).build(),
                Role.builder().code("SEC").name("Seguridad").description("Control de acceso/porteria").isActive(true).build()
            );
            roleRepo.saveAll(roles);
            log.info("Roles creados: {}", roles.size());
        }

        if (employeeRepo.count() == 0) {
            log.info("Creando empleado administrador inicial...");
            Role adminRole = roleRepo.findByCode("ADM").orElseThrow();
            Employee admin = Employee.builder()
                    .dni("00000001")
                    .firstName("Admin")
                    .lastName("Malvinas")
                    .email("admin@malvinas.pe")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .hireDate(LocalDate.now())
                    .isActive(true)
                    .build();
            employeeRepo.save(admin);
            log.info("Admin creado: admin@malvinas.pe / admin123");
        }
    }
}
