package com.banca.gestionale_banca.rubrica.service;

import com.banca.gestionale_banca.rubrica.dto.CreateContactRequest;
import com.banca.gestionale_banca.rubrica.model.Contact;

import java.util.List;

public interface ContactService {
    List<Contact> getContactsByUser(Long userId);
    Contact createContact(Long userId, CreateContactRequest request);
    void deleteContact(Long contactId, Long userId);
}
