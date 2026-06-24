package com.malvinas.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
public class GatewayRoutesConfig {

    @Value("${routes.auth-uri:http://localhost:8086}")
    private String authUri;

    @Value("${routes.personal-uri:http://localhost:8081}")
    private String personalUri;

    @Value("${routes.vehiculos-uri:http://localhost:8082}")
    private String vehiculosUri;

    @Value("${routes.cargas-uri:http://localhost:8083}")
    private String cargasUri;

    @Value("${routes.rutas-uri:http://localhost:8084}")
    private String rutasUri;

    @Value("${routes.reportes-uri:http://localhost:8085}")
    private String reportesUri;

    // lb:// URIs need LoadBalancerFilterFunctions.lb(); direct URIs use BeforeFilterFunctions.uri()
    private RouterFunction<ServerResponse> proxy(String id, String pattern, String targetUri) {
        var builder = route(id).route(RequestPredicates.path(pattern), http());
        if (targetUri.startsWith("lb://")) {
            return builder.filter(lb(targetUri.substring(5))).build();
        }
        return builder.before(uri(targetUri)).build();
    }

    @Bean public RouterFunction<ServerResponse> authRoutes() {
        return proxy("auth-service", "/api/auth/**", authUri);
    }
    @Bean public RouterFunction<ServerResponse> personalEmployeesRoutes() {
        return proxy("personal-employees", "/api/employees/**", personalUri);
    }
    @Bean public RouterFunction<ServerResponse> personalRolesRoutes() {
        return proxy("personal-roles", "/api/roles/**", personalUri);
    }
    @Bean public RouterFunction<ServerResponse> personalAttendancesRoutes() {
        return proxy("personal-attendances", "/api/attendances/**", personalUri);
    }
    @Bean public RouterFunction<ServerResponse> vehiculosVehiclesRoutes() {
        return proxy("vehiculos-vehicles", "/api/vehicles/**", vehiculosUri);
    }
    @Bean public RouterFunction<ServerResponse> vehiculosTypesRoutes() {
        return proxy("vehiculos-types", "/api/vehicle-types/**", vehiculosUri);
    }
    @Bean public RouterFunction<ServerResponse> cargasRoutes() {
        return proxy("cargas-service", "/api/loads/**", cargasUri);
    }
    @Bean public RouterFunction<ServerResponse> rutasDispatchesRoutes() {
        return proxy("rutas-dispatches", "/api/dispatches/**", rutasUri);
    }
    @Bean public RouterFunction<ServerResponse> rutasPointsRoutes() {
        return proxy("rutas-points", "/api/delivery-points/**", rutasUri);
    }
    @Bean public RouterFunction<ServerResponse> reportesReportsRoutes() {
        return proxy("reportes-reports", "/api/reports/**", reportesUri);
    }
    @Bean public RouterFunction<ServerResponse> reportesKpisRoutes() {
        return proxy("reportes-kpis", "/api/kpis/**", reportesUri);
    }
}
