package com.banca.gestionale_banca.user.model;

import com.banca.gestionale_banca.shared.model.BaseStatesEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_statuses")
@Getter @Setter
@NoArgsConstructor
public class UserStatus extends BaseStatesEntity {

    public UserStatus(String name){
        this.setName(name);
    }
    
}
