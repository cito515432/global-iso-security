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
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<?> login(String email, String password) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

        // Usuario no existe
        if (usuario.isEmpty()) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }

        // Contraseña incorrecta
        if (!passwordEncoder.matches(password, usuario.get().getPassword())) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }

        // Generar token
        String token = jwtUtil.generarToken(
            email,
            usuario.get().getRol().getNombre()
        );

        // Respuesta
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("token", token);
        respuesta.put("rol", usuario.get().getRol().getNombre());
        respuesta.put("nombre", usuario.get().getNombre());

        return ResponseEntity.ok(respuesta);
    }
}