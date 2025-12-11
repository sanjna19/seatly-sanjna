### Recurring Desk Booking -- Implementation Overview

This update adds support for weekly recurring desk bookings in the Seatly application.

The goal was to extend the existing booking system while keeping the current API, validation rules, and availability view unchanged.

### Functional Requirements Covered

1\. Weekly Recurrence Only

The system now supports creating bookings that repeat once per week on the same day and time as the initial booking.

Example:

"Book Desk A every Monday at 10:00am for 4 weeks."

Only this recurrence pattern is implemented, as requested.

No monthly/daily rules, intervals, or infinite series were added.

2\. Validation & Conflict Rules

Before saving any recurring booking:

-   All future weekly occurrences are computed.
-   Each occurrence is checked for time conflicts using the existing overlap logic.
-   If any occurrence overlaps an existing booking:

-   The entire recurring booking is rejected.
-   No partial bookings are created.
-   A 400 BAD_REQUEST error is returned with a clear message.

Single (non-recurring) bookings continue to work exactly as before.

3\. Viewing / Managing Bookings

Existing views did not require changes.

Recurring bookings appear the same as individual bookings:

-   They block their respective time slots in the availability table.
-   No new view, editing, or deletion logic was introduced (per requirements).

 Design Approach

The main idea was to extend the existing createBooking flow without modifying any of the persistence models or introducing new tables.

Key design choices:

-   No schema changes\
    Recurring bookings are stored as individual bookings, one per week.
-   Minimal API change\
    Added two optional fields:

    {\
      "recurring": true,\
      "weeks": 4\
    }
-   Atomic behavior\
    All bookings must be valid before saving any of them.
-   Same conflict logic reused\
    Uses the existing existsOverlappingBooking repository query.
-   Simple, predictable behavior\
    No special handling or metadata needed for recurring series.

🛠 Backend Logic Summary

New fields added to CreateBookingRequest:

val recurring: Boolean = false\
val weeks: Int = 1

Recurrence algorithm:

1.  Normalize start/end timestamps (existing behavior).
2.  If not recurring → fallback to original logic.
3.  If recurring:

-   Generate (start + i weeks, end + i weeks) for each week.
-   Validate all occurrences for overlap.
-   Save all occurrences only if validation succeeds.

Error handling

All conflict errors now throw:

HttpStatusException(HttpStatus.BAD_REQUEST, "...message...")

so that Micronaut returns meaningful HTTP status codes.

🖥 Frontend Changes

-   Added a checkbox: Repeat weekly
-   Added an input: Number of weeks
-   Updated the booking POST request to include:

{ "startAt": "...", "endAt": "...", "recurring": true, "weeks": 4 }

No changes were made to the availability display.
