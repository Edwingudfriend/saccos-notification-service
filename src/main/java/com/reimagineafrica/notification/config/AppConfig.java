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
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableScheduling;
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

// ─────────────────────────────────────────────────────────────────
// JWT FILTER
// ─────────────────────────────────────────────────────────────────
@Component
class NotificationJwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();

            String username = claims.getSubject();
            String role     = claims.get("role", String.class);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                Object perms = claims.get("permissions");
                if (perms instanceof List<?> permList) {
                    permList.forEach(p -> authorities.add(new SimpleGrantedAuthority(p.toString())));
                }
                var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute("tenantId", claims.get("tenantId", String.class));
                request.setAttribute("userId",   claims.get("userId",   String.class));
            }
        } catch (Exception e) {
            logger.warn("JWT validation failed: " + e.getMessage());
        }
        chain.doFilter(request, response);
    }
}

// ─────────────────────────────────────────────────────────────────
// SECURITY CONFIG
// ─────────────────────────────────────────────────────────────────
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableScheduling
@RequiredArgsConstructor
class SecurityConfig {

    private final NotificationJwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\"}");
                })
            )
            .build();
    }
}

// ─────────────────────────────────────────────────────────────────
// RABBITMQ — all queues declared here so consumers can start
// ─────────────────────────────────────────────────────────────────
@Configuration
class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange saccoExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean public org.springframework.amqp.core.Queue memberCreatedQueue()              { return QueueBuilder.durable("member.created").build(); }
    @Bean public org.springframework.amqp.core.Queue loanApprovedQueue()               { return QueueBuilder.durable("loan.approved").build(); }
    @Bean public org.springframework.amqp.core.Queue loanRejectedQueue()               { return QueueBuilder.durable("loan.rejected").build(); }
    @Bean public org.springframework.amqp.core.Queue loanDisbursedQueue()              { return QueueBuilder.durable("loan.disbursed").build(); }
    @Bean public org.springframework.amqp.core.Queue contributionMissedQueue()         { return QueueBuilder.durable("contribution.missed").build(); }
    @Bean public org.springframework.amqp.core.Queue guarantorConsentQueue()           { return QueueBuilder.durable("guarantor.consent.requested").build(); }
    @Bean public org.springframework.amqp.core.Queue dividendDeclaredQueue()           { return QueueBuilder.durable("dividend.declared").build(); }

    @Bean public Binding memberCreatedBinding()       { return BindingBuilder.bind(memberCreatedQueue()).to(saccoExchange()).with("member.created"); }
    @Bean public Binding loanApprovedBinding()        { return BindingBuilder.bind(loanApprovedQueue()).to(saccoExchange()).with("loan.approved"); }
    @Bean public Binding loanRejectedBinding()        { return BindingBuilder.bind(loanRejectedQueue()).to(saccoExchange()).with("loan.rejected"); }
    @Bean public Binding loanDisbursedBinding()       { return BindingBuilder.bind(loanDisbursedQueue()).to(saccoExchange()).with("loan.disbursed"); }
    @Bean public Binding contributionMissedBinding()  { return BindingBuilder.bind(contributionMissedQueue()).to(saccoExchange()).with("contribution.missed"); }
    @Bean public Binding guarantorConsentBinding()    { return BindingBuilder.bind(guarantorConsentQueue()).to(saccoExchange()).with("guarantor.consent.requested"); }
    @Bean public Binding dividendDeclaredBinding()    { return BindingBuilder.bind(dividendDeclaredQueue()).to(saccoExchange()).with("dividend.declared"); }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }

    @Bean
    public org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
            rabbitListenerContainerFactory(ConnectionFactory cf) {
        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory =
                new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
