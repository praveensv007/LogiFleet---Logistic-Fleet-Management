package com.logistics.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Reads "Authorization: Bearer <jwt>" from the STOMP CONNECT frame.
 * Anonymous clients (public tracking page) may connect but can only subscribe to
 * /topic/shipments/** and /topic/deliveries/**.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String username = jwtUtil.validate(header.substring(7), JwtUtil.TYPE_ACCESS);
                if (username != null) {
                    UserPrincipal principal = userDetailsService.loadUserByUsername(username);
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities()) {
                        @Override
                        public String getName() {
                            return principal.getUsername();
                        }
                    });
                }
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();
            boolean publicTopic = dest != null
                    && (dest.startsWith("/topic/shipments/") || dest.startsWith("/topic/deliveries/"));
            if (accessor.getUser() == null && !publicTopic) {
                throw new AccessDeniedException("Login required to subscribe to " + dest);
            }
        }
        return message;
    }
}
