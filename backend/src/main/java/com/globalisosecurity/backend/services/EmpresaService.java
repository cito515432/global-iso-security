/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

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

    public List<Empresa> obtenerTodas() {
        return empresaRepository.findAll();
    }

    public Optional<Empresa> obtenerPorId(Long id) {
        return empresaRepository.findById(id);
    }

    public Empresa crearEmpresa(Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    public Empresa actualizarEmpresa(Long id, Empresa empresaActualizada) {
        Optional<Empresa> empresa = empresaRepository.findById(id);

        if (empresa.isPresent()) {
            Empresa e = empresa.get();
            e.setNombre(empresaActualizada.getNombre());
            return empresaRepository.save(e);
        }

        return null;
    }

    public void eliminarEmpresa(Long id) {
        empresaRepository.deleteById(id);
    }
}
