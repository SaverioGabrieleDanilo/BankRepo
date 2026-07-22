package com.banca.gestionale_banca.transaction.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.banca.gestionale_banca.shared.model.BaseStatesEntity;

@Entity
@Table(name = "deposit_type")
@Getter
@Setter
@NoArgsConstructor
public class DepositType extends BaseStatesEntity {
    public DepositType(String name) {
        setName(name);
    }
}
