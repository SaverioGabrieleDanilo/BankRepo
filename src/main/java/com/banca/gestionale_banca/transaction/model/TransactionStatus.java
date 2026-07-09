package com.banca.gestionale_banca.transaction.model;

import com.banca.gestionale_banca.shared.model.BaseStatesEntity;
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
