package com.banca.gestionale_banca.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "registration_statuses")
@Getter @Setter
@NoArgsConstructor
public class RegistrationStatus extends BaseStatesEntity {
  
    public RegistrationStatus(String name) 
    { 
        this.setName(name);
    }
}