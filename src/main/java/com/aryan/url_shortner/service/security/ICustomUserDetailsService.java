package com.aryan.url_shortner.service.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface ICustomUserDetailsService {
    UserDetails loadUserByEmail(String email);
}