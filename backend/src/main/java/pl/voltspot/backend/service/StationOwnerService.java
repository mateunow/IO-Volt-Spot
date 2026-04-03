package pl.voltspot.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.voltspot.backend.dto.station.StationOwnerResponse;
import pl.voltspot.backend.entity.Station;
import pl.voltspot.backend.entity.StationOwner;
import pl.voltspot.backend.entity.User;
import pl.voltspot.backend.enums.UserRole;
import pl.voltspot.backend.exceptions.BadRequestException;
import pl.voltspot.backend.exceptions.NotFoundException;
import pl.voltspot.backend.mapper.StationMapper;
import pl.voltspot.backend.repository.StationOwnerRepository;
import pl.voltspot.backend.repository.StationRepository;
import pl.voltspot.backend.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StationOwnerService {

    private final StationOwnerRepository stationOwnerRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;

    public void assignOwner(Long stationId, Long ownerUserId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono stacji o id " + stationId));

        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika o id " + ownerUserId));

        if (owner.getRole() != UserRole.OWNER && owner.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Tylko użytkownik z rolą OWNER albo ADMIN może zostać przypisany do stacji");
        }

        boolean exists = stationOwnerRepository.existsByStationIdAndOwner_Id(stationId, ownerUserId);
        if (exists) {
            return;
        }

        StationOwner stationOwner = new StationOwner();
        stationOwner.setStation(station);
        stationOwner.setOwner(owner);

        stationOwnerRepository.save(stationOwner);
    }

    @Transactional(readOnly = true)
    public List<StationOwnerResponse> getOwnersForStation(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("Nie znaleziono stacji o id " + stationId);
        }

        return stationOwnerRepository.findByStationIdOrderByAssignedAtAsc(stationId)
                .stream()
                .map(StationMapper::toOwnerResponse)
                .toList();
    }
}