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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Profile("!test")
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepo;
    private final EmployeeRepository employeeRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedRoles();
            seedEmployees();
        } catch (Exception e) {
            log.warn("[seed] personal skipped: {}", e.getMessage());
        }
    }

    private void seedRoles() {
        if (roleRepo.count() > 0) return;
        roleRepo.saveAll(List.of(
            role("ADM", "Administrador",  "Acceso total al sistema"),
            role("SUP", "Supervisor",      "Gestiona despachos y flota"),
            role("MOV", "Movilizador",     "Control de carga"),
            role("DRV", "Conductor",       "Ejecuta despachos"),
            role("SEC", "Seguridad",       "Control de acceso/portería")
        ));
        log.info("[seed] 5 roles creados");
    }

    private void seedEmployees() {
        Role adm = roleRepo.findByCode("ADM").orElseThrow();
        Role sup = roleRepo.findByCode("SUP").orElseThrow();
        Role mov = roleRepo.findByCode("MOV").orElseThrow();
        Role drv = roleRepo.findByCode("DRV").orElseThrow();
        Role sec = roleRepo.findByCode("SEC").orElseThrow();

        // ponytail: assumed IDs 1-2=ADM, 3-5=SUP, 6-10=MOV, 11-40=DRV, 41-43=SEC on clean DB
        // ADM x2
        emp(adm, "00000001", "Admin",    "Malvinas",       "admin@malvinas.pe",    "admin123", null, null);
        emp(adm, "00000002", "Director", "Operaciones",    "director@malvinas.pe", "admin123", null, null);
        // SUP x3
        emp(sup, "00000011", "Carlos",  "Quispe Ramos",   "cquispe@malvinas.pe",  "sup12345", null, null);
        emp(sup, "00000012", "Rosa",    "Flores Taipe",   "rflores@malvinas.pe",  "sup12345", null, null);
        emp(sup, "00000013", "Jorge",   "Mendez Llanos",  "jmendez@malvinas.pe",  "sup12345", null, null);
        // MOV x5
        emp(mov, "00000021", "Luis",    "Torres Huanca",  "ltorres@malvinas.pe",  "mov12345", null, null);
        emp(mov, "00000022", "Ana",     "Vargas Ccoa",    "avargas@malvinas.pe",  "mov12345", null, null);
        emp(mov, "00000023", "Julio",   "Espinoza Cruz",  "jespinoza@malvinas.pe","mov12345", null, null);
        emp(mov, "00000024", "Maria",   "Quispe Leon",    "mquispe@malvinas.pe",  "mov12345", null, null);
        emp(mov, "00000025", "Eduardo", "Pinto Condori",  "epinto@malvinas.pe",   "mov12345", null, null);

        // DRV x30 — licencia categoría AIB para camiones
        String[][] drvData = {
            {"00000031","Pedro",    "Ramirez Soto"},    {"00000032","Juan",     "Torres Lima"},
            {"00000033","Miguel",   "Quispe Apaza"},    {"00000034","Roberto",  "Silva Cano"},
            {"00000035","Carlos",   "Vargas Tito"},     {"00000036","Luis",     "Huanca Mamani"},
            {"00000037","Marco",    "Espinoza Vera"},   {"00000038","Antonio",  "Mendez Cruz"},
            {"00000039","Fernando", "Pinto Lazo"},      {"00000040","Sergio",   "Castillo Rex"},
            {"00000041","David",    "Flores Cusi"},     {"00000042","Ricardo",  "Mamani Ticona"},
            {"00000043","Alex",     "Condori Larico"},  {"00000044","Pablo",    "Apaza Vilca"},
            {"00000045","Jesus",    "Choque Ramos"},    {"00000046","Victor",   "Cusi Gutierrez"},
            {"00000047","Eduardo",  "Ticona Paredes"},  {"00000048","Hector",   "Larico Ccama"},
            {"00000049","Omar",     "Vilca Tapia"},     {"00000050","Henry",    "Ramos Ayala"},
            {"00000051","Cesar",    "Gutierrez Puma"},  {"00000052","Mario",    "Paredes Layme"},
            {"00000053","Jorge",    "Ccama Cayo"},      {"00000054","Ruben",    "Tapia Quispe"},
            {"00000055","Felix",    "Ayala Huanca"},    {"00000056","Rolando",  "Puma Coila"},
            {"00000057","Gilberto", "Huanca Flores"},   {"00000058","Abel",     "Layme Mamani"},
            {"00000059","Nestor",   "Cayo Arce"},       {"00000060","Ramon",    "Quispe Benito"}
        };
        for (int i = 0; i < drvData.length; i++) {
            String[] d = drvData[i];
            String email = d[1].toLowerCase() + (i + 1) + "@malvinas.pe";
            String lic   = String.format("A-%05d", 10000 + i);
            emp(drv, d[0], d[1], d[2], email, "drv12345", lic, "AIB");
        }

        // SEC x3
        emp(sec, "00000071", "Marco",  "Sanchez Puma",  "msanchez@malvinas.pe", "sec12345", null, null);
        emp(sec, "00000072", "Rosa",   "Apaza Coila",   "rapaza@malvinas.pe",   "sec12345", null, null);
        emp(sec, "00000073", "Miguel", "Layme Torres",  "mlayme@malvinas.pe",   "sec12345", null, null);

        log.info("[seed] empleados sembrados — ADM×2 SUP×3 MOV×5 DRV×30 SEC×3");
    }

    private void emp(Role role, String dni, String fn, String ln, String email,
                     String pwd, String licNum, String licCat) {
        if (employeeRepo.existsByDni(dni)) return;
        employeeRepo.save(Employee.builder()
            .dni(dni).firstName(fn).lastName(ln).email(email)
            .passwordHash(passwordEncoder.encode(pwd))
            .role(role).hireDate(LocalDate.of(2023, 1, 15))
            .licenseNumber(licNum).licenseCategory(licCat)
            .isActive(true).build());
    }

    private Role role(String code, String name, String desc) {
        return Role.builder().code(code).name(name).description(desc).isActive(true).build();
    }
}
