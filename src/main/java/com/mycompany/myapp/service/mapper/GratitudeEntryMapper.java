package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.GratitudeEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.service.dto.GratitudeEntryDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link GratitudeEntry} and its DTO {@link GratitudeEntryDTO}.
 */
@Mapper(componentModel = "spring")
public interface GratitudeEntryMapper extends EntityMapper<GratitudeEntryDTO, GratitudeEntry> {
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    GratitudeEntryDTO toDto(GratitudeEntry s);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    /**
     * Partially update a GratitudeEntry entity from DTO, preserving timestamp and user fields.
     */
    @Override
    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "user", ignore = true)
    void partialUpdate(@MappingTarget GratitudeEntry entity, GratitudeEntryDTO dto);
}
