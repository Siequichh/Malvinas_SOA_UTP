package com.malvinas.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

/**
 * Routes for Spring Cloud Gateway Server MVC (5.0.x).
 *
 * Correct pattern per official docs:
 *   route(id).route(predicate, http()).before(uri("target")).build()
 *
 * BeforeFilterFunctions.uri() sets MvcUtils.GATEWAY_REQUEST_URL_ATTR on the request
 * so that HandlerFunctions.http() (LookupProxyExchangeHandlerFunction) can resolve
 * the target URI at proxy time.
 */
@Configuration
public class GatewayRoutesConfig {

    // Override these in application-dev.yml or env vars for prod (use lb://service-name)
    @Value("${routes.auth-uri:http://localhost:8086}")
    private String authUri;

    @Value("${routes.personal-uri:lb://personal-service}")
    private String personalUri;

    @Value("${routes.vehiculos-uri:lb://vehiculos-service}")
    private String vehiculosUri;

    @Value("${routes.cargas-uri:lb://cargas-service}")
    private String cargasUri;

    @Value("${routes.rutas-uri:lb://rutas-service}")
    private String rutasUri;

    @Value("${routes.reportes-uri:lb://reportes-service}")
    private String reportesUri;

    @Bean
    public RouterFunction<ServerResponse> authRoutes() {
        return route("auth-service")
                .route(RequestPredicates.path("/api/auth/**"), http())
                .before(uri(authUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> personalEmployeesRoutes() {
        return route("personal-employees")
                .route(RequestPredicates.path("/api/employees/**"), http())
                .before(uri(personalUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> personalRolesRoutes() {
        return route("personal-roles")
                .route(RequestPredicates.path("/api/roles/**"), http())
                .before(uri(personalUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> personalAttendancesRoutes() {
        return route("personal-attendances")
                .route(RequestPredicates.path("/api/attendances/**"), http())
                .before(uri(personalUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> vehiculosVehiclesRoutes() {
        return route("vehiculos-vehicles")
                .route(RequestPredicates.path("/api/vehicles/**"), http())
                .before(uri(vehiculosUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> vehiculosTypesRoutes() {
        return route("vehiculos-types")
                .route(RequestPredicates.path("/api/vehicle-types/**"), http())
                .before(uri(vehiculosUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> cargasRoutes() {
        return route("cargas-service")
                .route(RequestPredicates.path("/api/loads/**"), http())
                .before(uri(cargasUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> rutasDispatchesRoutes() {
        return route("rutas-dispatches")
                .route(RequestPredicates.path("/api/dispatches/**"), http())
                .before(uri(rutasUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> rutasPointsRoutes() {
        return route("rutas-points")
                .route(RequestPredicates.path("/api/delivery-points/**"), http())
                .before(uri(rutasUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> reportesReportsRoutes() {
        return route("reportes-reports")
                .route(RequestPredicates.path("/api/reports/**"), http())
                .before(uri(reportesUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> reportesKpisRoutes() {
        return route("reportes-kpis")
                .route(RequestPredicates.path("/api/kpis/**"), http())
                .before(uri(reportesUri))
                .build();
    }
}
