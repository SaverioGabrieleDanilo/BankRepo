package com.banca.gestionale_banca.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_statuses")
@Getter
@Setter
@NoArgsConstructor
public class AccountStatus extends BaseStatesEntity{
    public AccountStatus(String name){
        this.setName(name);
    }
}
