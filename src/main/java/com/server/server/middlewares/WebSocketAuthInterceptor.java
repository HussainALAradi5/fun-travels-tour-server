package com.server.server.middlewares;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.server.server.models.user.User;
import com.server.server.repositories.user.UserRepository;
import com.server.server.services.auth.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * Secures the STOMP channel: {@code CONNECT} must carry {@code Authorization: Bearer <token>}, and a
 * user may only subscribe to their own {@code /topic/notifications/<userId>} destination.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    public static final String NOTIFICATION_TOPIC_PREFIX = "/topic/notifications/";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (accessor.getCommand() == StompCommand.CONNECT) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            requireOwnTopic(accessor);
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessageDeliveryException("A bearer token is required.");
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            UserDetails user = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
            if (!user.isEnabled() || !jwtService.isTokenValid(token, user)) {
                throw new MessageDeliveryException("The token is invalid or expired.");
            }
            return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MessageDeliveryException("The token is invalid or expired.");
        }
    }

    private void requireOwnTopic(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessageDeliveryException("Connect with a token before subscribing.");
        }
        String destination = Objects.requireNonNullElse(accessor.getDestination(), "");
        Integer userId = userRepository.findByEmailIgnoreCase(accessor.getUser().getName()).map(User::getId).orElse(null);
        if (userId == null || !destination.equals(NOTIFICATION_TOPIC_PREFIX + userId)) {
            throw new MessageDeliveryException("You can only subscribe to your own notifications.");
        }
    }
}
