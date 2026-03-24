/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Sector;
import com.globalisosecurity.backend.repositories.SectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SectorService {

    @Autowired
    private SectorRepository sectorRepository;

    public List<Sector> obtenerTodos() {
        return sectorRepository.findAll();
    }

    public Optional<Sector> obtenerPorId(Long id) {
        return sectorRepository.findById(id);
    }

    public Sector crearSector(Sector sector) {
        return sectorRepository.save(sector);
    }

    public Sector actualizarSector(Long id, Sector sectorActualizado) {
        Optional<Sector> sector = sectorRepository.findById(id);

        if (sector.isPresent()) {
            Sector s = sector.get();
            s.setNombre(sectorActualizado.getNombre());
            return sectorRepository.save(s);
        }

        return null;
    }

    public void eliminarSector(Long id) {
        sectorRepository.deleteById(id);
    }
}