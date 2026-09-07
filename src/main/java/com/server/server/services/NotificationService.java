package com.server.server.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.Notification.NotificationType;
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.Notification;
import com.server.server.models.User;
import com.server.server.models.UserRequest;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.NotificationRepository;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.repositories.tourmanagement.TourRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TourRepository tourRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Lazy
    @Autowired
    private NotificationService self;

    public void sendUserRequestNotification(UserRequest request, NotificationType type) {
        // Create a truncated title so the notification header doesn't break UI if the
        // title is huge
        String shortTitle = request.getTitle() != null && request.getTitle().length() > 30
                ? request.getTitle().substring(0, 30) + "..."
                : request.getTitle();

        String title = switch (type) {
            case REQUEST_CREATED -> "Submitted: " + shortTitle;
            case REQUEST_ASSIGNED -> "Assigned: " + shortTitle;
            case REQUEST_SOLVED -> "Solved: " + shortTitle;
            case REQUEST_REJECTED -> "Rejected: " + shortTitle;
            default -> "Update: " + shortTitle;
        };

        String message = switch (type) {
            case REQUEST_CREATED ->
                "Your request '" + request.getTitle() + "' has been successfully submitted and is pending review.";
            case REQUEST_ASSIGNED -> {
                String agentName = (request.getAssignedTo() != null) ? request.getAssignedTo().getName() : "an agent";
                yield "Your request '" + request.getTitle() + "' has been assigned to agent " + agentName + ".";
            }
            case REQUEST_SOLVED ->
                "Great news! Your request '" + request.getTitle() + "' has been resolved.";
            case REQUEST_REJECTED ->
                "Your request '" + request.getTitle() + "' was reviewed and unfortunately rejected.";
            default ->
                "There is a new update regarding your request: '" + request.getTitle() + "'.";
        };

        self.sendNotification(request.getUser(), title, message, type, request.getId(), ReferenceType.USER_REQUEST);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(User recipient, String title, String message, NotificationType type, Integer refId,
            ReferenceType refType) {
        try {
            User managedUser = userRepository.findById(recipient.getId())
                    .orElseThrow(() -> new RuntimeException("Recipient not found"));

            Notification notification = Notification.builder()
                    .recipient(managedUser)
                    .title(title)
                    .message(message)
                    .type(type)
                    .isRead(false)
                    .referenceId(refId)
                    .referenceType(refType)
                    .build();

            notificationRepository.save(notification);

            // --- NEW REAL-TIME BROADCAST ---
            // Grab the new counts and send them directly to this specific user's topic
            Map<String, Long> counts = getNotificationCounts(managedUser.getId());
            messagingTemplate.convertAndSend("/topic/notifications/" + managedUser.getId(), counts);

        } catch (Exception e) {
            log.error("Async Notification Failure: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Integer userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void scheduleSevenDayReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(7);
        List<Tour> toursStartingSoon = tourRepository.findByStartDateAndStatus(targetDate, GenericStatus.ACTIVE);

        for (Tour tour : toursStartingSoon) {
            List<Ticket> tickets = ticketRepository.findByTourIdAndTicketStatus(tour.getId(), TicketStatus.CONFIRMED);
            for (Ticket ticket : tickets) {
                self.sendNotification(
                        ticket.getCustomer(),
                        "Tour Reminder",
                        "Your tour '" + tour.getTitle() + "' starts in 7 days!",
                        NotificationType.ONE_WEEK_TRAVEL_REMINDER,
                        tour.getId(),
                        ReferenceType.TOUR);
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getNotificationCounts(Integer userId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("unread", notificationRepository.countByRecipientIdAndIsReadFalse(userId));
        counts.put("read", notificationRepository.countByRecipientIdAndIsReadTrue(userId));
        return counts;
    }

    public void sendTicketAutoCancellationNotification(Ticket ticket) {
        Tour tour = ticket.getTour();
        String title = "Ticket Auto-Cancelled";

        String message = String.format(
                "Your ticket for '%s' (No: %s) has been automatically cancelled because " +
                        "it was not approved/confirmed before the tour start date (%s).",
                tour.getTitle(),
                ticket.getTicketNumber(),
                tour.getStartDate().toString());

        // Reference the ticket so the user can click the notification to see the
        // expired booking
        self.sendNotification(
                ticket.getCustomer(),
                title,
                message,
                NotificationType.CANCELLATION_ALERT,
                ticket.getId(),
                ReferenceType.TICKET);
    }

    public void sendTourCompletionGreeting(Ticket ticket) {
        Tour tour = ticket.getTour();
        User customer = ticket.getCustomer();

        // Check if this customer bought tickets for family/friends
        long groupSize = tour.getTickets().stream()
                .filter(t -> t.getCustomer().getId().equals(customer.getId())
                        && t.getApprovalStatus() == GenericStatus.APPROVED)
                .count();

        // Safely get the destination city name
        String destination = (tour.getEndCity() != null) ? tour.getEndCity().getName() : "your destination";

        String title = "Welcome back from " + destination + "!";

        // Dynamically change the text if they traveled with family/friends
        String groupText = groupSize > 1 ? " and your group" : "";

        String message = String.format(
                "Hi %s! We hope you%s had an amazing time on the '%s' tour. " +
                        "Thank you for choosing us for your journey. We'd love to hear about your experience " +
                        "and look forward to seeing you on your next adventure!",
                customer.getName(),
                groupText,
                tour.getTitle());

        // Push the in-app notification linking back to the completed Tour
        self.sendNotification(
                customer,
                title,
                message,
                NotificationType.TOUR_COMPLETED, // Assuming you use GENERAL_UPDATE
                tour.getId(),
                ReferenceType.TOUR);
    }

    @Transactional(readOnly = true)
    public List<Notification> filterUserNotifications(
            Integer userId, String search, Boolean isRead,
            NotificationType type, ReferenceType refType,
            LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;
        return notificationRepository.findFilteredNotifications(
                userId, search, isRead, type, refType, startDateTime, endDateTime);
    }

}