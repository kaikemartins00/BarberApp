package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return clienteRepository.findByEmail(username)
                .map(c -> (UserDetails) c)
                .or(() -> barbeiroRepository.findByEmail(username).map(b -> (UserDetails) b))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}