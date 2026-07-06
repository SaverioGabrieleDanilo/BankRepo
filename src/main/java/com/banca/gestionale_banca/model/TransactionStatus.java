package com.banca.gestionale_banca.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table (name = "transaction_status")
@Getter
@Setter
@NoArgsConstructor
public class TransactionStatus extends BaseStatesEntity {
    public TransactionStatus(String name){
        this.setName(name);
    }
}
