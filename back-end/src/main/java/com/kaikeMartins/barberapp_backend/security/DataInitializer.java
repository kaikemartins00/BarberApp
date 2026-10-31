package com.kaikeMartins.barberapp_backend.security;

import com.kaikeMartins.barberapp_backend.domain.entities.RoleEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.RoleTypeEnum;
import com.kaikeMartins.barberapp_backend.repository.RoleRepository;
import com.kaikeMartins.barberapp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.senha}")
    private String adminSenha;

    @Override
    public void run(String... args) {

        for (RoleTypeEnum roleEnum : RoleTypeEnum.values()) {
            if (roleRepository.findByName(roleEnum.name()).isEmpty()) {
                RoleEntity role = new RoleEntity();
                role.setName(roleEnum.name());
                roleRepository.save(role);
            }
        }

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        RoleEntity adminRole = roleRepository.findByName(RoleTypeEnum.ROLE_ADMIN.name())
                .orElseThrow(() -> new RuntimeException("Role ADMIN não encontrada"));

        UsuarioEntity admin = new UsuarioEntity();
        admin.setEmail(adminEmail);
        admin.setVerificado(true);
        admin.setCodigoVerificacao("123456");
        admin.setSenha(passwordEncoder.encode(adminSenha));

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);

        userRepository.save(admin);

        System.out.println("✅ Admin padrão criado: " + adminEmail);
    }
}