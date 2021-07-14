package com.graalvmonlambda.product;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Data;

/**
 * An object that represent a time slot for delivery.
 * The slot indicates a period where we could deliver to customer's home.
 * Each slot have a limited number of available delivery. ( field `availDeliveries`)
 * Once this number is 0, customers cannot book a delivery in this time slot anymore.
 */
@Data
@Builder
public class Slot {
  private Integer slotId;
  private LocalDate deliveryDate;
  private LocalDateTime from;
  private LocalDateTime to;
  private Integer availDeliveries;
  private Integer bookedDeliveries;
  private Integer farmId;

  public static LocalDateTime getLocalDateTimeFromIso(String dateTimeString) {
    return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

}
