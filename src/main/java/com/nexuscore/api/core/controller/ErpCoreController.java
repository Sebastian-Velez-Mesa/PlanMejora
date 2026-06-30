package com.nexuscore.api.core.controller;

import com.nexuscore.api.core.service.ErpCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ================================================================
 *  MÓDULO: Core - Controlador REST de Lógica de Negocio
 *  Activo 3: Servidor de Aplicaciones (Lógica Central ERP)
 *  Ruta base: /api/core
 * ================================================================
 *
 * [VULNERABILIDAD ORIGINAL]:
 *   El servidor de aplicaciones no requería autenticación.
 *   Los procesos del negocio podían ser invocados externamente
 *   sin ningún control, permitiendo abusos como ejecución masiva
 *   de procesos (DoS interno) o extracción de información lógica.
 *
 * [MITIGACIÓN APLICADA]:
 *   1. Todos los endpoints requieren autenticación JWT (configurado en SecurityConfig).
 *   2. El servicio core usa IDs de correlación para trazabilidad.
 *   3. Se registran los errores internamente sin exponerlos al cliente.
 */
@Slf4j
@RestController
@RequestMapping("/api/core")
@RequiredArgsConstructor
public class ErpCoreController {

    private final ErpCoreService erpCoreService;

    /**
     * POST /api/core/proceso
     * Activa el algoritmo central del ERP.
     *
     * [MITIGACIÓN APLICADA]: Cualquier usuario autenticado puede activar
     * procesos generales, pero la lógica interna registra quién lo hizo.
     */
    @PostMapping("/proceso")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> ejecutarProceso(
            @RequestParam(defaultValue = "GENERAL") String modulo,
            Authentication auth) {

        Map<String, Object> resultado = erpCoreService.ejecutarProcesoCore(modulo, auth.getName());
        return ResponseEntity.ok(resultado);
    }

    /**
     * GET /api/core/estado
     * Retorna el estado de todos los módulos del ERP.
     *
     * [MITIGACIÓN APLICADA]: Endpoint de diagnóstico accesible para
     * cualquier usuario autenticado. Útil para monitoreo de disponibilidad.
     */
    @GetMapping("/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getEstadoModulos(Authentication auth) {
        Map<String, Object> estado = erpCoreService.obtenerEstadoModulos(auth.getName());
        return ResponseEntity.ok(estado);
    }
}
