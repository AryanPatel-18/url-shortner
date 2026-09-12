package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.exceptions.UserNotFoundException;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{

    private final UserRepository userRepository;

    @Override
    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(()->new UserNotFoundException("The user does not exist"));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("This user does not exist"));
    }
    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }


}
