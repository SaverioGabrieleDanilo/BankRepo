package com.banca.gestionale_banca.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDetailsResponse {

    private String id;
    private BigDecimal amount;
    private LocalDateTime date;
    private String cause; 
    private PartyDto sender;    
    private PartyDto recipient; 

    public static class PartyDto {
        private String firstName; 
        private String lastName;  
        private String iban;      

        // Getters e Setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getIban() { return iban; }
        public void setIban(String iban) { this.iban = iban; }
    }

    // Getters e Setters principali
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }
    public PartyDto getSender() { return sender; }
    public void setSender(PartyDto sender) { this.sender = sender; }
    public PartyDto getRecipient() { return recipient; }
    public void setRecipient(PartyDto recipient) { this.recipient = recipient; }
}