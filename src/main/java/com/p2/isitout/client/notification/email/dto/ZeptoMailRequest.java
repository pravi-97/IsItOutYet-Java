package com.p2.isitout.client.notification.email.dto;

import java.util.List;

public record ZeptoMailRequest(From from, List<To> to, String subject, String htmlbody) {

    public record From(String address, String name) {}
    public record To(EmailAddress email_address) {}
    public record EmailAddress(String address, String name) {}
}