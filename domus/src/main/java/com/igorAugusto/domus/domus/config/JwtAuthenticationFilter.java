package com.igorAugusto.domus.domus.config;

import com.igorAugusto.domus.domus.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1️⃣ Pega o token JWT do cabeçalho Authorization
        String jwt = getJwtFromRequest(request);

        // 2️⃣ Se o token for válido, autentica o usuário no contexto do Spring Security
        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            String email = jwtTokenProvider.getEmailFromJWT(jwt);
            UserDetails userDetails = userService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 3️⃣ Coloca o usuário autenticado no contexto da requisição
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 4️⃣ Continua o fluxo do filtro
        filterChain.doFilter(request, response);
    }

    // ⬇️ Esse método auxiliar pode ficar logo abaixo (caso ainda não exista)
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // remove o prefixo "Bearer "
        }
        return null;
    }
}
