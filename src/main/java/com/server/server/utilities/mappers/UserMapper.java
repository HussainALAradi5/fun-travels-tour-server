package com.server.server.utilities.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.server.server.dto.user.UserResponse;
import com.server.server.dto.agency.AgencyResponse;
import com.server.server.dto.agency.AgencyBranchResponse;
import com.server.server.models.User;
import com.server.server.models.Agency.Agency;
import com.server.server.models.Agency.AgencyBranch;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) return null;
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setUserName(user.getUserName());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setMobileNumber(user.getMobileNumber());
        dto.setAge(user.getAge());
        dto.setUserType(user.getUserType());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setActive(user.isActive());
        if (user.getAgency() != null) {
            dto.setAgency(new UserResponse.AgencySummary(
                    user.getAgency().getId(), user.getAgency().getAgencyName()));
        }
        if (user.getAgencyBranch() != null) {
            dto.setAgencyBranch(new UserResponse.AgencyBranchSummary(
                    user.getAgencyBranch().getId(), user.getAgencyBranch().getBranchName()));
        }
        return dto;
    }

    public List<UserResponse> toUserResponseList(List<User> users) {
        return users.stream().map(this::toUserResponse).collect(Collectors.toList());
    }

    public AgencyResponse toAgencyResponse(Agency agency) {
        if (agency == null) return null;
        AgencyResponse dto = new AgencyResponse();
        dto.setId(agency.getId());
        dto.setAgencyName(agency.getAgencyName());
        dto.setAddress(agency.getAddress());
        dto.setContactNumber(agency.getContactNumber());
        dto.setOwnerMobileNumber(agency.getOwnerMobileNumber());
        dto.setActive(agency.isActive());
        if (agency.getCountry() != null) {
            dto.setCountry(new AgencyResponse.CountrySummary(
                    agency.getCountry().getId(), agency.getCountry().getFamousName()));
        }
        if (agency.getCity() != null) {
            dto.setCity(new AgencyResponse.CitySummary(
                    agency.getCity().getId(), agency.getCity().getName()));
        }
        if (agency.getAgencyOwner() != null) {
            dto.setAgencyOwner(new AgencyResponse.UserSummary(
                    agency.getAgencyOwner().getId(), agency.getAgencyOwner().getName(),
                    agency.getAgencyOwner().getEmail()));
        }
        return dto;
    }

    public List<AgencyResponse> toAgencyResponseList(List<Agency> agencies) {
        return agencies.stream().map(this::toAgencyResponse).collect(Collectors.toList());
    }

    public AgencyBranchResponse toBranchResponse(AgencyBranch branch) {
        if (branch == null) return null;
        AgencyBranchResponse dto = new AgencyBranchResponse();
        dto.setId(branch.getId());
        dto.setBranchName(branch.getBranchName());
        dto.setBranchAddress(branch.getBranchAddress());
        dto.setContactNumber(branch.getContactNumber());
        dto.setActive(branch.isActive());
        if (branch.getAgency() != null) {
            dto.setAgency(new AgencyBranchResponse.AgencySummary(
                    branch.getAgency().getId(), branch.getAgency().getAgencyName()));
        }
        if (branch.getBranchManager() != null) {
            dto.setBranchManager(new AgencyBranchResponse.UserSummary(
                    branch.getBranchManager().getId(), branch.getBranchManager().getName()));
        }
        return dto;
    }

    public List<AgencyBranchResponse> toBranchResponseList(List<AgencyBranch> branches) {
        return branches.stream().map(this::toBranchResponse).collect(Collectors.toList());
    }
}
