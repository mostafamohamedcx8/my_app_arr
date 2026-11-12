package com.myapp.aqdeeha.modules.courier.service;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import com.myapp.aqdeeha.modules.courier.dto.CourierDtos.CourierMeResponse;
import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.courier.mapper.CourierMapper;
import com.myapp.aqdeeha.modules.courier.repository.CourierProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Public API of the courier module. */
@Service
@RequiredArgsConstructor
public class CourierService {

    private final CourierProfileRepository profileRepository;
    private final CourierMapper mapper;

    @Transactional
    public CourierProfile createProfile(CourierProfile profile) {
        return profileRepository.saveAndFlush(profile);
    }

    @Transactional(readOnly = true)
    public Optional<CourierProfile> findProfile(Long userId) {
        return profileRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public CourierMeResponse me(Long userId) {
        return profileRepository.findRow(userId).map(mapper::toMe)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURIER_NOT_FOUND));
    }
}
