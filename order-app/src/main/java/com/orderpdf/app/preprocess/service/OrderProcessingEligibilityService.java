package com.orderpdf.app.preprocess.service;

import com.orderpdf.app.common.dto.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderProcessingEligibilityService {
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingEligibilityService.class);
    
    private final String reprocessPolicy;

    public OrderProcessingEligibilityService(String reprocessPolicy) {
        this.reprocessPolicy = reprocessPolicy;
    }

    public boolean shouldProcessOrder(OrderItem orderItem) {
        boolean eligible = switch (reprocessPolicy) {
            case "ALWAYS" -> true;
            case "FIRST_TIME_ONLY" -> isFirstTimeProcessing(orderItem);
            case "URL_CHANGED" -> hasUrlChanged(orderItem);
            default -> {
                logger.warn("Unknown reprocess policy: {}, defaulting to ALWAYS", reprocessPolicy);
                yield true;
            }
        };
        
        logger.debug("Order {} eligibility: {} (policy: {})", orderItem.orderId(), eligible, reprocessPolicy);
        return eligible;
    }

    private boolean isFirstTimeProcessing(OrderItem orderItem) {
        return orderItem.oldPdfKey() == null;
    }

    private boolean hasUrlChanged(OrderItem orderItem) {
        // For now, we always process if we reach this point since we don't have previous URL tracking
        // In a real implementation, we would compare against stored previous URL
        return true;
    }
}