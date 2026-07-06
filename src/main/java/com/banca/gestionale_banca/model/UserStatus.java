package com.banca.gestionale_banca.model;


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
