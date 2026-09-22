package com.quickbite.order.client;
import com.quickbite.auth.repository.UserRepository;
import com.quickbite.order.dto.external.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class AuthClient {
  private final UserRepository userRepository;
  public UserSummaryDto getUser(Long id){var u=userRepository.findById(id).orElseThrow(()->new IllegalArgumentException("No user with id "+id));return new UserSummaryDto(u.getId(),u.getFullName(),u.getEmail(),u.getPhone(),u.getRole().name());}
}