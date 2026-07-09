package com.banca.gestionale_banca.account.model;

import com.banca.gestionale_banca.shared.model.BaseStatesEntity;
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
