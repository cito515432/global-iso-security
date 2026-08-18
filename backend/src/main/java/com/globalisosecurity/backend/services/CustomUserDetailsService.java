package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            throw new UsernameNotFoundException("El usuario no tiene un rol válido");
        }

        boolean enabled = !Boolean.FALSE.equals(usuario.getRol().getActivo());
        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                enabled,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre().trim().toUpperCase())));
    }
}
