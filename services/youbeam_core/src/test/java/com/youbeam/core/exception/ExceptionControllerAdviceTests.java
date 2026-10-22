package com.youbeam.core.exception;

import com.youbeam.core.commons.SessionAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El filtro de sesión necesita la base de datos; este test solo prueba el manejo de errores
@WebMvcTest(controllers = ExceptionControllerAdviceTests.FailingController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = SessionAuthenticationFilter.class))
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

    @Test
    void frameworkClientErrorsKeepTheirStatus() throws Exception {
        mockMvc.perform(post("/test/forbidden"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method Not Allowed"));
        mockMvc.perform(post("/test/echo").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @RestController
    static class FailingController {

        @PostMapping(value = "/test/echo", consumes = MediaType.APPLICATION_JSON_VALUE)
        String echo(@RequestBody String body) {
            return body;
        }


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
