package edu.eci.arsw.RoyalArena.mappers;

import org.mapstruct.Mapper;

import edu.eci.arsw.RoyalArena.dto.response.UserResponseDTO;
import edu.eci.arsw.RoyalArena.model.enums.User;



@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Convierte User a UserResponseDTO. MapStruct mapea automáticamente todos
     * los campos por nombre (id, username, email, role, authProvider, active, createdAt).
     * El password no se mapea porque el DTO no tiene ese campo, y así garantizamos
     * que nunca sale del servidor.
     */
    UserResponseDTO toDto(User user);
}