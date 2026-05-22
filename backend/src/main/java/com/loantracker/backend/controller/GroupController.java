package com.loantracker.backend.controller;

import com.loantracker.backend.entity.GroupEntity;
import com.loantracker.backend.repository.GroupEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupEntityRepository groupEntityRepository;

    @GetMapping
    public List<GroupEntity> getAllGroups() {
        return groupEntityRepository.findAll();
    }
}
