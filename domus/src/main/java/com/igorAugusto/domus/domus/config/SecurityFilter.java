package com.igorAugusto.domus.domus.config;

import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.UserRepository;
import com.igorAugusto.domus.domus.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class SecurityFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    @Autowired
    JwtService tokenService;

    @Autowired
    UserRepository userRepository;


    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/api/auth/")) {
            logger.info("Ignorando autenticação para: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        var token = this.recoverToken(request);
        if (token != null) {
            logger.debug("Validando token: {}", token);
            try {
                var login = tokenService.validateToken(token);
                logger.debug("Email extraído do token: {}", login);
                if (!login.isEmpty()) {
                    var optionalUser = userRepository.findByEmail(login);

                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get(); // extrai o valor do Optional
                        logger.info("Usuário encontrado: {}", user.getUsername());

                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                    } else {
                        logger.warn("Usuário não encontrado para email: {}", login);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                } else {
                    logger.warn("Token inválido ou expirado");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            } catch(Exception e) {
                logger.error("Erro ao validar token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
