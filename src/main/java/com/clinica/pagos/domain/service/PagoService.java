package com.clinica.pagos.domain.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.clinica.pagos.domain.dto.PagoDTO;
import com.clinica.pagos.domain.repository.IPago;
import com.clinica.pagos.infrastructure.client.CitaClient;
import com.clinica.pagos.infrastructure.client.dto.CitaDTO;

@Service
public class PagoService {

    @Autowired
    private IPago repo;
    
    @Autowired
    private CitaClient citaClient;

    public List<PagoDTO> obtenerTodo() { 
        List<PagoDTO> pagos = repo.getAll();
        pagos.forEach(this::enriquecerPago);
        return pagos;
    }

    public Optional<PagoDTO> obtenerPorId(Long id) {
        Optional<PagoDTO> pago = repo.getById(id);
        pago.ifPresent(this::enriquecerPago);
        return pago;
    }

    public PagoDTO guardar(PagoDTO dto) {
        // Validar que la cita exista
        try {
            citaClient.getCitaById(dto.getCitaId());
        } catch (Exception e) {
            throw new RuntimeException("La cita no existe");
        }
        
        // Si no se especifica la fecha, usar la fecha actual
        if (dto.getFechaPago() == null && !"PENDIENTE".equals(dto.getEstado())) {
            dto.setFechaPago(LocalDateTime.now());
        }
        
        // Por defecto, el estado es PENDIENTE
        if (dto.getEstado() == null) {
            dto.setEstado("PENDIENTE");
        }
        
        PagoDTO pagoGuardado = repo.save(dto);
        enriquecerPago(pagoGuardado);
        return pagoGuardado;
    }

    public PagoDTO actualizar(Long id, PagoDTO dto) {
        Optional<PagoDTO> pagoExistente = repo.getById(id);
        if (pagoExistente.isPresent()) {
            PagoDTO pagoActual = pagoExistente.get();
            
            // Si cambia el estado a PAGADO, notificar a Citas
            if ("PAGADO".equals(dto.getEstado()) && !"PAGADO".equals(pagoActual.getEstado())) {
                dto.setFechaPago(LocalDateTime.now());
                
                // Actualizar el pago
                PagoDTO pagoActualizado = repo.update(id, dto);
                
                // Notificar a Citas
                try {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("estadoPago", "PAGADO");
                    payload.put("pagoId", pagoActualizado.getId());
                    
                    citaClient.actualizarEstadoPago(pagoActualizado.getCitaId(), payload);
                } catch (Exception e) {
                    System.err.println("Error al notificar a citas: " + e.getMessage());
                }
                
                enriquecerPago(pagoActualizado);
                return pagoActualizado;
            } else {
                PagoDTO pagoActualizado = repo.update(id, dto);
                enriquecerPago(pagoActualizado);
                return pagoActualizado;
            }
        }
        return null;
    }
    
    public PagoDTO procesarPago(Long id, String metodoPago) {
        Optional<PagoDTO> pagoExistente = repo.getById(id);
        if (pagoExistente.isPresent()) {
            PagoDTO pagoDTO = pagoExistente.get();
            pagoDTO.setEstado("PAGADO");
            pagoDTO.setMetodoPago(metodoPago);
            pagoDTO.setFechaPago(LocalDateTime.now());
            
            // Actualizar el pago
            PagoDTO pagoActualizado = repo.update(id, pagoDTO);
            
            // Notificar a Citas
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("estadoPago", "PAGADO");
                payload.put("pagoId", pagoActualizado.getId());
                
                citaClient.actualizarEstadoPago(pagoActualizado.getCitaId(), payload);
            } catch (Exception e) {
                System.err.println("Error al notificar a citas: " + e.getMessage());
            }
            
            enriquecerPago(pagoActualizado);
            return pagoActualizado;
        }
        return null;
    }

    public boolean eliminar(Long id) {
        return repo.delete(id);
    }
    
    public List<PagoDTO> obtenerPorCita(Long citaId) {
        List<PagoDTO> pagos = repo.getByCitaId(citaId);
        pagos.forEach(this::enriquecerPago);
        return pagos;
    }
    
    public List<PagoDTO> obtenerPorEstado(String estado) {
        List<PagoDTO> pagos = repo.getByEstado(estado);
        pagos.forEach(this::enriquecerPago);
        return pagos;
    }
    
    // Método para enriquecer el pago con datos de la cita
    private void enriquecerPago(PagoDTO pago) {
        try {
            CitaDTO cita = citaClient.getCitaById(pago.getCitaId());
            pago.setNombrePaciente(cita.getNombrePaciente());
            pago.setNombreMedico(cita.getNombreMedico());
        } catch (Exception e) {
            pago.setNombrePaciente("Paciente no encontrado");
            pago.setNombreMedico("Médico no encontrado");
        }
    }
}
