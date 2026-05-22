/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<?> login(String email, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }

        Usuario usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }

        // No se bloquea el inicio de sesión por el estado del rol.
        // El estado activo/inactivo se mantiene para la administración de roles,
        // pero no debe impedir que los usuarios existentes ingresen después de una migración.
        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().getNombre());

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("token", token);
        respuesta.put("email", usuario.getEmail());
        respuesta.put("nombre", usuario.getNombre());
        respuesta.put("rol", usuario.getRol().getNombre());
        respuesta.put("rolId", usuario.getRol().getId());
        respuesta.put("rolActivo", usuario.getRol().getActivo());
        respuesta.put("permisos", usuario.getRol().getPermisos());

        return ResponseEntity.ok(respuesta);
    }
}