package com.kaikeMartins.barberapp_backend.ai;


import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantAiService assistantAiService;


    public AssistantController(AssistantAiService assistantAiService) {
        this.assistantAiService = assistantAiService;
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('CLIENTE')")
    public String askAssistant(@RequestBody String message){
        String result = assistantAiService.chat(message);
        return result;
    }

}
