package com.banca.gestionale_banca.rubrica.service;

import com.banca.gestionale_banca.rubrica.dto.CreateContactRequest;
import com.banca.gestionale_banca.rubrica.model.Contact;
import com.banca.gestionale_banca.rubrica.repository.ContactRepository;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Contact> getContactsByUser(Long userId) {
        return contactRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Contact createContact(Long userId, CreateContactRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User non trovato"));

        Contact contact = new Contact();
        contact.setUser(user);
        contact.setName(request.getName());
        contact.setSurname(request.getSurname());
        contact.setIban(request.getIban());
        contact.setEmail(request.getEmail());
        contact.setNote(request.getNote());

        return contactRepository.save(contact);
    }

    @Override
    @Transactional
    public void deleteContact(Long contactId, Long userId) {
        contactRepository.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Contatto non trovato"));
        contactRepository.deleteByIdAndUserId(contactId, userId);
    }
}
