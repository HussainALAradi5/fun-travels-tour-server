package com.server.server.enums.payment;

public enum TransactionType {
    PAYMENT,           
    REFUND,            
    PARTIAL_REFUND,    
    CANCELLATION_FEE,  
    WALLET_TOP_UP,     
    MANUAL_ADJUSTMENT, 
    WITHDRAWAL         
}