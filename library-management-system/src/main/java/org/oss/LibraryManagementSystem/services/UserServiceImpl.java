package org.oss.LibraryManagementSystem.services;

import org.oss.LibraryManagementSystem.dto.UserPayload;
import org.oss.LibraryManagementSystem.models.Role;
import org.oss.LibraryManagementSystem.models.User;
import org.oss.LibraryManagementSystem.repositories.RoleRepository;
import org.oss.LibraryManagementSystem.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;

    private final RoleRepository roleRepository;
    public UserServiceImpl (UserRepository userRepository, RoleRepository roleRepository){
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<User> getAllUsers(Authentication authentication, String keyword, int page, int size, String[] sort) {
        var currentUser = userRepository.findByEmail(authentication.getName());
        var currentUserRoles = new ArrayList<String>();
        for (var role : currentUser.getRoles()){
            currentUserRoles.add(role.getName());
        }
        String sortField = sort[0];
        String sortDirection = sort[1];
        Sort.Direction direction = sortDirection.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort.Order order = new Sort.Order(direction, sortField);
        Pageable paging = PageRequest.of(page - 1, size, Sort.by(order));
        Page<User> pageUsers;
        if (Objects.equals(currentUserRoles.get(0), "ADMIN")){
            if (keyword == null) {
                pageUsers = userRepository.findAll(paging);
            } else {
                pageUsers = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(keyword, keyword, keyword, paging);
            }
            return pageUsers;
        }
        else {
            var memberRole = roleRepository.findByName("MEMBER");
            if (keyword == null) {
                pageUsers = userRepository.findByRolesEquals(memberRole, paging);
            } else {
                pageUsers = userRepository.findByRolesEqualsAndEmailContaining(memberRole, keyword, paging);
            }
            return pageUsers;
        }
    }

    @Override
    public User getUser (Integer id){
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var currentUser = userRepository.findByEmail(authentication.getName());
        var currentUserRoles = new ArrayList<String>();
        for (var role : currentUser.getRoles()){
            currentUserRoles.add(role.getName());
        }
        var user = userRepository.findById(id).orElse(null);
        var userRoles = new ArrayList<String>();
        for (var role : user.getRoles()){
            userRoles.add(role.getName());
        }
        if(Objects.equals(currentUserRoles.get(0), "LIBRARIAN") & (Objects.equals(userRoles.get(0), "ADMIN") || (Objects.equals(userRoles.get(0), "LIBRARIAN")))) {
            return null;
        }
        else
            return user;
    }

    @Override
    public User currentUserDetails (){
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName());
    }

    @Override
    public void deleteUserById (Integer id){
        userRepository.findById(id).ifPresent(userRepository::delete);
    }

    @Override
    public User createUser (UserPayload userPayload) throws ParseException {
        var bCryptPasswordEncoder = new BCryptPasswordEncoder();
        var user = new User();
        user.setFirstName(userPayload.getFirstName());
        user.setLastName(userPayload.getLastName());
        user.setEmail(userPayload.getEmail());
        user.setContactNumber(userPayload.getContactNumber());

        var roleUser = roleRepository.findByName(userPayload.getUserRole());
        Set<Role> roles = new HashSet<>();
        roles.add(roleUser);
        user.setRoles(roles);

        if (userPayload.getPassword() != null){
            var encodedPassword = bCryptPasswordEncoder.encode(userPayload.getPassword());
            user.setPassword(encodedPassword);
        }

        var date = userPayload.getDateOfBirth();
        var timestamp = new Timestamp(new SimpleDateFormat("yyyy-MM-dd").parse(date.toString()).getTime());
        user.setDateOfBirth(timestamp);
        user.setEnabled(true);

        return user;
    }

}