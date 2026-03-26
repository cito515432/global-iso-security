/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.repositories.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    public List<Empresa> obtenerTodas() {
        return empresaRepository.findAll();
    }

    public Optional<Empresa> obtenerPorId(Long id) {
        return empresaRepository.findById(id);
    }

    public Empresa crearEmpresa(Empresa empresa) {
        validarNombreEmpresa(empresa.getNombre());

        Empresa existente = empresaRepository.findByNombre(empresa.getNombre().trim());
        if (existente != null) {
            throw new BadRequestException("Ya existe una empresa con ese nombre");
        }

        empresa.setNombre(empresa.getNombre().trim());
        Empresa nueva = empresaRepository.save(empresa);

        logAuditoriaService.registrarLog(
                "sistema",
                "CREAR",
                "EMPRESAS",
                "Se creó la empresa: " + nueva.getNombre(),
                "127.0.0.1"
        );

        return nueva;
    }

    public Empresa actualizarEmpresa(Long id, Empresa empresaActualizada) {
        Optional<Empresa> empresaOpt = empresaRepository.findById(id);

        if (empresaOpt.isEmpty()) {
            throw new ResourceNotFoundException("Empresa no encontrada");
        }

        validarNombreEmpresa(empresaActualizada.getNombre());

        Empresa empresaConMismoNombre = empresaRepository.findByNombre(empresaActualizada.getNombre().trim());
        if (empresaConMismoNombre != null && !empresaConMismoNombre.getId().equals(id)) {
            throw new BadRequestException("Ya existe una empresa con ese nombre");
        }

        Empresa empresa = empresaOpt.get();
        empresa.setNombre(empresaActualizada.getNombre().trim());

        Empresa actualizada = empresaRepository.save(empresa);

        logAuditoriaService.registrarLog(
                "sistema",
                "ACTUALIZAR",
                "EMPRESAS",
                "Se actualizó la empresa con ID: " + actualizada.getId(),
                "127.0.0.1"
        );

        return actualizada;
    }

    public void eliminarEmpresa(Long id) {
        if (!empresaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Empresa no encontrada");
        }

        empresaRepository.deleteById(id);

        logAuditoriaService.registrarLog(
                "sistema",
                "ELIMINAR",
                "EMPRESAS",
                "Se eliminó la empresa con ID: " + id,
                "127.0.0.1"
        );
    }

    private void validarNombreEmpresa(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new BadRequestException("El nombre de la empresa es obligatorio");
        }
    }
}