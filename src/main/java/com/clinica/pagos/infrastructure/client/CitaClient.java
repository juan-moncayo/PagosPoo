package com.clinica.pagos.infrastructure.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.clinica.pagos.infrastructure.client.dto.CitaDTO;

@FeignClient(name = "citas-service", url = "${citas.service.url}")
public interface CitaClient {
    
    @GetMapping("/citas/{id}")
    CitaDTO getCitaById(@PathVariable("id") Long id);
    
    @PutMapping("/citas/{id}/estado-pago")
    CitaDTO actualizarEstadoPago(@PathVariable("id") Long id, @RequestBody Map<String, Object> payload);
}
