package com.reimagineafrica.notification.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
class NotifJwtFilter extends OncePerRequestFilter {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                String token = auth.substring(7);
                SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token).getPayload();
                String username = claims.getSubject();
                String role     = claims.get("role", String.class);
                String userId   = claims.get("userId", String.class);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    Object perms = claims.get("permissions");
                    if (perms instanceof List<?> p) p.forEach(x -> authorities.add(new SimpleGrantedAuthority(x.toString())));
                    var a = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    a.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(a);
                    req.setAttribute("userId", userId);
                }
            } catch (Exception e) { logger.warn("JWT error: " + e.getMessage()); }
        }
        chain.doFilter(req, res);
    }
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
class SecurityConfig {
    private final NotifJwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\"}");
            })).build();
    }
}

@Configuration
class RabbitMQConfig {
    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Bean public TopicExchange saccoExchange() { return new TopicExchange(exchange, true, false); }

    // Declare all queues this service consumes
    @Bean public org.springframework.amqp.core.Queue memberCreatedQ()       { return QueueBuilder.durable("member.created").build(); }
    @Bean public org.springframework.amqp.core.Queue loanApprovedQ()        { return QueueBuilder.durable("loan.approved").build(); }
    @Bean public org.springframework.amqp.core.Queue loanRejectedQ()        { return QueueBuilder.durable("loan.rejected").build(); }
    @Bean public org.springframework.amqp.core.Queue loanDisbursedQ()       { return QueueBuilder.durable("loan.disbursed").build(); }
    @Bean public org.springframework.amqp.core.Queue contributionMissedQ()  { return QueueBuilder.durable("contribution.missed").build(); }
    @Bean public org.springframework.amqp.core.Queue guarantorConsentQ()    { return QueueBuilder.durable("guarantor.consent.requested").build(); }
    @Bean public org.springframework.amqp.core.Queue dividendDeclaredQ()    { return QueueBuilder.durable("dividend.declared").build(); }

    @Bean public Binding memberCreatedBinding()      { return BindingBuilder.bind(memberCreatedQ()).to(saccoExchange()).with("member.created"); }
    @Bean public Binding loanApprovedBinding()       { return BindingBuilder.bind(loanApprovedQ()).to(saccoExchange()).with("loan.approved"); }
    @Bean public Binding loanRejectedBinding()       { return BindingBuilder.bind(loanRejectedQ()).to(saccoExchange()).with("loan.rejected"); }
    @Bean public Binding loanDisbursedBinding()      { return BindingBuilder.bind(loanDisbursedQ()).to(saccoExchange()).with("loan.disbursed"); }
    @Bean public Binding contributionMissedBinding() { return BindingBuilder.bind(contributionMissedQ()).to(saccoExchange()).with("contribution.missed"); }
    @Bean public Binding guarantorConsentBinding()   { return BindingBuilder.bind(guarantorConsentQ()).to(saccoExchange()).with("guarantor.consent.requested"); }
    @Bean public Binding dividendDeclaredBinding()   { return BindingBuilder.bind(dividendDeclaredQ()).to(saccoExchange()).with("dividend.declared"); }

    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
