package com.nexuscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ================================================================
 *  NexusCore ERP v1.0 - Punto de Entrada Principal
 *  Proyecto Educativo: Matriz de Riesgos de Seguridad - SENA ADSO
 * ================================================================
 *
 *  Módulos activos:
 *   - com.nexuscore.api.customer  → Gestión de Datos de Clientes
 *   - com.nexuscore.api.auth      → Autenticación y Credenciales JWT
 *   - com.nexuscore.api.core      → Lógica Central del Negocio
 *   - com.nexuscore.api.financial → Módulo Financiero y Contable
 *   - com.nexuscore.api.salary    → Módulo de Salarios y Nómina
 *
 *  Consola H2: http://localhost:8080/h2-console
 *  API Base:   http://localhost:8080/api
 */
@SpringBootApplication
public class NexusCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusCoreApplication.class, args);
        System.out.println("""
                
                ╔═══════════════════════════════════════════════════════╗
                ║          NexusCore ERP v1.0 - INICIADO                ║
                ║  Sistema Educativo | Matriz de Riesgos SENA ADSO      ║
                ║  Puerto: 8080  |  H2 Console: /h2-console             ║
                ╚═══════════════════════════════════════════════════════╝
                """);
    }
}
