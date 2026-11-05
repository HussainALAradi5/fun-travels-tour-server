package com.server.server.services.tourmanagement;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import java.util.Set;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.services.support.GenericTrackingService;
import com.server.server.services.notification.NotificationService;
import com.server.server.services.common.SystemSchedulingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Nightly tour housekeeping: cancels PENDING tours whose start date passed and completes finished ACTIVE tours. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourSchedulerService {

    private final TourRepository tourRepository;
    private final GenericTrackingService trackingService;
    private final SystemSchedulingService schedulingService;
    private final NotificationService notificationService;

    @PostConstruct
    public void init() {
        schedulingService.registerNightlyTask(this::autoProcessScheduledTours);
    }

    @Transactional
    public void autoProcessScheduledTours() {
        LocalDate today = LocalDate.now();

        // Task A: Auto-Cancel tours that passed their start date but are still PENDING
        List<Tour> pendingExpiredTours = tourRepository.findByStartDateBeforeAndStatus(today, GenericStatus.PENDING);

        for (Tour tour : pendingExpiredTours) {
            try {
                tour.setStatus(GenericStatus.CANCELLED);
                tour.setAvailableSlots(0); // Wipe inventory
                Tour savedTour = tourRepository.save(tour);

                // Log the system event (passing null for actor implies the System did it)
                trackingService.logEvent(
                        savedTour.getId(),
                        ReferenceType.TOUR,
                        "SYSTEM_AUTO_CANCEL",
                        "System automatically cancelled the tour because it passed the start date while still PENDING.",
                        null);

                log.info("System auto-cancelled expired pending tour: {}", tour.getTourNumber());
            } catch (Exception e) {
                log.error("Failed to auto-cancel tour {}: {}", tour.getTourNumber(), e.getMessage());
            }
        }

        // Task B: Auto-Complete tours that passed their end date and are
        // ACTIVE/APPROVED
        List<Tour> finishedTours = tourRepository.findByEndDateBeforeAndStatusIn(
                today,
                List.of(GenericStatus.ACTIVE, GenericStatus.APPROVED));

        for (Tour tour : finishedTours) {
            try {
                tour.setStatus(GenericStatus.COMPLETED);
                Tour savedTour = tourRepository.save(tour);

                // Log the system event
                trackingService.logEvent(
                        savedTour.getId(),
                        ReferenceType.TOUR,
                        "SYSTEM_AUTO_COMPLETE",
                        "System automatically marked tour as COMPLETED because the end date passed.",
                        null);

                if (savedTour.getTickets() != null) {
                    // Use a Set to prevent spamming a user who bought multiple tickets
                    java.util.Set<Integer> notifiedUsers = new java.util.HashSet<>();

                    for (Ticket ticket : savedTour.getTickets()) {
                        boolean eligibleForGreeting = ticket.getApprovalStatus() == GenericStatus.APPROVED
                                && ticket.getCustomer() != null;
                        if (eligibleForGreeting) {
                            Integer customerId = ticket.getCustomer().getId();

                            // Only send if we haven't already notified this specific user for this tour
                            if (!notifiedUsers.contains(customerId)) {
                                notificationService.sendTourCompletionGreeting(ticket);
                                notifiedUsers.add(customerId);
                            }
                        }
                        if (ticket.getTicketStatus() != TicketStatus.CANCELLED) {
                            ticket.setTicketStatus(TicketStatus.COMPLETED);
                            ticket.setApprovalStatus(GenericStatus.COMPLETED);
                        }
                    }
                }

                log.info("System auto-completed tour: {}", tour.getTourNumber());
            } catch (Exception e) {
                log.error("Failed to auto-complete tour {}: {}", tour.getTourNumber(), e.getMessage());
            }
        }
    }
}
