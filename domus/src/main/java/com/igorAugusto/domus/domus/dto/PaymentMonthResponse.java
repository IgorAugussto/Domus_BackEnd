package com.igorAugusto.domus.domus.dto;

import com.igorAugusto.domus.domus.enums.Frequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMonthResponse {

    private Long outgoingId;
    private String description;
    private BigDecimal value;
    private String category;
    private String paymentType;
    private Frequency frequency;
    private String startDate;
    private String yearMonth;
    private Boolean paid;
    private Long creditCardId;
}
