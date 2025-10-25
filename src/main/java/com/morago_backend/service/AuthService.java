package com.morago_backend.service;

import com.morago_backend.entity.*;
import com.morago_backend.payload.AuthResponse;
import com.morago_backend.payload.ClientSignupRequest;
import com.morago_backend.payload.SignupResponse;
import com.morago_backend.payload.TranslatorSignupRequest;
import com.morago_backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtSecret;
    private final long jwtExpirationMs;
    private final long refreshTokenExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${security.jwt.secret}") String base64Secret,
                       @Value("${security.jwt.expiration-ms:3600000}") long jwtExpirationMs,
                       @Value("${security.jwt.refresh-expiration-ms:86400000}") long refreshTokenExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String login(String phone, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(phone, password));

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return generateAccessToken(user);
    }

    public SignupResponse signupClient(ClientSignupRequest request) {
        // Check if phone already exists
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        // Create user
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setIsActive(true);
        user.setIsDebtor(false);
        
        // Add CLIENT role
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.CLIENT);
        user.setRoles(roles);

        // Create user profile
        UserProfile userProfile = new UserProfile();
        userProfile.setCreatedAt(LocalDateTime.now());
        userProfile.setUpdatedAt(LocalDateTime.now());
        userProfile.setIsFreeCallMade(false);
        userProfile.setUser(user);
        user.setUserProfile(userProfile);

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = generateToken(savedUser);

        // Return response
        return new SignupResponse(
                token,
                savedUser.getId(),
                savedUser.getPhone(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }

    public SignupResponse signupTranslator(TranslatorSignupRequest request) {
        // Check if phone already exists
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        // Create user
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setIsActive(true);
        user.setIsDebtor(false);
        
        // Add INTERPRETER role
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.INTERPRETER);
        user.setRoles(roles);

        // Create translator profile
        TranslatorProfile translatorProfile = new TranslatorProfile();
        translatorProfile.setEmail(request.getEmail());
        translatorProfile.setDateOfBirth(request.getDateOfBirth());
        translatorProfile.setLevelOfKorean(request.getLevelOfKorean());
        translatorProfile.setCreatedAt(LocalDateTime.now());
        translatorProfile.setUpdatedAt(LocalDateTime.now());
        translatorProfile.setIsAvailable(false);
        translatorProfile.setIsOnline(false);
        translatorProfile.setIsVerified(false); // Translator needs to be verified by admin
        translatorProfile.setUser(user);
        user.setTranslatorProfile(translatorProfile);

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = generateToken(savedUser);

        // Return response
        return new SignupResponse(
                token,
                savedUser.getId(),
                savedUser.getPhone(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }

    private String generateToken(User user) {
        return generateAccessToken(user);
    }

    /**
     * Generates an access token with configured expiration time
     * @param user User for whom to generate the token
     * @return JWT access token
     */
    public String generateAccessToken(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .toList();

        long now = System.currentTimeMillis();
        long exp = now + jwtExpirationMs;

        return Jwts.builder()
                .setSubject(user.getPhone())
                .claim("roles", roles)
                .claim("id", user.getId())
                .claim("type", "access")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(exp))
                .signWith(jwtSecret)
                .compact();
    }

    /**
     * Generates a refresh token with extended expiration time
     * @param user User for whom to generate the refresh token
     * @return JWT refresh token
     */
    public String generateRefreshToken(User user) {
        long now = System.currentTimeMillis();
        long exp = now + refreshTokenExpirationMs;

        return Jwts.builder()
                .setSubject(user.getPhone())
                .claim("id", user.getId())
                .claim("type", "refresh")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(exp))
                .signWith(jwtSecret)
                .compact();
    }

    /**
     * Gets the configured JWT expiration time in milliseconds
     * @return JWT expiration time in ms
     */
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Gets the configured refresh token expiration time in milliseconds
     * @return Refresh token expiration time in ms
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    /**
     * Creates enhanced auth response with both access and refresh tokens
     * @param user User for whom to generate tokens
     * @return AuthResponse with access token, refresh token, and expiration details
     */
    public AuthResponse createAuthResponse(User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        
        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        logger.info("Generated tokens for user: {} with access token expiration: {}ms, refresh token expiration: {}ms",
                user.getPhone(), jwtExpirationMs, refreshTokenExpirationMs);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtExpirationMs,
                refreshTokenExpirationMs,
                user.getId(),
                user.getPhone(),
                user.getFirstName(),
                user.getLastName(),
                roles
        );
    }

    /**
     * Refreshes access token using a valid refresh token
     * @param refreshToken The refresh token
     * @return New AuthResponse with fresh access token
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecret)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("Invalid token type. Expected refresh token.");
            }

            String phone = claims.getSubject();
            User user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            logger.info("Refreshing access token for user: {}", phone);
            return createAuthResponse(user);

        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired refresh token", e);
        }
    }

    /**
     * Validates a token and extracts the phone number
     * @param token JWT token to validate
     * @return Phone number from the token
     */
    public String validateTokenAndGetPhone(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecret)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }
}
