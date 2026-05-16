package com.otus.user_service.mapper;

import com.otus.user_service.dto.UserRegistrationDTO;
import com.otus.user_service.dto.UserResponseDTO;
import com.otus.user_service.dto.UserUpdateDTO;
import com.otus.user_service.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRegistrationDTO registrationDTO) {
        if (registrationDTO == null) {
            return null;
        }

        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setEmail(registrationDTO.getEmail());
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setPhone(registrationDTO.getPhone());

        return user;
    }

    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(user.getId());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setEmail(user.getEmail());
        responseDTO.setFirstName(user.getFirstName());
        responseDTO.setLastName(user.getLastName());
        responseDTO.setPhone(user.getPhone());
        responseDTO.setRole(user.getRole() != null ? user.getRole().name() : null);
        responseDTO.setActive(user.getActive());
        responseDTO.setCreatedAt(user.getCreatedAt());

        return responseDTO;
    }

    public void updateEntity(User user, UserUpdateDTO updateDTO) {
        if (user == null || updateDTO == null) {
            return;
        }

        if (updateDTO.getFirstName() != null) {
            user.setFirstName(updateDTO.getFirstName());
        }
        if (updateDTO.getLastName() != null) {
            user.setLastName(updateDTO.getLastName());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
    }
}
