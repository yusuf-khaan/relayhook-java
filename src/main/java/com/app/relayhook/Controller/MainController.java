package com.app.relayhook.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.Service.MainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@CrossOrigin(origins = { "http://localhost:4200" }, allowCredentials = "true")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    public Object saveWorkflow(WorkflowDTO workflowDTO) {
        return mainService.saveWorkflow(workflowDTO);
    }
}
