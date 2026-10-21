package com.youbeam.core.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExceptionControllerAdviceTests.FailingController.class)
@Import({ExceptionControllerAdvice.class, ExceptionControllerAdviceTests.FailingController.class})
class ExceptionControllerAdviceTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void controllerExceptionKeepsItsStatusAndMessage() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void unexpectedExceptionReturnsGenericErrorWithoutInternals() throws Exception {
        mockMvc.perform(get("/test/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal error, please try again later"))
                .andExpect(content().string(not(containsString("secret-detail"))));
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/forbidden")
        String forbidden() throws ControllerException {
            throw new ControllerException(ErrorResponses.FORBIDDEN);
        }

        @GetMapping("/test/crash")
        String crash() {
            throw new IllegalStateException("secret-detail");
        }
    }
}
