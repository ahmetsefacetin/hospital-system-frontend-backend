package com.cetin.hospital.service;

import com.cetin.hospital.model.*;
import com.cetin.hospital.repository.PrescriptionRepository;
import com.cetin.hospital.request.DrugRequest;
import com.cetin.hospital.request.InvoiceRequest;
import com.cetin.hospital.request.PrescriptionRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PrescriptionService {
    private final PrescriptionRepository prescriptionRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final DrugService drugService;
    private final InvoiceService invoiceService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository, DoctorService doctorService,
                               PatientService patientService, DrugService drugService, InvoiceService invoiceService) {
        this.prescriptionRepository = prescriptionRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.drugService = drugService;
        this.invoiceService = invoiceService;
    }

    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findAll();
    }

    public Prescription getPrescriptionById(Long prescriptionId) {
        return prescriptionRepository.findById(prescriptionId).orElseThrow(() -> new EntityNotFoundException("Invalid prescriptionId"));
    }

    public List<Prescription> getPrescriptionsByTC(Optional<String> patientTC, Optional<String> doctorTC) {
        List<Prescription> prescriptions;
        if (patientTC.isPresent() && doctorTC.isPresent()) {
            prescriptions = prescriptionRepository.findByPatientTCAndDoctorTC(patientTC.get(), doctorTC.get());
        } else if (patientTC.isPresent()) {
            prescriptions = prescriptionRepository.findByPatientTC(patientTC.get());
        } else {
            prescriptions = prescriptionRepository.findByDoctorTC(doctorTC.get());
        }
        return prescriptions;
    }

    public Prescription createPrescription(PrescriptionRequest prescriptionRequest) {
        Doctor doctor = doctorService.getDoctorById(prescriptionRequest.getDoctorId());
        Patient patient = patientService.getPatientById(prescriptionRequest.getPatientId());

        Prescription prescription = Prescription.builder().
                doctor(doctor).
                patient(patient).
                build();
        prescriptionRepository.save(prescription);

        for (String drugName : prescriptionRequest.getDrugNames()) {
            DrugRequest drugRequest = new DrugRequest();
            drugRequest.setName(drugName);
            drugRequest.setPrice(30);
            drugRequest.setPrescriptionId(prescription.getId());
            drugService.createDrug(drugRequest);
        }

        InvoiceRequest invoiceRequest = new InvoiceRequest();
        invoiceRequest.setPrescriptionId(prescription.getId());
        invoiceRequest.setPatientId(patient.getId());

        invoiceService.createInvoice(invoiceRequest);
        return prescription;
    }
}