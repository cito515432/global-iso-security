package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        List<com.globalisosecurity.backend.models.Rol> activeRoles = RolPerfilUtil.activeRoles(usuario);
        if (activeRoles.isEmpty()) {
            throw new UsernameNotFoundException("El usuario no tiene un rol válido");
        }
        LinkedHashSet<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        for (var role : activeRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + RolPerfilUtil.effectiveProfile(role)));
            RolPerfilUtil.permissionAuthorities(role).forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission)));
        }
        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                true,
                true,
                true,
                true,
                List.copyOf(authorities));
    }
}
