package com.server.server.middlewares;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.server.server.models.user.User;
import com.server.server.repositories.user.UserRepository;
import com.server.server.services.auth.JwtService;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    private static final String EMAIL = "customer@funtravel.com";

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void connectWithoutTokenIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void connectWithValidTokenSetsTheUser() {
        UserDetails details = new org.springframework.security.core.userdetails.User(EMAIL, "x", List.of());
        when(jwtService.extractUsername("good")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(details);
        when(jwtService.isTokenValid("good", details)).thenReturn(true);

        Message<?> message = frame(StompCommand.CONNECT, "Bearer good", null, null);
        interceptor.preSend(message, channel);

        assertThat(StompHeaderAccessor.wrap(message).getUser()).isNotNull();
    }

    @Test
    void subscribingToAnotherUsersTopicIsRejected() {
        User user = new User();
        user.setId(5);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        var principal = new UsernamePasswordAuthenticationToken(EMAIL, null, List.of());

        interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/topic/notifications/5", principal), channel);
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, null, "/topic/notifications/6", principal), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscribingWithoutConnectingIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, null, "/topic/notifications/5", null), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private static Message<?> frame(StompCommand command, String authorization, String destination,
            UsernamePasswordAuthenticationToken user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
