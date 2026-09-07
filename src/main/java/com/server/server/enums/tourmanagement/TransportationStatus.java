package com.server.server.enums.tourmanagement;

public enum TransportationStatus {
    AVAILABLE,    // Unit has free seats
    PARTIAL,      // Unit is partially booked but still has room
    FULL,         // Unit has 0 remaining seats
    MAINTENANCE,  // Unit is out of service regardless of seats
    INACTIVE      // Unit is decommissioned
}